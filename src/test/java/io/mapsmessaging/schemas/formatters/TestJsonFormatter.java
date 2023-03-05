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

package io.mapsmessaging.schemas.formatters;


import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestJsonFormatter extends BaseTest {


  byte[] pack(io.mapsmessaging.schemas.formatters.Person p) throws IOException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("stringId", p.getStringId());
    jsonObject.put("longId", p.getLongId());
    jsonObject.put("intId", p.getIntId());
    jsonObject.put("floatId", p.getFloatId());
    jsonObject.put("doubleId", p.getDoubleId());
    return jsonObject.toString(2).getBytes();
  }

  @Override
  List<byte[]> packList(List<io.mapsmessaging.schemas.formatters.Person> list) throws IOException {
    List<byte[]> packed = new ArrayList<>();
    for (io.mapsmessaging.schemas.formatters.Person p : list) {
      packed.add(pack(p));
    }
    return packed;
  }

  @Override
  SchemaConfig getSchema() throws IOException {
    return new JsonSchemaConfig();
  }

  @Test
  void testStructuredLookups() throws IOException {
    SchemaConfig config = new JsonSchemaConfig();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);

    JSONObject top = new JSONObject();
    for (int x = 0; x < 10; x++) {
      top.put("" + x, x);
    }
    JSONObject next = new JSONObject();
    for (int x = 0; x < 10; x++) {
      next.put("" + x, x + 10);
    }
    top.put("next", next);
    ParsedObject parsed = formatter.parse(top.toString(2).getBytes());
    Assertions.assertEquals(11, parsed.get("next.1"));
    Assertions.assertEquals(1, parsed.get("1"));
  }


  @Test
  void testArrayLookups() throws IOException {
    SchemaConfig config = new JsonSchemaConfig();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);

    JSONArray jsonArray = new JSONArray();
    for (int x = 0; x < 10; x++) {
      jsonArray.put(x);
    }
    JSONObject top = new JSONObject();
    top.put("arr", jsonArray);
    ParsedObject parsed = formatter.parse(top.toString(2).getBytes());
    Assertions.assertEquals(1, parsed.get("arr[1]"));
    Assertions.assertEquals(0, parsed.get("arr[0]"));
    Assertions.assertEquals(9, parsed.get("arr[9]"));
    Assertions.assertNull(parsed.get("arr[10]"));
    Assertions.assertNull(parsed.get("invalidEntry"));
  }

}
