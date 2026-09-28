/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.formatters.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

class GsonFactoryTest {

  private final Gson gson = GsonFactory.createStrictJsonWithSafeFloats();

  @Test
  void serialisesFiniteAndNonFiniteDoublesSafely() {
    assertEquals("12.5", gson.toJson(12.5d, Double.class));
    assertEquals("null", gson.toJson(Double.NaN, Double.class));
    assertEquals("null", gson.toJson(Double.POSITIVE_INFINITY, double.class));
    assertEquals("null", gson.toJson((Double) null, Double.class));
  }

  @Test
  void deserialisesDoubleNumbersStringsAndSpecialValues() {
    assertEquals(12.5d, gson.fromJson("12.5", Double.class));
    assertEquals(12.5d, gson.fromJson(""12.5"", Double.class));
    assertNull(gson.fromJson(""NaN"", Double.class));
    assertNull(gson.fromJson(""Infinity"", Double.class));
    assertNull(gson.fromJson(""-Infinity"", Double.class));
    assertNull(gson.fromJson("null", Double.class));
  }

  @Test
  void serialisesFiniteAndNonFiniteFloatsSafely() {
    assertEquals("12.5", gson.toJson(12.5f, Float.class));
    assertEquals("null", gson.toJson(Float.NaN, Float.class));
    assertEquals("null", gson.toJson(Float.NEGATIVE_INFINITY, float.class));
    assertEquals("null", gson.toJson((Float) null, Float.class));
  }

  @Test
  void deserialisesFloatNumbersStringsAndSpecialValues() {
    assertEquals(12.5f, gson.fromJson("12.5", Float.class));
    assertEquals(12.5f, gson.fromJson(""12.5"", Float.class));
    assertNull(gson.fromJson(""NaN"", Float.class));
    assertNull(gson.fromJson(""Infinity"", Float.class));
    assertNull(gson.fromJson(""-Infinity"", Float.class));
    assertNull(gson.fromJson("null", Float.class));
  }

  @Test
  void htmlEscapingIsDisabled() {
    assertEquals(""<tag>"", gson.toJson("<tag>"));
  }
}
