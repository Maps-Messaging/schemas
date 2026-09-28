/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.config.impl.cbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonPrimitive;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FieldSpecificationTest {

  @Test
  void fromParsesSupportedScalarValues() {
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("name", "speed");
    input.put("type", "uint");
    input.put("size", "16.9");
    input.put("optional", "1");
    input.put("fixed", "0");
    input.put("description", new JsonPrimitive("vehicle speed"));
    input.put("decalc", "x / 10");
    input.put("encalc", "x * 10");
    input.put("min", "0x10");
    input.put("max", 500L);

    FieldSpecification field = FieldSpecification.from(input);

    assertEquals("speed", field.getName());
    assertEquals("uint", field.getType());
    assertEquals(16, field.getSize());
    assertTrue(field.getOptional());
    assertFalse(field.getFixed());
    assertEquals("vehicle speed", field.getDescription());
    assertEquals("x / 10", field.getDecalc());
    assertEquals("x * 10", field.getEncalc());
    assertEquals(16L, field.getMin());
    assertEquals(500L, field.getMax());
  }

  @Test
  void fromParsesEnumAndNestedFields() {
    Map<Object, Object> enumValues = new LinkedHashMap<>();
    enumValues.put("0x1", "ONE");
    enumValues.put(2, "TWO");
    enumValues.put("bad", "ignored");
    enumValues.put(3, " ");

    Map<String, Object> child = Map.of("name", "child", "type", "string");
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("name", "root");
    input.put("type", "struct");
    input.put("enum", enumValues);
    input.put("fields", List.of(child, "ignored"));

    FieldSpecification field = FieldSpecification.from(input);

    assertEquals(Map.of(1, "ONE", 2, "TWO"), field.getEnumTable());
    assertEquals(1, field.getFields().size());
    assertEquals("child", field.getFields().getFirst().getName());
  }

  @Test
  void fromKeepsDefaultsWhenOptionalValuesAreInvalidOrMissing() {
    Map<String, Object> input = new LinkedHashMap<>();
    input.put("name", "value");
    input.put("type", "int");
    input.put("size", "bad");
    input.put("min", "bad");
    input.put("max", null);

    FieldSpecification field = FieldSpecification.from(input);

    assertNull(field.getSize());
    assertFalse(field.getOptional());
    assertTrue(field.getFixed());
    assertNull(field.getMin());
    assertNull(field.getMax());
    assertNull(field.getEnumTable());
    assertNull(field.getFields());
  }

  @Test
  void requiredNameAndTypeAreValidated() {
    assertThrows(
        IllegalArgumentException.class,
        () -> FieldSpecification.from(Map.of("type", "int"))
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> FieldSpecification.from(Map.of("name", "value"))
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> FieldSpecification.from(Map.of("name", " ", "type", "int"))
    );
  }

  @Test
  void toJsonWritesOnlyNonDefaultOptionalValues() {
    FieldSpecification field = FieldSpecification.builder()
        .name("status")
        .type("enum")
        .size(8)
        .optional(true)
        .fixed(false)
        .description("status field")
        .decalc("decode")
        .encalc("encode")
        .min(1L)
        .max(3L)
        .enumTable(Map.of(2, "TWO", 1, "ONE"))
        .fields(List.of(FieldSpecification.builder().name("nested").type("uint").build()))
        .build();

    var json = field.toJson();

    assertEquals("status", json.get("name").getAsString());
    assertEquals("enum", json.get("type").getAsString());
    assertEquals(8, json.get("size").getAsInt());
    assertTrue(json.get("optional").getAsBoolean());
    assertFalse(json.get("fixed").getAsBoolean());
    assertEquals("status field", json.get("description").getAsString());
    assertEquals("decode", json.get("decalc").getAsString());
    assertEquals("encode", json.get("encalc").getAsString());
    assertEquals(1L, json.get("min").getAsLong());
    assertEquals(3L, json.get("max").getAsLong());
    assertEquals("ONE", json.getAsJsonObject("enum").get("1").getAsString());
    assertEquals("TWO", json.getAsJsonObject("enum").get("2").getAsString());
    assertEquals("nested", json.getAsJsonArray("fields").get(0).getAsJsonObject().get("name").getAsString());
  }

  @Test
  void toJsonOmitsDefaultOptionalFields() {
    FieldSpecification field = FieldSpecification.builder()
        .name("value")
        .type("int")
        .build();

    var json = field.toJson();

    assertFalse(json.has("size"));
    assertFalse(json.has("optional"));
    assertFalse(json.has("fixed"));
    assertFalse(json.has("enum"));
    assertFalse(json.has("fields"));
    assertFalse(json.has("description"));
    assertFalse(json.has("decalc"));
    assertFalse(json.has("encalc"));
    assertFalse(json.has("min"));
    assertFalse(json.has("max"));
  }
}
