/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParseException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class OffsetDateTimeAdapterCoverageTest {

  private final OffsetDateTimeAdapter adapter = new OffsetDateTimeAdapter();

  @Test
  void writesNullAndNormalisesValuesToUtc() throws Exception {
    StringWriter nullBuffer = new StringWriter();
    JsonWriter nullWriter = new JsonWriter(nullBuffer);
    adapter.write(nullWriter, null);
    nullWriter.flush();
    assertEquals("null", nullBuffer.toString());

    StringWriter valueBuffer = new StringWriter();
    JsonWriter valueWriter = new JsonWriter(valueBuffer);
    adapter.write(valueWriter, OffsetDateTime.parse("2026-09-28T20:15:30+10:00"));
    valueWriter.flush();
    assertEquals(jsonString("2026-09-28T10:15:30Z"), valueBuffer.toString());
  }

  @Test
  void readsNullNumberIsoLocalLegacyAndEpochStringForms() throws Exception {
    assertNull(read("null"));
    assertEquals(
        Instant.ofEpochSecond(1_700_000_000L).atOffset(ZoneOffset.UTC),
        read("1700000000")
    );
    assertEquals(
        Instant.ofEpochMilli(1_700_000_000_000L).atOffset(ZoneOffset.UTC),
        read("1700000000000")
    );

    OffsetDateTime expected = OffsetDateTime.of(2026, 9, 28, 10, 15, 30, 0, ZoneOffset.UTC);
    assertEquals(expected, read(jsonString("2026-09-28T10:15:30Z")));
    assertEquals(expected, read(jsonString("2026-09-28T10:15:30")));
    assertEquals(expected, read(jsonString("2026-09-28 10:15:30")));
    assertEquals(
        Instant.ofEpochSecond(1_700_000_000L).atOffset(ZoneOffset.UTC),
        read(jsonString("1700000000"))
    );
    assertNull(read(jsonString("   ")));
  }

  @Test
  void rejectsUnsupportedText() {
    assertThrows(JsonParseException.class, () -> read(jsonString("not-a-date")));
  }

  private OffsetDateTime read(String json) throws Exception {
    return adapter.read(new JsonReader(new StringReader(json)));
  }

  private String jsonString(String value) {
    return '"' + value + '"';
  }
}
