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
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestCsv {

  @Test
  void testCsvSchemaConfig() {
    SchemaConfig config = new CsvSchemaConfig("key1, val1, v1l2");
    Assertions.assertEquals("CSV", config.getFormat());
    String packed = config.pack();
    JSONObject jsonObject = new JSONObject(packed);
    Assertions.assertEquals("CSV", jsonObject.getJSONObject("schema").get("format"));
  }

  @Test
  void testSchemaReload() throws IOException {
    SchemaConfig config = new CsvSchemaConfig("key1, val1, v1l2");
    Assertions.assertEquals("CSV", config.getFormat());
    String packed = config.pack();
    SchemaConfig parsed = SchemaConfigFactory.getInstance().constructConfig(packed);
    Assertions.assertEquals("CSV", parsed.getFormat());
  }
}