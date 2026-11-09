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

import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CdrSchemaConfig;
import io.mapsmessaging.schemas.formatters.impl.cdr.CdrWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Runs the same Person, selector, concurrent and invalid-input tests as the other formatters. */
class TestCdrBaseFormatter extends BaseTest {

  @Override
  List<byte[]> packList(List<Person> persons) {
    List<byte[]> packed = new ArrayList<>(persons.size());
    for (Person person : persons) {
      CdrWriter writer = new CdrWriter(true);
      byte[] name = person.getStringId().getBytes(StandardCharsets.UTF_8);
      writer.writeInteger(name.length + 1L, 4);
      writer.writeBytes(name);
      writer.writeBytes(new byte[]{0});
      writer.writeInteger(person.getLongId(), 8);
      writer.writeInteger(person.getIntId(), 4);
      writer.writeInteger(Float.floatToRawIntBits(person.getFloatId()), 4);
      writer.writeInteger(Double.doubleToRawLongBits(person.getDoubleId()), 8);
      packed.add(writer.toByteArray());
    }
    return packed;
  }

  @Override
  SchemaConfig getSchema() throws IOException {
    CdrSchemaConfig config = new CdrSchemaConfig();
    config.setSchema(JsonParser.parseString("""
        {
          "fields": [
            {"name":"stringId","type":"string"},
            {"name":"longId","type":"int64"},
            {"name":"intId","type":"int32"},
            {"name":"floatId","type":"float32"},
            {"name":"doubleId","type":"float64"}
          ]
        }
        """).getAsJsonObject());
    return config;
  }
}
