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
package io.camunda.connector.runtime.outbound.lifecycle;

import io.camunda.connector.impl.outbound.OutboundConnectorConfiguration;
import java.util.Comparator;

public class OutboundConnectorConfigurationComparator
    implements Comparator<OutboundConnectorConfiguration> {

  @Override
  public int compare(OutboundConnectorConfiguration o1, OutboundConnectorConfiguration o2) {
    if (o1 == o2) return 0;
    if (o1 == null) return -1;

    return o1.getType().compareTo(o2.getType());
  }
}
