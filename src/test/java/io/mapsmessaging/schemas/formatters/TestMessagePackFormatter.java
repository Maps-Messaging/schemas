/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.schemas.formatters;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.MessagePackSchemaConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.msgpack.jackson.dataformat.MessagePackFactory;

import java.io.IOException;
import java.util.*;

class TestMessagePackFormatter extends BaseTest {

  byte[] pack(Person p) throws IOException {
    Map<String, Object> map = new LinkedHashMap<>();
    map.put("stringId", p.getStringId());
    map.put("longId", p.getLongId());
    map.put("intId", p.getIntId());
    map.put("floatId", p.getFloatId());
    map.put("doubleId", p.getDoubleId());
    ObjectMapper mapper = new ObjectMapper(new MessagePackFactory());
    return mapper.writeValueAsBytes(map);
  }

  @Override
  List<byte[]> packList(List<Person> list) {
    List<byte[]> packed = new ArrayList<>();
    for (Person p : list) {
      try {
        packed.add(pack(p));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    return packed;
  }

  @Override
  SchemaConfig getSchema() {
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
    return new MessagePackSchemaConfig(jsonSchema);
  }

  @Test
  void invalidMessagePack() throws IOException {
    SchemaConfig config = getSchema();
    config.setUniqueId(UUID.randomUUID());
    config.setSource("test");
    config.setVersion("1.0");
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);

    // This doesn't match the required fields in schema
    Map<String, Object> invalid = new HashMap<>();
    invalid.put("something_else", "test");
    byte[] payload = new ObjectMapper(new MessagePackFactory()).writeValueAsBytes(invalid);
    Assertions.assertNotNull(formatter.parse(payload));
  }

  @Test
  void testStructuredLookups() throws IOException {
    SchemaConfig config = new MessagePackSchemaConfig();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);

    Map<String, Object> map = new HashMap<>();
    Map<String, Object> nested = new HashMap<>();
    for (int x = 0; x < 10; x++) {
      map.put("" + x, x);
      nested.put("" + x, x + 10);
    }
    map.put("next", nested);

    byte[] payload = new ObjectMapper(new MessagePackFactory()).writeValueAsBytes(map);
    ParsedObject parsed = formatter.parse(payload);

    Assertions.assertEquals(11, ((Number) parsed.get("next.1")).intValue());
    Assertions.assertEquals(1, ((Number) parsed.get("1")).intValue());
  }

  @Test
  void testArrayLookups() throws IOException {
    SchemaConfig config = new MessagePackSchemaConfig();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);

    Map<String, Object> map = new HashMap<>();
    List<Integer> array = new ArrayList<>();
    for (int x = 0; x < 10; x++) {
      array.add(x);
    }
    map.put("arr", array);

    byte[] payload = new ObjectMapper(new MessagePackFactory()).writeValueAsBytes(map);
    ParsedObject parsed = formatter.parse(payload);

    Assertions.assertEquals(1, ((Number) parsed.get("arr[1]")).intValue());
    Assertions.assertEquals(0, ((Number) parsed.get("arr[0]")).intValue());
    Assertions.assertEquals(9, ((Number) parsed.get("arr[9]")).intValue());
    Assertions.assertNull(parsed.get("arr[10]"));
    Assertions.assertNull(parsed.get("invalidEntry"));
  }
}
