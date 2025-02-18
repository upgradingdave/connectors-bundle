/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.sns.outbound.model;

import io.camunda.connector.api.annotation.Secret;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class SnsConnectorRequest {

  @Valid @NotNull @Secret private SnsAuthenticationRequestData authentication;
  @Valid @NotNull @Secret private TopicRequestData topic;

  public SnsAuthenticationRequestData getAuthentication() {
    return authentication;
  }

  public void setAuthentication(final SnsAuthenticationRequestData authentication) {
    this.authentication = authentication;
  }

  public TopicRequestData getTopic() {
    return topic;
  }

  public void setTopic(final TopicRequestData topic) {
    this.topic = topic;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    SnsConnectorRequest that = (SnsConnectorRequest) o;
    return authentication.equals(that.authentication) && topic.equals(that.topic);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authentication, topic);
  }

  @Override
  public String toString() {
    return "SnsConnectorRequest{" + "authentication=" + authentication + ", topic=" + topic + '}';
  }
}
