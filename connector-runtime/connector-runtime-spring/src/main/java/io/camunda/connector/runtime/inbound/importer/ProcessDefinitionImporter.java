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
package io.camunda.connector.runtime.inbound.importer;

import io.camunda.connector.runtime.inbound.lifecycle.InboundConnectorManager;
import io.camunda.connector.runtime.metrics.ConnectorMetrics.Inbound;
import io.camunda.operate.CamundaOperateClient;
import io.camunda.operate.dto.ProcessDefinition;
import io.camunda.operate.dto.SearchResult;
import io.camunda.operate.exception.OperateException;
import io.camunda.operate.search.SearchQuery;
import io.camunda.zeebe.spring.client.metrics.MetricsRecorder;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.util.CollectionUtils;

public class ProcessDefinitionImporter {

  private static final Logger LOG = LoggerFactory.getLogger(ProcessDefinitionImporter.class);

  private final CamundaOperateClient camundaOperateClient;
  private final InboundConnectorManager inboundManager;
  private final MetricsRecorder metricsRecorder;

  private List<Object> paginationIndex;

  @Autowired
  public ProcessDefinitionImporter(
      CamundaOperateClient camundaOperateClient,
      InboundConnectorManager inboundManager,
      @Autowired(required = false) MetricsRecorder metricsRecorder) {
    this.camundaOperateClient = camundaOperateClient;
    this.inboundManager = inboundManager;
    this.metricsRecorder = metricsRecorder;
  }

  @Scheduled(fixedDelayString = "${camunda.connector.polling.interval:5000}")
  public synchronized void scheduleImport() throws OperateException {
    LOG.trace("Query process deployments...");

    SearchResult<ProcessDefinition> result;
    do {
      LOG.trace("Running paginated query");
      // automatically sorted by process definition key, i.e. in chronological order of deployment
      SearchQuery processDefinitionQuery =
          new SearchQuery.Builder().searchAfter(paginationIndex).size(20).build();

      result = camundaOperateClient.search(processDefinitionQuery, ProcessDefinition.class);
      List<Object> newPaginationIdx = result.getSortValues();

      if (!CollectionUtils.isEmpty(newPaginationIdx)) {
        paginationIndex = newPaginationIdx;
      }
      handleImportedDefinitions(result.getItems());

    } while (result.getItems().size() > 0);
  }

  private void handleImportedDefinitions(List<ProcessDefinition> processDefinitions) {

    if (processDefinitions == null || processDefinitions.isEmpty()) {
      LOG.trace("... returned no process definitions.");
      return;
    }
    LOG.trace("... returned " + processDefinitions.size() + " process definitions.");
    meter(processDefinitions.size());

    inboundManager.registerProcessDefinitions(processDefinitions);
  }

  private void meter(int count) {
    if (metricsRecorder != null) {
      metricsRecorder.increase(
          Inbound.METRIC_NAME_INBOUND_PROCESS_DEFINITIONS_CHECKED, null, null, count);
    }
  }
}
