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

package io.mapsmessaging.schemas.config;

import static io.mapsmessaging.schemas.config.Constants.DEFAULT_FORMAT;
import static io.mapsmessaging.schemas.config.Constants.FORMAT;
import static io.mapsmessaging.schemas.config.Constants.SCHEMA;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.SCHEMA_CONFIG_FACTORY_INVALID_CONFIG;
import static io.mapsmessaging.schemas.logging.SchemaLogMessages.SCHEMA_CONFIG_FACTORY_SCHEMA_NOT_FOUND;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import java.io.IOException;
import java.util.Map;
import java.util.ServiceLoader;
import org.json.JSONObject;

public class SchemaConfigFactory {


  private static final SchemaConfigFactory instance = new SchemaConfigFactory();

  public static SchemaConfigFactory getInstance() {
    return instance;
  }

  private final ServiceLoader<SchemaConfig> schemaConfigServiceLoader;
  private final Logger logger;

  public SchemaConfig constructConfig(Map<String, Object> properties) throws IOException {
    if(properties.containsKey(SCHEMA)){
      Map<String, Object> formatMap = (Map) properties.get(SCHEMA);
      Object formatName = formatMap.get(FORMAT);
      if(formatName == null){
        formatName = DEFAULT_FORMAT;
      }
      for (SchemaConfig config : schemaConfigServiceLoader) {
        if (config.getFormat().equalsIgnoreCase(formatName.toString())) {
          return config.getInstance(formatMap);
        }
      }
    }
    logger.log(SCHEMA_CONFIG_FACTORY_INVALID_CONFIG);
    throw new IOException("Unknown schema config found");
  }

  public SchemaConfig constructConfig(byte[] rawPayload) throws IOException {
    return constructConfig(new String(rawPayload));
  }

  public SchemaConfig constructConfig(String payload) throws IOException {
    JSONObject schemaJson = new JSONObject(payload);
    if (!schemaJson.has(SCHEMA)) {
      logger.log(SCHEMA_CONFIG_FACTORY_INVALID_CONFIG);
      throw new IOException("Not a valid schema config");
    }

    schemaJson = schemaJson.getJSONObject(SCHEMA);
    if (!schemaJson.has(FORMAT)) {
      logger.log(SCHEMA_CONFIG_FACTORY_INVALID_CONFIG);
      throw new IOException("Not a valid schema config");
    }

    String formatName = schemaJson.getString(FORMAT);
    for (SchemaConfig config : schemaConfigServiceLoader) {
      if (config.getFormat().equalsIgnoreCase(formatName)) {
        return config.getInstance(schemaJson.toMap());
      }
    }
    logger.log(SCHEMA_CONFIG_FACTORY_SCHEMA_NOT_FOUND, formatName);
    throw new IOException("Unknown schema config found");
  }

  private SchemaConfigFactory() {
    schemaConfigServiceLoader = ServiceLoader.load(SchemaConfig.class);
    logger = LoggerFactory.getLogger(SchemaConfigFactory.class);
  }

}
