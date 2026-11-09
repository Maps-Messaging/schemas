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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.impl.CdrSchemaConfig;
import io.mapsmessaging.schemas.formatters.impl.cdr.CdrIdlParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestCdrPersonIdlResource {

  private String resource(String name) throws IOException {
    try (InputStream stream = getClass().getClassLoader().getResourceAsStream(name)) {
      if (stream == null) {
        throw new IOException("Missing Person test resource: " + name);
      }
      return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  @Test
  void personIdlFileCompilesToExpectedFields() throws IOException {
    JsonObject compiled = CdrIdlParser.parse(resource("Person.idl"), "Person");
    List<String> fields = new ArrayList<>();
    for (JsonElement field : compiled.getAsJsonArray("fields")) {
      fields.add(field.getAsJsonObject().get("name").getAsString());
    }
    assertEquals(List.of("stringId", "longId", "intId", "floatId", "doubleId", "booleanId", "bytesId", "arrayId", "enumId", "timestampId"), fields);
    assertEquals("int64", compiled.getAsJsonArray("fields").get(1).getAsJsonObject().get("type").getAsString());
    assertEquals("array", compiled.getAsJsonArray("fields").get(7).getAsJsonObject().get("type").getAsString());
  }

  @Test
  void personIdlFileLoadsThroughFactoryAndMatchesPersonCdr() throws IOException {
    CdrSchemaConfig idlConfig = new CdrSchemaConfig();
    idlConfig.setIdl(resource("Person.idl"), "Person");
    CdrSchemaConfig cdrConfig = new CdrSchemaConfig();
    cdrConfig.setSchema(JsonParser.parseString(resource("Person.cdr")).getAsJsonObject());

    MessageFormatter fromIdl = MessageFormatterFactory.getInstance().getFormatter(idlConfig);
    MessageFormatter fromSchema = MessageFormatterFactory.getInstance().getFormatter(cdrConfig);

    JsonObject person = JsonParser.parseString("""
        {
          "stringId":"Alice","longId":-9223372036854775807,"intId":12345,"floatId":12.5,
          "doubleId":123456.875,"booleanId":true,"bytesId":"AQIDBAUGBwg=",
          "arrayId":[1,2,3,4,5],"enumId":"GREEN","timestampId":1794301200000
        }
        """).getAsJsonObject();

    byte[] encoded = fromIdl.parseFromJson(person);
    assertArrayEquals(fromSchema.parseFromJson(person), encoded);
    assertEquals(person, fromIdl.parseToJson(encoded, ParseMode.STRICT));
    assertEquals(person, fromSchema.parseToJson(encoded, ParseMode.STRICT));

    ParsedObject parsed = fromIdl.parse(encoded, ParseMode.STRICT);
    assertEquals("Alice", parsed.get("stringId"));
    assertEquals(3, ((Number) parsed.get("arrayId[2]")).intValue());
    assertEquals(10, parsed.getKeys().size());
  }
}
