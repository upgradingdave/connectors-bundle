/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.aws.model.impl;

import io.camunda.connector.api.annotation.Secret;
import java.util.Objects;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;

public class AwsBaseRequest {

  @Valid @NotNull @Secret private AwsBaseAuthentication authentication;
  @Valid @NotNull @Secret private AwsBaseConfiguration configuration;

  public AwsBaseAuthentication getAuthentication() {
    return authentication;
  }

  public void setAuthentication(final AwsBaseAuthentication authentication) {
    this.authentication = authentication;
  }

  public AwsBaseConfiguration getConfiguration() {
    return configuration;
  }

  public void setConfiguration(final AwsBaseConfiguration configuration) {
    this.configuration = configuration;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    final AwsBaseRequest that = (AwsBaseRequest) o;
    return Objects.equals(authentication, that.authentication)
        && Objects.equals(configuration, that.configuration);
  }

  @Override
  public int hashCode() {
    return Objects.hash(authentication, configuration);
  }

  @Override
  public String toString() {
    return "AwsBaseRequest{"
        + "authentication="
        + authentication
        + ", configuration="
        + configuration
        + "}";
  }
}
