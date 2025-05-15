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
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.schemas.config.impl.RawSchemaConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import static io.mapsmessaging.schemas.config.SchemaConfigFactory.gson;

class TestConfigConstructors {

  @Test
  void validSchemaLoad() throws IOException {
    SchemaConfig good = new RawSchemaConfig();
    good.setUniqueId(UUID.randomUUID());
    JsonObject schema = new JsonObject();
    schema.add("schema", good.packData());
    String config = schema.toString();

    Assertions.assertNotNull(SchemaConfigFactory.getInstance().constructConfig(config));
    Assertions.assertNotNull(SchemaConfigFactory.getInstance().constructConfig(config.getBytes()));
    Type type = new TypeToken<Map<String, Object>>() {
    }.getType();
    Map<String, Object> map = gson.fromJson(schema, type);
    Assertions.assertNotNull(SchemaConfigFactory.getInstance().constructConfig(map));
  }


  @Test
  void invalidSchemaLoad() throws IOException {
    SchemaConfig bad = new BadSchema();
    JsonObject schema = new JsonObject();
    schema.add("schema", bad.packData());
    String config = schema.toString();
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(config));
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(config.getBytes()));
    Type type = new TypeToken<Map<String, Object>>() {
    }.getType();
    Map<String, Object> map = gson.fromJson(schema, type);
    Assertions.assertThrowsExactly(IOException.class, () ->
        SchemaConfigFactory.getInstance().constructConfig(map)
    );
  }

  @Test
  void invalidDataLoad() {
    Gson gson = new Gson();

    // Empty object as string
    Assertions.assertThrowsExactly(IOException.class, () ->
        SchemaConfigFactory.getInstance().constructConfig("{}"));

    // Invalid JSON bytes
    Assertions.assertThrowsExactly(IllegalStateException.class, () ->
        SchemaConfigFactory.getInstance().constructConfig("".getBytes(StandardCharsets.UTF_8)));

    // Empty map
    Assertions.assertThrowsExactly(IOException.class, () ->
        SchemaConfigFactory.getInstance().constructConfig(new LinkedHashMap<>()));

    // schema = 2 (invalid schema structure)
    JsonObject invalidSchema1 = new JsonObject();
    invalidSchema1.addProperty("schema", 2);
    String config1 = gson.toJson(invalidSchema1);
    Assertions.assertThrowsExactly(IOException.class, () ->
        SchemaConfigFactory.getInstance().constructConfig(config1));
    Assertions.assertThrowsExactly(IOException.class, () -> {
      Type type = new TypeToken<Map<String, Object>>() {
      }.getType();
      Map<String, Object> map = gson.fromJson(invalidSchema1, type);
      SchemaConfigFactory.getInstance().constructConfig(map);
    });

    // schema = {} (still invalid, no "format" field)
    JsonObject invalidSchema2 = new JsonObject();
    invalidSchema2.add("schema", new JsonObject());
    String config2 = gson.toJson(invalidSchema2);
    Assertions.assertThrowsExactly(IOException.class, () ->
        SchemaConfigFactory.getInstance().constructConfig(config2));
    Assertions.assertThrowsExactly(IOException.class, () -> {
      Type type = new TypeToken<Map<String, Object>>() {
      }.getType();
      Map<String, Object> map = gson.fromJson(invalidSchema2, type);
      SchemaConfigFactory.getInstance().constructConfig(map);
    });
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
    protected JsonObject packData() {
      JsonObject data = new JsonObject();
      packData(data);
      return data;
    }

    @Override
    protected SchemaConfig getInstance(Map<String, Object> config) {
      return this;
    }
  }

}
