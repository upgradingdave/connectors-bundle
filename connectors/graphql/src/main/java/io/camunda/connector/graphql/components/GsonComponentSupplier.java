/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.graphql.components;

import com.google.api.client.json.gson.GsonFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.ToNumberPolicy;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;
import io.camunda.connector.common.auth.Authentication;
import io.camunda.connector.common.auth.BasicAuthentication;
import io.camunda.connector.common.auth.BearerAuthentication;
import io.camunda.connector.common.auth.NoAuthentication;
import io.camunda.connector.common.auth.OAuthAuthentication;

public class GsonComponentSupplier {

  private static final GsonFactory GSON_FACTORY = new GsonFactory();
  private static final Gson GSON =
      new GsonBuilder()
          .serializeNulls()
          .setObjectToNumberStrategy(ToNumberPolicy.LONG_OR_DOUBLE)
          .registerTypeAdapterFactory(
              RuntimeTypeAdapterFactory.of(Authentication.class, "type")
                  .registerSubtype(NoAuthentication.class, "noAuth")
                  .registerSubtype(BasicAuthentication.class, "basic")
                  .registerSubtype(BearerAuthentication.class, "bearer")
                  .registerSubtype(OAuthAuthentication.class, "oauth-client-credentials-flow"))
          .create();

  private GsonComponentSupplier() {}

  public static Gson gsonInstance() {
    return GSON;
  }

  public static GsonFactory gsonFactoryInstance() {
    return GSON_FACTORY;
  }
}
