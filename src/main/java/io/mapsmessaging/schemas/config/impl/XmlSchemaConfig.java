/*
 *
 *     Copyright [ 2020 - 2022 ] [Matthew Buckton]
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package io.mapsmessaging.schemas.config.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public class XmlSchemaConfig extends SimpleSchemaConfig {

  private static final String NAME = "XML";
  private static final String NAMESPACE_AWARE = "namespaceAware";
  private static final String VALIDATING = "validating";
  private static final String COALESCING = "coalescing";

  @Getter
  @Setter
  private boolean namespaceAware = false;

  @Getter
  @Setter
  private boolean validating = false;

  @Getter
  @Setter
  private boolean coalescing = false;

  public XmlSchemaConfig() {
    super(NAME);
  }

  private XmlSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
    namespaceAware = (Boolean) config.getOrDefault(NAMESPACE_AWARE, false);
    validating = (Boolean) config.getOrDefault(VALIDATING, false);
    coalescing = (Boolean) config.getOrDefault(COALESCING, false);
  }


  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new XmlSchemaConfig(config);
  }

  @Override
  protected JSONObject packData() {
    JSONObject data = new JSONObject();
    packData(data);
    data.put(NAMESPACE_AWARE, namespaceAware);
    data.put(VALIDATING, validating);
    data.put(COALESCING, coalescing);
    return data;
  }

}
