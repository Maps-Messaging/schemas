/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package io.mapsmessaging.schemas.formatters;

import io.mapsmessaging.schemas.formatters.impl.RawFormatter;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

class TestRawFormatter {

  @Test
  void testJSONpacker() {
    RawFormatter rawFormatter = new RawFormatter();
    JSONObject jsonObject = rawFormatter.parseToJson("Hi there".getBytes());
    Assertions.assertEquals("Hi there", new String(Base64.getDecoder().decode(jsonObject.getString("payload"))));
  }

  @Test
  void testNullResponse() {
    RawFormatter rawFormatter = new RawFormatter();
    Assertions.assertNull(rawFormatter.parse("what ever is here can not be parsed".getBytes()).get("whatEver"));
  }
}
