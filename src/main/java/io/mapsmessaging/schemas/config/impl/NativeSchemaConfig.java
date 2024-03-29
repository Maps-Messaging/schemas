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

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.NATIVE_TYPE_UNKNOWN;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

/**
 * The type Native schema config.
 */
public class NativeSchemaConfig extends SimpleSchemaConfig {

  private static final String NAME = "Native";
  @Getter
  @Setter
  private TYPE type;

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
  protected NativeSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
    String typeName = (String)config.get("type");
    switch (typeName.toUpperCase()) {
      case "STRING":
        type = TYPE.STRING;
        break;
      case "NUMERIC_STRING":
        type = TYPE.NUMERIC_STRING;
        break;
      case "INT8":
        type = TYPE.INT8;
        break;
      case "INT16":
        type = TYPE.INT16;
        break;
      case "INT32":
        type = TYPE.INT32;
        break;
      case "INT64":
        type = TYPE.INT64;
        break;
      case "FLOAT":
        type = TYPE.FLOAT;
        break;
      case "DOUBLE":
        type = TYPE.DOUBLE;
        break;

      default:
        logger.log(NATIVE_TYPE_UNKNOWN, getFormat(), uniqueId);
        type = TYPE.STRING;
    }
  }

  @Override
  protected JSONObject packData() throws IOException {
    if (type == null) {
      logger.log(NATIVE_TYPE_UNKNOWN, getFormat(), uniqueId);
      throw new IOException("No type defined specified");
    }
    JSONObject data = new JSONObject();
    packData(data);
    data.put("type", type.toString());
    return data;
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
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

