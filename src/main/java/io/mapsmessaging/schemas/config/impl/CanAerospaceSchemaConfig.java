/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.IOException;

@Schema(description = "Canbus Schema Configuration")
public class CanAerospaceSchemaConfig extends SchemaConfig {

  private static final String YML_PATH = "YamlPath";

  public CanAerospaceSchemaConfig() {
    super("canaerospace");
  }

  protected CanAerospaceSchemaConfig(SchemaConfig config) {
    super(config);
  }

  @Override
  public String getMimeType() {
    return "application/octet-stream";
  }

  @Override
  public SchemaConfig getInstance(SchemaConfig config) {
    return new CanAerospaceSchemaConfig(config);
  }

  public String getYamlPath() {
    JsonObject json = getSchema();
    if (json != null && json.has(YML_PATH)) {
      return json.get(YML_PATH).getAsString();
    }
    return null;
  }

  public void setYamlPath(String xmlPath) {
    JsonObject json = ensureSchema();
    if (xmlPath == null || xmlPath.isBlank()) {
      json.remove(YML_PATH);
      return;
    }
    json.addProperty(YML_PATH, xmlPath);
  }

  @Override
  public String pack() throws IOException {
    String path = getYamlPath();
    if ((path == null || path.isBlank())) {
      throw new IOException("Canaerospace config requires a 'dialect'");
    }
    return super.pack();
  }

  private JsonObject ensureSchema() {
    JsonObject json = getSchema();
    if (json == null) {
      json = new JsonObject();
      setSchema(json);
    }
    return json;
  }
}