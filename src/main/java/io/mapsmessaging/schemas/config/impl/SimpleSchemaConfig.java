/*
 *
 *     Copyright [ 2020 - 2023 ] [Matthew Buckton]
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
import java.io.IOException;
import java.util.Map;
import org.json.JSONObject;

/**
 * The type Simple schema config.
 */
abstract class SimpleSchemaConfig extends SchemaConfig {

  /**
   * Instantiates a new Simple schema config.
   *
   * @param format the format
   */
  protected SimpleSchemaConfig(String format) {
    super(format);
  }

  /**
   * Instantiates a new Simple schema config.
   *
   * @param format the format
   * @param config the config
   */
  protected SimpleSchemaConfig(String format, Map<String, Object> config) {
    super(format, config);
  }

  @Override
  protected JSONObject packData() throws IOException {
    JSONObject data = new JSONObject();
    packData(data);
    return data;
  }
}