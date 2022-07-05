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

package io.mapsmessaging.schemas.formatters;


import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class TestJsonFormatter extends BaseTest {


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
}
