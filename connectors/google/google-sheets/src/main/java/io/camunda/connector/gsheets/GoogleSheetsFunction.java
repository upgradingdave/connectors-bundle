/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.gsheets;

import com.google.gson.Gson;
import io.camunda.connector.api.annotation.OutboundConnector;
import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.api.outbound.OutboundConnectorFunction;
import io.camunda.connector.gsheets.model.request.GoogleSheetsRequest;
import io.camunda.connector.gsheets.operation.GoogleSheetOperation;
import io.camunda.connector.gsheets.supplier.GsonSheetsComponentSupplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@OutboundConnector(
    name = "GOOGLESHEETS",
    inputVariables = {"authentication", "operation"},
    type = "io.camunda:google-sheets:1")
public class GoogleSheetsFunction implements OutboundConnectorFunction {

  private static final Logger LOGGER = LoggerFactory.getLogger(GoogleSheetsFunction.class);
  private final Gson gson;
  private final GoogleSheetsOperationFactory operationFactory;

  public GoogleSheetsFunction() {
    gson = GsonSheetsComponentSupplier.gsonInstance();
    operationFactory = GoogleSheetsOperationFactory.getInstance();
  }

  @Override
  public Object execute(OutboundConnectorContext context) {
    String variables = context.getVariables();
    final GoogleSheetsRequest request = gson.fromJson(variables, GoogleSheetsRequest.class);
    context.validate(request);
    context.replaceSecrets(request);
    LOGGER.debug("Request verified successfully and all required secrets replaced");
    GoogleSheetOperation operation = operationFactory.createOperation(request.getOperation());
    return operation.execute(request.getAuthentication());
  }
}
