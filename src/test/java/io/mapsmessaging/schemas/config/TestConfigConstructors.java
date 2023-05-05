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

package io.mapsmessaging.schemas.config;

import io.mapsmessaging.schemas.config.impl.RawSchemaConfig;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

class TestConfigConstructors {

  @Test
  void validSchemaLoad() throws IOException {
    SchemaConfig good = new RawSchemaConfig();
    good.setUniqueId(UUID.randomUUID());
    JSONObject schema = new JSONObject();
    schema.put("schema", good.packData());
    String config = schema.toString(2);

    Assertions.assertNotNull(SchemaConfigFactory.getInstance().constructConfig(config));
    Assertions.assertNotNull(SchemaConfigFactory.getInstance().constructConfig(config.getBytes()));
    Assertions.assertNotNull(SchemaConfigFactory.getInstance().constructConfig(schema.toMap()));
  }


  @Test
  void invalidSchemaLoad() throws IOException {
    SchemaConfig bad = new BadSchema();
    JSONObject schema = new JSONObject();
    schema.put("schema", bad.packData());
    String config = schema.toString(2);
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(config));
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(config.getBytes()));
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(schema.toMap()));
  }

  @Test
  void invalidDataLoad() {
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(new JSONObject().toString()));

    Assertions.assertThrowsExactly(JSONException.class, () -> SchemaConfigFactory.getInstance().constructConfig("".getBytes()));
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(new LinkedHashMap<>()));

    JSONObject schema = new JSONObject();
    schema.put("schema", 2);
    String config = schema.toString(2);
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(config));
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(schema.toMap()));

    JSONObject schema1 = new JSONObject();
    schema1.put("schema", new JSONObject());
    String config1 = schema1.toString(2);
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(config1));
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(schema1.toMap()));

  }


  static class BadSchema extends SchemaConfig {

    protected BadSchema() {
      super("BAD");
      uniqueId = UUID.randomUUID().toString();
    }

    protected BadSchema(String format, Map<String, Object> config) {
      super(format, config);
    }

    @Override
    protected JSONObject packData() throws IOException {
      JSONObject data = new JSONObject();
      packData(data);
      return data;
    }

    @Override
    protected SchemaConfig getInstance(Map<String, Object> config) {
      return this;
    }
  }

}
