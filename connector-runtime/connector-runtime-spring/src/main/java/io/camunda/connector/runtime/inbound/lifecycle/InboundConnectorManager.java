/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information regarding copyright
 * ownership. Camunda licenses this file to you under the Apache License,
 * Version 2.0; you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.camunda.connector.runtime.inbound.lifecycle;

import io.camunda.connector.api.inbound.Health;
import io.camunda.connector.api.inbound.InboundConnectorExecutable;
import io.camunda.connector.api.inbound.webhook.WebhookConnectorExecutable;
import io.camunda.connector.impl.inbound.InboundConnectorProperties;
import io.camunda.connector.runtime.core.inbound.InboundConnectorContextImpl;
import io.camunda.connector.runtime.core.inbound.InboundConnectorFactory;
import io.camunda.connector.runtime.core.inbound.correlation.InboundCorrelationHandler;
import io.camunda.connector.runtime.core.secret.SecretProviderAggregator;
import io.camunda.connector.runtime.inbound.importer.ProcessDefinitionInspector;
import io.camunda.connector.runtime.inbound.webhook.WebhookConnectorRegistry;
import io.camunda.connector.runtime.metrics.ConnectorMetrics.Inbound;
import io.camunda.operate.dto.ProcessDefinition;
import io.camunda.zeebe.spring.client.metrics.MetricsRecorder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class InboundConnectorManager {

  private static final Logger LOG = LoggerFactory.getLogger(InboundConnectorManager.class);

  public static final String WEBHOOK_CONTEXT_BPMN_FIELD = "inbound.context";

  private final InboundConnectorFactory connectorFactory;
  private final InboundCorrelationHandler correlationHandler;
  private final ProcessDefinitionInspector processDefinitionInspector;
  private final SecretProviderAggregator secretProviderAggregator;
  private final WebhookConnectorRegistry webhookConnectorRegistry;
  private final MetricsRecorder metricsRecorder;

  // TODO: consider using external storage instead of these collections to allow multi-instance
  // setup
  private final Set<Long> registeredProcessDefinitionKeys = new HashSet<>();

  private final Map<String, Set<ActiveInboundConnector>> activeConnectorsByBpmnId = new HashMap<>();

  public InboundConnectorManager(
      InboundConnectorFactory connectorFactory,
      InboundCorrelationHandler correlationHandler,
      ProcessDefinitionInspector processDefinitionInspector,
      SecretProviderAggregator secretProviderAggregator,
      MetricsRecorder metricsRecorder,
      @Autowired(required = false) WebhookConnectorRegistry webhookConnectorRegistry) {
    this.connectorFactory = connectorFactory;
    this.correlationHandler = correlationHandler;
    this.processDefinitionInspector = processDefinitionInspector;
    this.secretProviderAggregator = secretProviderAggregator;
    this.metricsRecorder = metricsRecorder;
    this.webhookConnectorRegistry = webhookConnectorRegistry;
  }

  /** Process a batch of process definitions */
  public void registerProcessDefinitions(List<ProcessDefinition> processDefinitions) {
    if (processDefinitions == null || processDefinitions.isEmpty()) {
      return;
    }
    var newProcDefs =
        processDefinitions.stream()
            // register unregistered proc definitions
            .filter(procDef -> !isProcessDefinitionRegistered(procDef.getKey()))
            .peek(procDef -> registeredProcessDefinitionKeys.add(procDef.getKey()))
            // group by BPMN ID
            .collect(Collectors.groupingBy(ProcessDefinition::getBpmnProcessId));

    // we will only handle the latest versions of process defs for each BPMN ID
    var relevantProcDefs =
        newProcDefs.values().stream()
            .map(list -> Collections.max(list, Comparator.comparing(ProcessDefinition::getVersion)))
            .toList();

    for (ProcessDefinition procDef : relevantProcDefs) {
      try {
        handleLatestBpmnVersion(
            procDef.getBpmnProcessId(), processDefinitionInspector.findInboundConnectors(procDef));
      } catch (Exception e) {
        // log and continue with other process definitions anyway
        LOG.error(
            "Failed to activate inbound connectors in process '{}'. It will be ignored",
            procDef.getBpmnProcessId(),
            e);
      }
    }
  }

  /** Check whether process definition with provided key is already registered */
  protected boolean isProcessDefinitionRegistered(long processDefinitionKey) {
    return registeredProcessDefinitionKeys.contains(processDefinitionKey);
  }

  private void handleLatestBpmnVersion(String bpmnId, List<InboundConnectorProperties> connectors) {
    var alreadyActiveConnectors = activeConnectorsByBpmnId.get(bpmnId);
    if (alreadyActiveConnectors != null) {
      var connectorsToDeactivate = alreadyActiveConnectors.stream().toList();
      connectorsToDeactivate.forEach(this::deactivateConnector);
    }
    connectors.forEach(this::activateConnector);
  }

  private void activateConnector(InboundConnectorProperties newProperties) {
    InboundConnectorExecutable executable = connectorFactory.getInstance(newProperties.getType());
    Consumer<Throwable> cancellationCallback = throwable -> deactivateConnector(newProperties);

    var inboundContext =
        new InboundConnectorContextImpl(
            secretProviderAggregator, newProperties, correlationHandler, cancellationCallback);

    var connector = new ActiveInboundConnector(executable, newProperties, inboundContext);

    try {
      addActiveConnector(connector);
      if (webhookConnectorRegistry == null && executable instanceof WebhookConnectorExecutable) {
        throw new Exception(
            "Cannot activate webhook connector. "
                + "Check whether property camunda.connector.webhook.enabled is set to true.");
      }
      executable.activate(inboundContext);
      if (webhookConnectorRegistry != null
          && connector.executable() instanceof WebhookConnectorExecutable) {
        webhookConnectorRegistry.register(connector);
        LOG.trace("Registering webhook: " + newProperties.getType());
      }
      inboundContext.reportHealth(Health.up());
      metricsRecorder.increase(
          Inbound.METRIC_NAME_ACTIVATIONS, Inbound.ACTION_ACTIVATED, newProperties.getType());
    } catch (Exception e) {
      inboundContext.reportHealth(Health.down(e));
      // log and continue with other connectors anyway
      LOG.error("Failed to activate inbound connector " + newProperties, e);
      metricsRecorder.increase(
          Inbound.METRIC_NAME_ACTIVATIONS,
          Inbound.ACTION_ACTIVATION_FAILED,
          newProperties.getType());
    }
  }

  private void addActiveConnector(ActiveInboundConnector connector) {
    activeConnectorsByBpmnId.compute(
        connector.properties().getBpmnProcessId(),
        (bpmnId, connectors) -> {
          if (connectors == null) {
            Set<ActiveInboundConnector> set = new HashSet<>();
            set.add(connector);
            return set;
          }
          connectors.add(connector);
          return connectors;
        });
  }

  private void deactivateConnector(InboundConnectorProperties properties) {
    findActiveConnector(properties).ifPresent(this::deactivateConnector);
    metricsRecorder.increase(
        Inbound.METRIC_NAME_ACTIVATIONS, Inbound.ACTION_DEACTIVATED, properties.getType());
  }

  private void deactivateConnector(ActiveInboundConnector connector) {
    try {
      connector.executable().deactivate();
      activeConnectorsByBpmnId.get(connector.properties().getBpmnProcessId()).remove(connector);
      if (webhookConnectorRegistry != null
          && connector.executable() instanceof WebhookConnectorExecutable) {
        webhookConnectorRegistry.deregister(connector);
        LOG.trace("Unregistering webhook: " + connector.properties().getType());
      }
      metricsRecorder.increase(
          Inbound.METRIC_NAME_ACTIVATIONS,
          Inbound.ACTION_DEACTIVATED,
          connector.properties().getType());
    } catch (Exception e) {
      // log and continue with other connectors anyway
      LOG.error("Failed to deactivate inbound connector " + connector, e);
    }
  }

  private Optional<ActiveInboundConnector> findActiveConnector(
      InboundConnectorProperties properties) {
    return Optional.ofNullable(activeConnectorsByBpmnId.get(properties.getBpmnProcessId()))
        .flatMap(
            connectors ->
                connectors.stream().filter(c -> c.properties().equals(properties)).findFirst());
  }

  public List<ActiveInboundConnector> query(ActiveInboundConnectorQuery request) {
    var filteredByBpmnProcessId = filterByBpmnProcessId(request.bpmnProcessId());
    var filteredByType = filterByConnectorType(filteredByBpmnProcessId, request.type());
    return filterByElementId(filteredByType, request.elementId());
  }

  private List<ActiveInboundConnector> filterByBpmnProcessId(String bpmnProcessId) {
    if (bpmnProcessId != null) {
      return new ArrayList<>(
          activeConnectorsByBpmnId.getOrDefault(bpmnProcessId, Collections.emptySet()));
    } else {
      return activeConnectorsByBpmnId.values().stream()
          .flatMap(Collection::stream)
          .collect(Collectors.toList());
    }
  }

  private List<ActiveInboundConnector> filterByConnectorType(
      List<ActiveInboundConnector> connectors, String type) {
    if (type == null) {
      return connectors;
    }
    return connectors.stream()
        .filter(props -> type.equals(props.properties().getType()))
        .collect(Collectors.toList());
  }

  private List<ActiveInboundConnector> filterByElementId(
      List<ActiveInboundConnector> connectors, String elementId) {

    if (elementId == null) {
      return connectors;
    }
    return connectors.stream()
        .filter(connector -> elementId.equals(connector.properties().getElementId()))
        .collect(Collectors.toList());
  }
}
