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

import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * The type Native schema config.
 */
@Schema(description = "Native Schema Configuration")
public class NativeSchemaConfig extends SchemaConfig {

  private static final String NAME = "native";

  /**
   * Instantiates a new Native schema config.
   */
  public NativeSchemaConfig() {
    super(NAME);
  }

  /**
   * Instantiates a new Native schema config.
   *
   * @param config the config
   */
  public NativeSchemaConfig(SchemaConfig config) {
    super(config);
  }

  @Override
  public String getMimeType() {
    return "application/text";
  }

  public TYPE getType() {
    JsonObject json = getSchema();
    if (json.has("type")) {
      return TYPE.valueOf(json.get("type").getAsString());
    }
    return null;
  }

  public void setType(TYPE type) {
    JsonObject json = new JsonObject();
    json.addProperty("type", type.name());
    setSchema(json);
  }

  public SchemaConfig getInstance(SchemaConfig config) {
    return new NativeSchemaConfig(config);
  }

  /**
   * The enum Type.
   */
  public enum TYPE {
    /**
     * String type.
     */
    STRING,
    /**
     * Numeric string type.
     */
    NUMERIC_STRING,
    /**
     * Int 8 type.
     */
    INT8,
    /**
     * Int 16 type.
     */
    INT16,
    /**
     * Int 32 type.
     */
    INT32,
    /**
     * Int 64 type.
     */
    INT64,
    /**
     * Float type.
     */
    FLOAT,
    /**
     * Double type.
     */
    DOUBLE
  }
}

