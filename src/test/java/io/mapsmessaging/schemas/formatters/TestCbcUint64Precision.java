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
import io.mapsmessaging.schemas.config.impl.CbcSchemaConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestCbcUint64Precision {

  private static final List<String> BOUNDARIES = List.of(
      "0", "1", "9007199254740993", "9223372036854775807",
      "9223372036854775808", "18446744073709551615");

  private MessageFormatter formatter() throws IOException {
    CbcSchemaConfig config = new CbcSchemaConfig();
    config.setSchema(JsonParser.parseString("""
        {
          "messageKey":0,
          "fields":[
            {"name":"id","type":"uint","size":64},
            {"name":"small","type":"uint","size":32},
            {"name":"signed","type":"int","size":64},
            {"name":"nested","type":"struct","fields":[{"name":"id","type":"uint","size":64}]}
          ]
        }
        """).getAsJsonObject());
    return MessageFormatterFactory.getInstance().getFormatter(config);
  }

  @Test
  void unscaledUnsigned64IsExactAcrossWireJsonAndParsedAccess() throws IOException {
    MessageFormatter formatter = formatter();
    for (String decimal : BOUNDARIES) {
      JsonObject sample = JsonParser.parseString("""
          {"id":%s,"small":42,"signed":-10,"nested":{"id":18446744073709551615}}
          """.formatted(decimal)).getAsJsonObject();
      byte[] wire = formatter.parseFromJson(sample);
      assertEquals(sample, formatter.parseToJson(wire, ParseMode.STRICT));
      ParsedObject parsed = formatter.parse(wire, ParseMode.STRICT);
      assertEquals(new BigInteger(decimal), parsed.get("id"), decimal);
      assertEquals(new BigInteger("18446744073709551615"), parsed.get("nested.id"));
      assertInstanceOf(Long.class, parsed.get("signed"));
      assertFalse(parsed.get("small") instanceof BigInteger);
    }
  }

  @Test
  void unsigned64RejectsInvalidValuesWithoutClipping() throws IOException {
    MessageFormatter formatter = formatter();
    for (String value : List.of("-1", "18446744073709551616", "1.5", "\"12\"")) {
      JsonObject sample = JsonParser.parseString("""
          {"id":%s,"small":42,"signed":-10,"nested":{"id":12}}
          """.formatted(value)).getAsJsonObject();
      assertThrows(IOException.class, () -> formatter.parseFromJson(sample), value);
    }
  }

  @Test
  void encodedUnsigned64RetainsExistingBitLayout() throws IOException {
    MessageFormatter formatter = formatter();
    JsonObject sample = JsonParser.parseString("""
        {"id":18446744073709551615,"small":0,"signed":0,"nested":{"id":0}}
        """).getAsJsonObject();
    byte[] wire = formatter.parseFromJson(sample);
    assertEquals(-1, wire[0]);
    assertEquals(-1, wire[7]);
    assertEquals(sample, formatter.parseToJson(wire, ParseMode.STRICT));
  }
}
