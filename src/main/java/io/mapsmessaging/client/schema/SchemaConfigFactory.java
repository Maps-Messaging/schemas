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
import java.util.ServiceLoader;
import org.json.JSONObject;

public class SchemaConfigFactory {

  private static final SchemaConfigFactory instance = new SchemaConfigFactory();

  public static SchemaConfigFactory getInstance() {
    return instance;
  }

  private final ServiceLoader<SchemaConfig> schemaConfigServiceLoader;

  public SchemaConfig parse(byte[] rawPayload) throws IOException {
    return parse(new String(rawPayload));
  }

  public SchemaConfig parse(String payload) throws IOException {
    JSONObject schemaJson = new JSONObject(payload);
    if (!schemaJson.has("schema")) {
      throw new IOException("Not a valid schema config");
    }

    schemaJson = schemaJson.getJSONObject("schema");
    if (!schemaJson.has("format")) {
      throw new IOException("Not a valid schema config");
    }

    for (SchemaConfig config : schemaConfigServiceLoader) {
      if (config.getFormat().equalsIgnoreCase(schemaJson.getString("format"))) {
        return config.getInstance(schemaJson);
      }
    }
    throw new IOException("Unknown schema config found");

  }

  private SchemaConfigFactory() {
    schemaConfigServiceLoader = ServiceLoader.load(SchemaConfig.class);
  }

}
