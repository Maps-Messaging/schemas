/*
 *
 *  Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *  Copyright [ 2024 - 2025 ] [Maps Messaging B.V.]
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 *
 */
package io.mapsmessaging.schemas.config.impl;

import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * The type Xml schema config.
 */
@Schema(description = "XML Schema Configuration")
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

  /**
   * Instantiates a new Xml schema config.
   */
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
  protected JsonObject packData() {
    JsonObject data = new JsonObject();
    packData(data);
    data.addProperty(ROOT, rootEntry);
    data.addProperty(NAMESPACE_AWARE_HEADER, namespaceAware);
    data.addProperty(VALIDATING_HEADER, validating);
    data.addProperty(COALESCING_HEADER, coalescing);
    return data;
  }

}
