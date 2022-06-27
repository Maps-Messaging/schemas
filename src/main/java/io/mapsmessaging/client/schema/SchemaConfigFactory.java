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

package io.mapsmessaging.client.schema;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.ServiceLoader;
import java.util.function.Consumer;
import org.json.JSONObject;

public class SchemaConfigFactory {

  private static final SchemaConfigFactory instance = new SchemaConfigFactory();

  public static SchemaConfigFactory getInstance() {
    return instance;
  }

  private final ServiceLoader<SchemaConfig> schemaConfigServiceLoader;

  public SchemaConfig constructConfig(Properties properties) throws IOException {
    if(properties.contains("schema")){
      Properties formatProperties = (Properties) properties.get("schema");
      String formatName = formatProperties.getProperty("format", "RAW");
      for (SchemaConfig config : schemaConfigServiceLoader) {
        if (config.getFormat().equalsIgnoreCase(formatName)) {
          Map<String, Object> map = new LinkedHashMap<>();
          properties.forEach((key, value) -> map.put(key.toString(), value));
          return config.getInstance(map);
        }
      }
    }
    throw new IOException("Unknown schema config found");
  }

  public SchemaConfig constructConfig(byte[] rawPayload) throws IOException {
    return constructConfig(new String(rawPayload));
  }

  public SchemaConfig constructConfig(String payload) throws IOException {
    JSONObject schemaJson = new JSONObject(payload);
    if (!schemaJson.has("schema")) {
      throw new IOException("Not a valid schema config");
    }

    schemaJson = schemaJson.getJSONObject("schema");
    if (!schemaJson.has("format")) {
      throw new IOException("Not a valid schema config");
    }

    String formatName = schemaJson.getString("format");
    for (SchemaConfig config : schemaConfigServiceLoader) {
      if (config.getFormat().equalsIgnoreCase(formatName)) {
        return config.getInstance(schemaJson.toMap());
      }
    }
    throw new IOException("Unknown schema config found");
  }

  private SchemaConfigFactory() {
    schemaConfigServiceLoader = ServiceLoader.load(SchemaConfig.class);
  }

}
