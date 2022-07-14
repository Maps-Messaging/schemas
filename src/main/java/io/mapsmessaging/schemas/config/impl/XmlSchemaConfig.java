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
  private static final String ROOT = "root";
  private static final String NAMESPACE_AWARE_HEADER = "namespaceAware";
  private static final String VALIDATING_HEADER = "validating";
  private static final String COALESCING_HEADER = "coalescing";

  @Getter
  @Setter
  private String rootEntry;

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
    setMimeType("application/xml");
  }

  private XmlSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
    rootEntry = config.getOrDefault("root", "").toString();
    namespaceAware = (Boolean) config.getOrDefault(NAMESPACE_AWARE_HEADER, false);
    validating = (Boolean) config.getOrDefault(VALIDATING_HEADER, false);
    coalescing = (Boolean) config.getOrDefault(COALESCING_HEADER, false);
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new XmlSchemaConfig(config);
  }

  @Override
  protected JSONObject packData() {
    JSONObject data = new JSONObject();
    packData(data);
    data.put(ROOT, rootEntry);
    data.put(NAMESPACE_AWARE_HEADER, namespaceAware);
    data.put(VALIDATING_HEADER, validating);
    data.put(COALESCING_HEADER, coalescing);
    return data;
  }

}
