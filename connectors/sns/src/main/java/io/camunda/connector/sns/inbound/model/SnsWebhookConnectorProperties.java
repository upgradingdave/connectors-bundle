/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.sns.inbound.model;

import io.camunda.connector.api.annotation.Secret;
import io.camunda.connector.impl.inbound.InboundConnectorProperties;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

public class SnsWebhookConnectorProperties {

  private final InboundConnectorProperties genericProperties;
  @Secret private String context;
  private SubscriptionAllowListFlag subscriptionAllowListFlag;
  @Secret private List<String> subscriptionAllowList;

  public SnsWebhookConnectorProperties(InboundConnectorProperties properties) {
    this.genericProperties = properties;
    this.context = genericProperties.getProperties().get("inbound.context");
    // If no value somehow passed, force it to specific.
    // In this case, BPMN process might fail but at the same time, we enforce security.
    this.subscriptionAllowListFlag =
        SubscriptionAllowListFlag.valueOf(
            genericProperties
                .getProperties()
                .getOrDefault(
                    "inbound.securitySubscriptionAllowedFor",
                    SubscriptionAllowListFlag.specific.name()));
    this.subscriptionAllowList =
        Arrays.stream(
                genericProperties
                    .getProperties()
                    .getOrDefault("inbound.topicsAllowList", "")
                    .trim()
                    .split(","))
            .collect(Collectors.toList());
  }

  public InboundConnectorProperties getGenericProperties() {
    return genericProperties;
  }

  public String getContext() {
    return context;
  }

  public SubscriptionAllowListFlag getSubscriptionAllowListFlag() {
    return Optional.ofNullable(subscriptionAllowListFlag)
        .orElse(SubscriptionAllowListFlag.specific);
  }

  public List<String> getSubscriptionAllowList() {
    return Optional.ofNullable(subscriptionAllowList).orElse(Collections.emptyList());
  }

  public void setContext(String context) {
    this.context = context;
  }

  public void setSubscriptionAllowListFlag(SubscriptionAllowListFlag subscriptionAllowListFlag) {
    this.subscriptionAllowListFlag = subscriptionAllowListFlag;
  }

  public void setSubscriptionAllowList(List<String> subscriptionAllowList) {
    this.subscriptionAllowList = subscriptionAllowList;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    SnsWebhookConnectorProperties that = (SnsWebhookConnectorProperties) o;
    return Objects.equals(genericProperties, that.genericProperties)
        && Objects.equals(context, that.context)
        && subscriptionAllowListFlag == that.subscriptionAllowListFlag
        && Objects.equals(subscriptionAllowList, that.subscriptionAllowList);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        genericProperties, context, subscriptionAllowListFlag, subscriptionAllowList);
  }

  @Override
  public String toString() {
    return "SnsWebhookConnectorProperties{"
        + "genericProperties="
        + genericProperties
        + ", context='"
        + context
        + '\''
        + ", subscriptionAllowListFlag="
        + subscriptionAllowListFlag
        + ", subscriptionAllowList="
        + subscriptionAllowList
        + '}';
  }
}
