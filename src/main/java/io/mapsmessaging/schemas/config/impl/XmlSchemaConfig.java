/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package io.mapsmessaging.schemas.config.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;


/**
 * The type Xml schema config.
 */
@Schema(description = "XML Schema Configuration")
public class XmlSchemaConfig extends SchemaConfig {

  private static final String NAME = "XML";

  /**
   * Instantiates a new Xml schema config.
   */
  public XmlSchemaConfig() {
    super(NAME);
  }

  private XmlSchemaConfig(XRegistrySchemaVersion config) {
    super(config);
  }

  public SchemaConfig getInstance(XRegistrySchemaVersion config) {
    return new XmlSchemaConfig(config);
  }

  @Override
  public String getMimeType() {
    return "application/xml";
  }

  public XmlConfig getConfig() {
    return gson.fromJson(getSchema(), XmlConfig.class);
  }

  public void setConfig(XmlConfig xmlConfig) {
    setSchema(gson.toJsonTree(xmlConfig).getAsJsonObject());
  }

  @Getter
  @Setter
  public static final class XmlConfig {
    private String rootEntry;
    private boolean namespaceAware = false;
    private boolean validating = false;
    private boolean coalescing = false;
  }
}