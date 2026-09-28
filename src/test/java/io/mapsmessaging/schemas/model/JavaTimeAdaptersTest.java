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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class JavaTimeAdaptersTest {

  private final Gson gson = JavaTimeAdapters.registerJavaTime(new GsonBuilder()).create();

  @Test
  void instantAdapterReadsAndWritesIsoAndEpochValues() {
    Instant value = Instant.parse("2026-09-28T10:15:30Z");

    assertEquals("\\\"2026-09-28T10:15:30Z\\\"", gson.toJson(value, Instant.class));
    assertEquals(value, gson.fromJson("\\\"2026-09-28T10:15:30Z\\\"", Instant.class));
    assertEquals(Instant.ofEpochSecond(1_700_000_000L), gson.fromJson("1700000000", Instant.class));
    assertEquals(Instant.ofEpochMilli(1_700_000_000_000L), gson.fromJson("1700000000000", Instant.class));
    assertNull(gson.fromJson("null", Instant.class));
    assertEquals("null", gson.toJson(null, Instant.class));
  }

  @Test
  void offsetDateTimeAdapterNormalisesWritesToUtc() {
    OffsetDateTime value = OffsetDateTime.parse("2026-09-28T20:15:30+10:00");

    assertEquals("\\\"2026-09-28T10:15:30Z\\\"", gson.toJson(value, OffsetDateTime.class));
    assertNull(gson.fromJson("null", OffsetDateTime.class));
    assertEquals("null", gson.toJson(null, OffsetDateTime.class));
  }

  @Test
  void offsetDateTimeAdapterAcceptsSupportedInputShapes() {
    OffsetDateTime expected = OffsetDateTime.of(2026, 9, 28, 10, 15, 30, 0, ZoneOffset.UTC);

    assertEquals(expected, gson.fromJson("\\\"2026-09-28T10:15:30Z\\\"", OffsetDateTime.class));
    assertEquals(expected, gson.fromJson("\\\"2026-09-28T10:15:30\\\"", OffsetDateTime.class));
    assertEquals(expected, gson.fromJson("\\\"2026-09-28 10:15:30\\\"", OffsetDateTime.class));
    assertEquals(
        Instant.ofEpochSecond(1_700_000_000L).atOffset(ZoneOffset.UTC),
        gson.fromJson("1700000000", OffsetDateTime.class)
    );
    assertEquals(
        Instant.ofEpochMilli(1_700_000_000_000L).atOffset(ZoneOffset.UTC),
        gson.fromJson("\\\"1700000000000\\\"", OffsetDateTime.class)
    );
    assertNull(gson.fromJson("\\\"   \\\"", OffsetDateTime.class));
  }

  @Test
  void offsetDateTimeAdapterRejectsUnsupportedText() {
    assertThrows(JsonParseException.class,
        () -> gson.fromJson("\\\"not-a-date\\\"", OffsetDateTime.class));
  }
}
