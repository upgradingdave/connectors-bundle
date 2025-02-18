/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.gdrive.request;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.camunda.connector.api.outbound.OutboundConnectorContext;
import io.camunda.connector.gdrive.BaseTest;
import io.camunda.connector.gdrive.model.request.GoogleDriveRequest;
import io.camunda.connector.impl.ConnectorInputException;
import io.camunda.connector.test.outbound.OutboundConnectorContextBuilder;
import io.camunda.google.model.AuthenticationType;
import java.io.IOException;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class GoogleDriveRequestTest extends BaseTest {
  private static final String SUCCESS_CASES_RESOURCE_PATH =
      "src/test/resources/requests/request-success-test-cases.json";
  private static final String FAIL_CASES_RESOURCE_PATH =
      "src/test/resources/requests/request-fail-test-cases.json";

  private static Stream<String> successRequestCases() throws IOException {
    return BaseTest.loadTestCasesFromResourceFile(SUCCESS_CASES_RESOURCE_PATH);
  }

  private static Stream<String> failRequestCases() throws IOException {
    return BaseTest.loadTestCasesFromResourceFile(FAIL_CASES_RESOURCE_PATH);
  }

  @DisplayName("Should replace all secrets data if variable start with 'secret.' ")
  @ParameterizedTest(name = "Executing test case # {index}")
  @MethodSource("successRequestCases")
  void replaceSecrets_shouldReplaceAllSecrets(final String input) {
    // Given
    GoogleDriveRequest request = parseInput(input, GoogleDriveRequest.class);
    OutboundConnectorContext context =
        OutboundConnectorContextBuilder.create()
            .secret(SECRET_BEARER_TOKEN, ACTUAL_BEARER_TOKEN)
            .secret(SECRET_REFRESH_TOKEN, ACTUAL_REFRESH_TOKEN)
            .secret(SECRET_OAUTH_CLIENT_ID, ACTUAL_OAUTH_CLIENT_ID)
            .secret(SECRET_OAUTH_SECRET_ID, ACTUAL_OAUTH_SECRET_ID)
            .build();

    // When
    context.replaceSecrets(request);

    // Then
    if (request.getAuthentication().getAuthType() == AuthenticationType.BEARER) {
      assertThat(request.getAuthentication().getBearerToken())
          .isNotNull()
          .isEqualTo(ACTUAL_BEARER_TOKEN);
    }

    if (request.getAuthentication().getAuthType() == AuthenticationType.REFRESH) {
      assertThat(request.getAuthentication().getOauthClientId())
          .isNotNull()
          .isEqualTo(ACTUAL_OAUTH_CLIENT_ID);
      assertThat(request.getAuthentication().getOauthClientSecret())
          .isNotNull()
          .isEqualTo(ACTUAL_OAUTH_SECRET_ID);
      assertThat(request.getAuthentication().getOauthRefreshToken())
          .isNotNull()
          .isEqualTo(ACTUAL_REFRESH_TOKEN);
    }
  }

  @DisplayName("Throw IllegalArgumentException when request without require fields")
  @ParameterizedTest(name = "Executing test case # {index}")
  @MethodSource("failRequestCases")
  void validateWith_shouldThrowExceptionWhenNonExistLeastOneRequireField(final String input) {
    // Given
    GoogleDriveRequest request = parseInput(input, GoogleDriveRequest.class);
    OutboundConnectorContext context = OutboundConnectorContextBuilder.create().build();
    // When and Then
    ConnectorInputException thrown =
        assertThrows(ConnectorInputException.class, () -> context.validate(request));
    assertThat(thrown.getMessage()).contains("Found constraints violated while validating input:");
  }
}
