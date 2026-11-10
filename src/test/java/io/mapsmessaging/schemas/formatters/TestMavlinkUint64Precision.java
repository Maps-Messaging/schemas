/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package io.mapsmessaging.schemas.formatters;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.impl.MavlinkSchemaConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestMavlinkUint64Precision {

  private MessageFormatter formatter() throws IOException {
    MavlinkSchemaConfig config = new MavlinkSchemaConfig();
    config.setDialect("common");
    return MessageFormatterFactory.getInstance().getFormatter(config);
  }

  private JsonObject message(String uid) {
    return JsonParser.parseString("""
        {
          "header":{"messageId":148,"systemId":1,"componentId":1,"sequence":2},
          "payload":{"uid":%s,"capabilities":9007199254740993,"flight_sw_version":1}
        }
        """.formatted(uid)).getAsJsonObject();
  }

  @Test
  void autopilotVersionUint64IsExactAcrossBinaryAndSelectors() throws IOException {
    MessageFormatter formatter = formatter();
    for (String value : List.of("0", "9007199254740993", "9223372036854775808", "18446744073709551615")) {
      byte[] frame = formatter.parseFromJson(message(value));
      JsonObject decoded = formatter.parseToJson(frame, ParseMode.STRICT).getAsJsonObject("payload");
      assertEquals(new BigInteger(value), decoded.get("uid").getAsBigInteger());
      assertEquals(new BigInteger("9007199254740993"), decoded.get("capabilities").getAsBigInteger());
      ParsedObject parsed = formatter.parse(frame, ParseMode.STRICT);
      assertEquals(new BigInteger(value), parsed.get("uid"));
      assertEquals(new BigInteger("9007199254740993"), parsed.get("capabilities"));
      assertFalse(parsed.get("flight_sw_version") instanceof BigInteger);
    }
  }

  @Test
  void rejectsUnsignedOverflowWithoutChangingOtherFields() throws IOException {
    MessageFormatter formatter = formatter();
    for (String value : List.of("-1", "18446744073709551616", "1.5", "\"12\"")) {
      assertThrows(IOException.class, () -> formatter.parseFromJson(message(value)), value);
    }
  }
}
