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
import io.mapsmessaging.schemas.config.impl.CdrSchemaConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestCdrUint64Precision {

  private static final List<String> UINT64_BOUNDARIES = List.of(
      "0", "1", "9007199254740993", "9223372036854775807",
      "9223372036854775808", "18446744073709551615");

  private MessageFormatter formatter(String schema) throws IOException {
    CdrSchemaConfig config = new CdrSchemaConfig();
    config.setSchema(JsonParser.parseString(schema).getAsJsonObject());
    return MessageFormatterFactory.getInstance().getFormatter(config);
  }

  @Test
  void explicitUint64PreservesEveryBoundaryInParsedObjectAndJson() throws IOException {
    for (boolean littleEndian : new boolean[]{true, false}) {
      MessageFormatter format = formatter("""
          {"littleEndian":%s,"fields":[{"name":"identifier","type":"uint64"}]}
          """.formatted(littleEndian));

      for (String decimal : UINT64_BOUNDARIES) {
        JsonObject input = JsonParser.parseString("{\"identifier\":" + decimal + "}").getAsJsonObject();
        byte[] wire = format.parseFromJson(input);
        assertEquals(input, format.parseToJson(wire, ParseMode.STRICT));
        ParsedObject parsed = format.parse(wire, ParseMode.STRICT);
        assertEquals(new BigInteger(decimal), parsed.get("identifier"), "unsigned " + decimal);
        assertInstanceOf(BigInteger.class, parsed.get("identifier"));
        assertSame(parsed.getReferenced().getClass(), input.getClass());
      }
    }
  }

  @Test
  void nestedCollectionsAndAliasPreserveUnsignedTypeOnly() throws IOException {
    MessageFormatter format = formatter("""
        {"fields":[
          {"name":"signed","type":"int64"},
          {"name":"counter","type":"uint32"},
          {"name":"position","type":"float64"},
          {"name":"values","type":"array","length":2,"element":"ulonglong"},
          {"name":"nested","type":"struct","fields":[
            {"name":"ids","type":"sequence","element":"uint64"},
            {"name":"reading","type":"int32"}
          ]}
        ]}
        """);
    JsonObject data = JsonParser.parseString("""
        {
          "signed":-9223372036854775808,
          "counter":4294967295,
          "position":1.25,
          "values":[9007199254740993,18446744073709551615],
          "nested":{"ids":[9223372036854775808,1],"reading":-100}
        }
        """).getAsJsonObject();
    byte[] wire = format.parseFromJson(data);
    ParsedObject parsed = format.parse(wire, ParseMode.STRICT);

    assertEquals(new BigInteger("9007199254740993"), parsed.get("values[0]"));
    assertEquals(new BigInteger("18446744073709551615"), parsed.get("values[1]"));
    assertEquals(new BigInteger("9223372036854775808"), parsed.get("nested.ids[0]"));
    assertEquals(BigInteger.ONE, parsed.get("nested.ids[1]"));

    assertFalse(parsed.get("signed") instanceof BigInteger);
    assertFalse(parsed.get("counter") instanceof BigInteger);
    assertFalse(parsed.get("position") instanceof BigInteger);
    assertFalse(parsed.get("nested.reading") instanceof BigInteger);
    assertEquals(data, format.parseToJson(wire, ParseMode.STRICT));
  }

  @Test
  void nestedStructArraysPreserveBigInteger() throws IOException {
    MessageFormatter format = formatter("""
        {"fields":[{"name":"samples","type":"array","length":2,"element":{
          "type":"struct","fields":[{"name":"id","type":"uint64"},{"name":"active","type":"boolean"}]
        }}]}
        """);
    JsonObject data = JsonParser.parseString("""
        {"samples":[{"id":18446744073709551615,"active":true},{"id":0,"active":false}]}
        """).getAsJsonObject();
    ParsedObject parsed = format.parse(format.parseFromJson(data), ParseMode.STRICT);
    assertEquals(new BigInteger("18446744073709551615"), parsed.get("samples[0].id"));
    assertEquals(BigInteger.ZERO, parsed.get("samples[1].id"));
    assertEquals(data, format.parseToJson(format.parseFromJson(data), ParseMode.STRICT));
  }

  @Test
  void unsignedRangeValidationRemainsUnchanged() throws IOException {
    MessageFormatter format = formatter("""
        {"fields":[{"name":"value","type":"uint64"}]}
        """);
    for (String invalid : List.of("-1", "18446744073709551616", "1.5", "true", "null")) {
      JsonObject value = JsonParser.parseString("{\"value\":" + invalid + "}").getAsJsonObject();
      assertThrows(IOException.class, () -> format.parseFromJson(value), invalid);
    }
  }

  @Test
  void signedAndFloatingPointTypesAreNotConvertedToBigInteger() throws IOException {
    MessageFormatter format = formatter("""
        {"fields":[
          {"name":"signed","type":"int64"},
          {"name":"small","type":"int32"},
          {"name":"unsigned32","type":"uint32"},
          {"name":"single","type":"float32"},
          {"name":"doubleValue","type":"float64"}
        ]}
        """);
    JsonObject data = JsonParser.parseString("""
        {"signed":-9223372036854775808,"small":-42,"unsigned32":4294967295,"single":1.5,"doubleValue":2.25}
        """).getAsJsonObject();
    ParsedObject parsed = format.parse(format.parseFromJson(data), ParseMode.STRICT);
    for (String name : List.of("signed", "small", "unsigned32", "single", "doubleValue")) {
      assertFalse(parsed.get(name) instanceof BigInteger, name);
    }
    assertEquals(data, format.parseToJson(format.parseFromJson(data), ParseMode.STRICT));
  }
}
