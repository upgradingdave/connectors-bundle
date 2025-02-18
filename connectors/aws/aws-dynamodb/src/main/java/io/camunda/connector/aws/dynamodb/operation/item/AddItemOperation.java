/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH
 * under one or more contributor license agreements. Licensed under a proprietary license.
 * See the License.txt file for more information. You may not use this file
 * except in compliance with the proprietary license.
 */
package io.camunda.connector.aws.dynamodb.operation.item;

import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Item;
import com.amazonaws.services.dynamodbv2.document.PutItemOutcome;
import com.amazonaws.services.dynamodbv2.document.Table;
import io.camunda.connector.aws.GsonComponentSupplier;
import io.camunda.connector.aws.dynamodb.model.item.AddItem;
import io.camunda.connector.aws.dynamodb.operation.AwsDynamoDbOperation;

public class AddItemOperation implements AwsDynamoDbOperation {

  private final AddItem addItemModel;

  public AddItemOperation(final AddItem addItemModel) {
    this.addItemModel = addItemModel;
  }

  public PutItemOutcome invoke(final DynamoDB dynamoDB) {
    String itemStr = GsonComponentSupplier.gsonInstance().toJson(addItemModel.getItem());
    Item item = Item.fromJSON(itemStr);

    final Table table = dynamoDB.getTable(addItemModel.getTableName());
    return table.putItem(item);
  }
}
