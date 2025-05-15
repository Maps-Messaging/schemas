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

package io.mapsmessaging.schemas.formatters;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

class TestJsonFormatter extends BaseTest {


  byte[] pack(io.mapsmessaging.schemas.formatters.Person p) {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("stringId", p.getStringId());
    jsonObject.addProperty("longId", p.getLongId());
    jsonObject.addProperty("intId", p.getIntId());
    jsonObject.addProperty("floatId", p.getFloatId());
    jsonObject.addProperty("doubleId", p.getDoubleId());
    return jsonObject.toString().getBytes();
  }

  @Override
  List<byte[]> packList(List<io.mapsmessaging.schemas.formatters.Person> list) {
    List<byte[]> packed = new ArrayList<>();
    for (io.mapsmessaging.schemas.formatters.Person p : list) {
      packed.add(pack(p));
    }
    return packed;
  }

  @Override
  SchemaConfig getSchema()  {
    String jsonSchema = "{\n" +
        "  \"$schema\": \"http://json-schema.org/draft-07/schema#\",\n" +
        "  \"type\": \"object\",\n" +
        "  \"properties\": {\n" +
        "    \"stringId\": { \"type\": \"string\" },\n" +
        "    \"longId\": { \"type\": \"number\" },\n" +
        "    \"intId\": { \"type\": \"number\" },\n" +
        "    \"floatId\": { \"type\": \"number\" },\n" +
        "    \"doubleId\": { \"type\": \"number\" }\n" +
        "  },\n" +
        " \"required\": [\"stringId\", \"longId\", \"intId\", \"floatId\", \"doubleId\"],\n" +
        "  \"additionalProperties\": false\n" +
        "}";
    return new JsonSchemaConfig(jsonSchema);
  }

  @Test
  void invalidJson() throws IOException {
    SchemaConfig config = getSchema();
    config.setUniqueId(UUID.randomUUID());
    config.setSource("test");
    config.setVersion("1.0");
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("something_different", "hello");
    Assertions.assertNotNull(formatter.parse(jsonObject.toString().getBytes()));

  }

  @Test
  void testStructuredLookups() throws IOException {
    SchemaConfig config = new JsonSchemaConfig();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);

    JsonObject top = new JsonObject();
    for (int x = 0; x < 10; x++) {
      top.addProperty("" + x, x);
    }
    JsonObject next = new JsonObject();
    for (int x = 0; x < 10; x++) {
      next.addProperty("" + x, x + 10);
    }
    top.add("next", next);
    ParsedObject parsed = formatter.parse(top.toString().getBytes());
    Assertions.assertEquals(11, ((Number) parsed.get("next.1")).intValue());
    Assertions.assertEquals(1, ((Number) parsed.get("1")).intValue());
  }


  @Test
  void testArrayLookups() throws IOException {
    SchemaConfig config = new JsonSchemaConfig();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);

    JsonArray jsonArray = new JsonArray();
    for (int x = 0; x < 10; x++) {
      jsonArray.add(x);
    }
    JsonObject top = new JsonObject();
    top.add("arr", jsonArray);
    ParsedObject parsed = formatter.parse(top.toString().getBytes());
    Assertions.assertEquals(1, ((Number) parsed.get("arr[1]")).intValue());
    Assertions.assertEquals(0, ((Number) parsed.get("arr[0]")).intValue());
    Assertions.assertEquals(9, ((Number) parsed.get("arr[9]")).intValue());
    Assertions.assertNull(parsed.get("arr[10]"));
    Assertions.assertNull(parsed.get("invalidEntry"));
  }

}
