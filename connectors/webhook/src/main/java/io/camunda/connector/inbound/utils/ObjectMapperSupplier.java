/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.inbound.utils;

import com.fasterxml.jackson.databind.ObjectMapper;

public final class ObjectMapperSupplier {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private ObjectMapperSupplier() {}

  public static ObjectMapper getMapperInstance() {
    return MAPPER;
  }
}
