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

  public SchemaConfig constructConfig(Map<String, Object> properties) throws IOException {
    if(properties.containsKey("schema")){
      Map<String, Object> formatMap = (Map) properties.get("schema");
      Object formatName = formatMap.get("format");
      if(formatName == null){
        formatName = "RAW";
      }
      for (SchemaConfig config : schemaConfigServiceLoader) {
        if (config.getFormat().equalsIgnoreCase(formatName.toString())) {
          return config.getInstance(formatMap);
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
