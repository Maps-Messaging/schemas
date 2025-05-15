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

package io.mapsmessaging.schemas.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import static io.mapsmessaging.schemas.config.Constants.FORMAT;
import static io.mapsmessaging.schemas.config.Constants.SCHEMA;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.SCHEMA_CONFIG_FACTORY_INVALID_CONFIG;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.SCHEMA_CONFIG_FACTORY_SCHEMA_NOT_FOUND;

/**
 * The type Schema config factory.
 */

@SuppressWarnings("java:S6548") // yes it is a singleton
public class SchemaConfigFactory {
  public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  private static final String ERROR_MESSAGE = "Not a valid schema config";
  private static final String CONFIG_ERROR = "Unknown schema config found";

  private static class Holder {
    static final SchemaConfigFactory INSTANCE = new SchemaConfigFactory();
  }

  public static SchemaConfigFactory getInstance() {
    return SchemaConfigFactory.Holder.INSTANCE;
  }

  private final List<SchemaConfig> schemaConfigs;
  private final Logger logger;

  private SchemaConfigFactory() {
    schemaConfigs = new ArrayList<>();
    ServiceLoader<SchemaConfig> schemaConfigServiceLoader = ServiceLoader.load(SchemaConfig.class);
    for (SchemaConfig config : schemaConfigServiceLoader) {
      schemaConfigs.add(config);
    }
    logger = LoggerFactory.getLogger(SchemaConfigFactory.class);
  }

  /**
   * Construct config schema config.
   *
   * @param properties the properties
   * @return the schema config
   * @throws IOException the io exception
   */
  public SchemaConfig constructConfig(Map<String, Object> properties) throws IOException {
    if (properties.containsKey(SCHEMA)) {
      Object val = properties.get(SCHEMA);
      if (val instanceof Map) {
        Map<String, Object> formatMap = (Map) properties.get(SCHEMA);
        Object formatName = formatMap.get(FORMAT);
        if (formatName != null) {
          for (SchemaConfig config : schemaConfigs) {
            if (config.getFormat().equalsIgnoreCase(formatName.toString())) {
              return config.getInstance(formatMap);
            }
          }
        }
      }
    }
    logger.log(SCHEMA_CONFIG_FACTORY_INVALID_CONFIG);
    throw new IOException(CONFIG_ERROR);
  }

  /**
   * Construct config schema config.
   *
   * @param rawPayload the raw payload
   * @return the schema config
   * @throws IOException the io exception
   */
  public SchemaConfig constructConfig(byte[] rawPayload) throws IOException {
    return constructConfig(new String(rawPayload));
  }

  /**
   * Construct config schema config.
   *
   * @param payload the payload
   * @return the schema config
   * @throws IOException the io exception
   */
  public SchemaConfig constructConfig(String payload) throws IOException {
    JsonObject json = JsonParser.parseString(payload).getAsJsonObject();
    return constructConfig(json);
  }

  public SchemaConfig constructConfig(JsonObject schemaJson) throws IOException {
    String formatName = null;
    try {
      if (!schemaJson.has(SCHEMA) || !schemaJson.get(SCHEMA).isJsonObject()) {
        logger.log(SCHEMA_CONFIG_FACTORY_INVALID_CONFIG);
        throw new IOException(ERROR_MESSAGE);
      }

      JsonObject inner = schemaJson.getAsJsonObject(SCHEMA);
      if (!inner.has(FORMAT) || !inner.get(FORMAT).isJsonPrimitive()) {
        logger.log(SCHEMA_CONFIG_FACTORY_INVALID_CONFIG);
        throw new IOException(CONFIG_ERROR);
      }

      formatName = inner.get(FORMAT).getAsString();
      for (SchemaConfig config : schemaConfigs) {
        if (config.getFormat().equalsIgnoreCase(formatName)) {
          Type type = new TypeToken<Map<String, Object>>() {
          }.getType();
          Map<String, Object> configMap = gson.fromJson(inner, type);
          return config.getInstance(configMap);
        }
      }
    } catch (Exception e) {
      logger.log(SCHEMA_CONFIG_FACTORY_SCHEMA_NOT_FOUND, formatName);
      throw new IOException(CONFIG_ERROR, e);
    }

    logger.log(SCHEMA_CONFIG_FACTORY_SCHEMA_NOT_FOUND, formatName);
    throw new IOException(CONFIG_ERROR);
  }

}
