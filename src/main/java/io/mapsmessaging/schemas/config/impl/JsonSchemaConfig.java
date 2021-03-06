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

/**
 * The type Json schema config.
 */
public class JsonSchemaConfig extends SimpleSchemaConfig {

  private static final String NAME = "JSON";

  /**
   * Instantiates a new Json schema config.
   */
  public JsonSchemaConfig() {
    super(NAME);
    setMimeType("application/json");
  }

  private JsonSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new JsonSchemaConfig(config);
  }
}
