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
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CborSchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import io.mapsmessaging.schemas.config.impl.MessagePackSchemaConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestSchemaUint64Precision {

  private static final String SCHEMA = """
      {
        "type":"object",
        "properties":{
          "id":{"type":"integer","x-cdr-type":"uint64"},
          "signed":{"type":"integer"},
          "temperature":{"type":"number"},
          "nested":{"type":"object","properties":{"id":{"type":"integer","x-cdr-type":"uint64"}}},
          "ids":{"type":"array","items":{"type":"integer","x-cdr-type":"uint64"}}
        }
      }
      """;

  private static final List<String> BOUNDARIES = List.of(
      "0", "1", "9007199254740993", "9223372036854775807",
      "9223372036854775808", "18446744073709551615");

  private SchemaConfig configuration(String name) {
    JsonObject schema = JsonParser.parseString(SCHEMA).getAsJsonObject();
    return switch (name) {
      case "JSON" -> {
        JsonSchemaConfig config = new JsonSchemaConfig();
        config.setSchema(schema);
        yield config;
      }
      case "CBOR" -> {
        CborSchemaConfig config = new CborSchemaConfig();
        config.setSchema(schema);
        yield config;
      }
      case "MessagePack" -> {
        MessagePackSchemaConfig config = new MessagePackSchemaConfig();
        config.setSchema(schema);
        yield config;
      }
      default -> throw new IllegalArgumentException("Unknown format " + name);
    };
  }

  @Test
  void schemaDeclaredUint64RemainsExactInAllThreeFormats() throws IOException {
    for (String formatName : List.of("JSON", "CBOR", "MessagePack")) {
      MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(configuration(formatName));
      for (String value : BOUNDARIES) {
        JsonObject sample = JsonParser.parseString("""
            {
              "id":%s,"signed":-12,"temperature":1.25,
              "nested":{"id":18446744073709551615},
              "ids":[0,9007199254740993,9223372036854775808]
            }
            """.formatted(value)).getAsJsonObject();
        byte[] payload = formatter.parseFromJson(sample);
        assertEquals(sample, formatter.parseToJson(payload, ParseMode.STRICT), formatName + " " + value);

        ParsedObject parsed = formatter.parse(payload, ParseMode.STRICT);
        assertEquals(new BigInteger(value), parsed.get("id"), formatName + " " + value);
        assertEquals(new BigInteger("18446744073709551615"), parsed.get("nested.id"));
        assertEquals(new BigInteger("9007199254740993"), parsed.get("ids[1]"));
        assertEquals(new BigInteger("9223372036854775808"), parsed.get("ids[2]"));
        assertFalse(parsed.get("signed") instanceof BigInteger);
        assertFalse(parsed.get("temperature") instanceof BigInteger);
      }
    }
  }

  @Test
  void untypedIntegerDoesNotBecomeBigInteger() throws IOException {
    JsonObject schema = JsonParser.parseString("""
        {"type":"object","properties":{"id":{"type":"integer"},"temperature":{"type":"number"}}}
        """).getAsJsonObject();
    JsonSchemaConfig config = new JsonSchemaConfig();
    config.setSchema(schema);
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);
    JsonObject sample = JsonParser.parseString("""
        {"id":42,"temperature":1.5}
        """).getAsJsonObject();
    ParsedObject parsed = formatter.parse(formatter.parseFromJson(sample), ParseMode.STRICT);
    assertFalse(parsed.get("id") instanceof BigInteger);
    assertFalse(parsed.get("temperature") instanceof BigInteger);
  }

  @Test
  void invalidUnsignedValuesCannotBeEncodedAsCborOrMessagePack() throws IOException {
    for (String name : List.of("CBOR", "MessagePack")) {
      MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(configuration(name));
      for (String value : List.of("-1", "18446744073709551616", "1.5", "\"42\"")) {
        JsonObject sample = JsonParser.parseString("{\"id\":" + value + "}").getAsJsonObject();
        assertThrows(IOException.class, () -> formatter.parseFromJson(sample), name + " " + value);
      }
    }
  }
}
