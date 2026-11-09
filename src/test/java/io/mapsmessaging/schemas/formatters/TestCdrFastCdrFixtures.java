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
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.impl.CdrSchemaConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestCdrFastCdrFixtures {

  private JsonArray testVectors() throws IOException {
    String name = "cdr/fastcdr/verified-xcdr1-vectors.json";
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
      if (stream == null) {
        throw new IOException("Missing Fast-CDR golden-vector resource: " + name);
      }
      return JsonParser.parseReader(new InputStreamReader(stream, StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("cases");
    }
  }

  @Test
  void upstreamFastCdrGoldenVectorsDecodeAndReencode() throws IOException {
    JsonArray cases = testVectors();
    assertEquals(3, cases.size());

    for (int index = 0; index < cases.size(); index++) {
      JsonObject vector = cases.get(index).getAsJsonObject();
      String name = vector.get("name").getAsString();
      JsonObject expected = vector.getAsJsonObject("expected");

      for (boolean littleEndian : new boolean[]{false, true}) {
        JsonObject schema = vector.getAsJsonObject("schema").deepCopy();
        schema.addProperty("littleEndian", littleEndian);
        CdrSchemaConfig config = new CdrSchemaConfig();
        config.setSchema(schema);
        MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);

        String property = littleEndian ? "littleEndian" : "bigEndian";
        byte[] wire = HexFormat.of().parseHex(vector.get(property).getAsString());
        String label = name + " " + property;

        assertEquals(expected, formatter.parseToJson(wire, ParseMode.STRICT), "decode " + label);
        assertArrayEquals(wire, formatter.parseFromJson(expected), "encode " + label);
        assertThrows(ParseException.class,
            () -> formatter.parseToJson(java.util.Arrays.copyOf(wire, wire.length - 1), ParseMode.STRICT), "truncate " + label);
      }
    }
  }
}
