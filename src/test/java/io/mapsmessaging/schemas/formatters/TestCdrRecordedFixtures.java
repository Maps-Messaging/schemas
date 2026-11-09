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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.impl.CdrSchemaConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestCdrRecordedFixtures {

  private static final String ROOT = "cdr/ros2/";

  private String resource(String name) throws IOException {
    try (InputStream input = getClass().getClassLoader().getResourceAsStream(ROOT + name)) {
      if (input == null) {
        throw new IOException("Missing CDR interoperability fixture: " + name);
      }
      return new String(input.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private byte[] fixtureBytes(String name) throws IOException {
    return HexFormat.of().parseHex(resource(name).replaceAll("\\s+", ""));
  }

  private JsonObject fixtureJson(String name) throws IOException {
    return JsonParser.parseString(resource(name)).getAsJsonObject();
  }

  private MessageFormatter formatter(String schema) throws IOException {
    CdrSchemaConfig config = new CdrSchemaConfig();
    config.setSchema(fixtureJson(schema));
    return MessageFormatterFactory.getInstance().getFormatter(config);
  }

  @Test
  void recordedRos2BasicTypesDecodeToKnownValues() throws IOException {
    MessageFormatter formatter = formatter("basic-types.schema.json");
    JsonObject expected = fixtureJson("basic-types.expected.json");
    assertEquals(expected, formatter.parseToJson(fixtureBytes("basic-types.hex"), ParseMode.STRICT));
  }

  @Test
  void recordedRos2BasicTypesAreReencodedByteForByte() throws IOException {
    MessageFormatter formatter = formatter("basic-types.schema.json");
    assertArrayEquals(fixtureBytes("basic-types.hex"), formatter.parseFromJson(fixtureJson("basic-types.expected.json")));
  }

  @Test
  void recordedRos2PrimitiveArraysDecodeToKnownValues() throws IOException {
    MessageFormatter formatter = formatter("arrays-primitives.schema.json");
    JsonObject expected = fixtureJson("arrays-primitives.expected.json");
    assertEquals(expected, formatter.parseToJson(fixtureBytes("arrays-primitives.hex"), ParseMode.STRICT));
  }

  @Test
  void recordedRos2PrimitiveArraysAreReencodedByteForByte() throws IOException {
    MessageFormatter formatter = formatter("arrays-primitives.schema.json");
    assertArrayEquals(fixtureBytes("arrays-primitives.hex"), formatter.parseFromJson(fixtureJson("arrays-primitives.expected.json")));
  }

  @Test
  void primitiveArrayFixtureIsExactPrefixOfOriginalRecordedMessage() throws IOException {
    byte[] full = fixtureBytes("arrays-original-full.hex");
    byte[] prefix = fixtureBytes("arrays-primitives.hex");
    assertEquals(696, full.length);
    assertEquals(148, prefix.length);
    assertArrayEquals(prefix, Arrays.copyOf(full, prefix.length));
  }

  @Test
  void fullRecordedArrayCannotBeSilentlyAcceptedAsTruncatedSchema() throws IOException {
    MessageFormatter formatter = formatter("arrays-primitives.schema.json");
    byte[] full = fixtureBytes("arrays-original-full.hex");
    assertThrows(ParseException.class, () -> formatter.parseToJson(full, ParseMode.STRICT));
  }

  @Test
  void recordedFixtureTruncationFailsAtEachByteBoundary() throws IOException {
    MessageFormatter formatter = formatter("basic-types.schema.json");
    byte[] source = fixtureBytes("basic-types.hex");
    for (int length = 0; length < source.length; length++) {
      byte[] truncated = Arrays.copyOf(source, length);
      assertThrows(ParseException.class, () -> formatter.parseToJson(truncated, ParseMode.STRICT), "truncated at " + length);
    }
  }
  @Test
  void allSevenOriginalRos2RecordsAreRetainedByteForByte() throws IOException {
    JsonArray rows = JsonParser.parseString(resource("all-recorded-messages.json")).getAsJsonObject().getAsJsonArray("rows");
    assertEquals(7, rows.size());
    int basicCount = 0;
    int arrayCount = 0;
    for (JsonElement rowElement : rows) {
      JsonObject row = rowElement.getAsJsonObject();
      StringBuilder hex = new StringBuilder();
      for (JsonElement chunk : row.getAsJsonArray("hexChunks")) {
        hex.append(chunk.getAsString());
      }
      byte[] sample = HexFormat.of().parseHex(hex.toString());
      String topic = row.get("topic").getAsString();
      if (topic.equals("test_msgs/msg/BasicTypes")) {
        basicCount++;
        assertArrayEquals(fixtureBytes("basic-types.hex"), sample);
      } else if (topic.equals("test_msgs/msg/Arrays")) {
        arrayCount++;
        assertArrayEquals(fixtureBytes("arrays-original-full.hex"), sample);
      } else {
        throw new IOException("Unknown ROS 2 recorded message type: " + topic);
      }
      assertEquals(row.get("bytes").getAsInt(), sample.length);
    }
    assertEquals(3, basicCount);
    assertEquals(4, arrayCount);
  }


}
