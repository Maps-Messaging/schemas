/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.config;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SchemaConfigCoverageTest {

  @Test
  void formatCanOnlyBeInitialisedOnceAndIsNormalised() {
    SchemaConfig config = new SchemaConfig();

    config.setFormat("JSON");
    config.setFormat("avro");

    assertEquals("json", config.getFormat());
  }

  @Test
  void parentStateAndDefaultBundleBehaviourAreConsistent() {
    SchemaConfig config = new SchemaConfig();

    assertTrue(config.isPrimary());
    assertFalse(config.isChild());
    assertFalse(config.isBundle());
    assertEquals(List.of(), config.getBundledSchemas());

    config.setParentUuid("parent");
    assertFalse(config.isPrimary());
    assertTrue(config.isChild());

    config.setParentUuid("");
    assertTrue(config.isPrimary());
    assertFalse(config.isChild());
  }

  @Test
  void labelsExposeConveniencePropertiesAndValidateRegex() {
    SchemaConfig config = new SchemaConfig();

    assertNull(config.getMatchExpression());
    assertNull(config.getUniqueId());
    assertNull(config.getInterfaceDescription());
    assertNull(config.getResourceType());
    assertNull(config.getComments());
    assertNull(config.getSource());

    config.setMatchExpression("a.*b");
    config.setUniqueId("id");
    config.setInterfaceDescription("iface");
    config.setResourceType("sensor");
    config.setComments("comments");
    config.setSource("source");

    assertEquals("a.*b", config.getMatchExpression());
    assertEquals("id", config.getUniqueId());
    assertEquals("iface", config.getInterfaceDescription());
    assertEquals("sensor", config.getResourceType());
    assertEquals("comments", config.getComments());
    assertEquals("source", config.getSource());

    config.setMatchExpression("[");
    assertEquals("a.*b", config.getMatchExpression());
  }

  @Test
  void uniqueIdUuidVersionAndTitleAliasesWork() {
    SchemaConfig config = new SchemaConfig();
    UUID id = UUID.randomUUID();

    config.setUniqueId(id);
    config.setVersion(42);
    config.setTitle("title");

    assertEquals(id.toString(), config.getUniqueId());
    assertEquals("42", config.getVersion());
    assertEquals("title", config.getTitle());
  }

  @Test
  void packRequiresSchemaOrBase64() {
    SchemaConfig config = new SchemaConfig();

    assertThrows(IOException.class, config::pack);
    assertThrows(IOException.class, config::packAsBytes);
  }

  @Test
  void packAndPackAsBytesSerialiseSchema() throws Exception {
    SchemaConfig config = new SchemaConfig();
    config.setFormat("json");
    JsonObject schema = new JsonObject();
    schema.addProperty("type", "object");
    config.setSchema(schema);

    String packed = config.pack();

    assertTrue(packed.contains("\"type\""));
    assertArrayEquals(packed.getBytes(StandardCharsets.UTF_8), config.packAsBytes());
    assertEquals("object", config.packData().getAsJsonObject("schema").get("type").getAsString());
  }

  @Test
  void toMapConvertsNestedJsonAndNumericTypes() {
    SchemaConfig config = new SchemaConfig();
    config.setFormat("json");

    JsonObject schema = new JsonObject();
    schema.addProperty("boolean", true);
    schema.addProperty("int", 7);
    schema.addProperty("long", 3_000_000_000L);
    schema.addProperty("decimal", 1.25);
    schema.addProperty("huge", "123456789012345678901234567890");
    JsonArray array = new JsonArray();
    array.add(1);
    array.add("two");
    schema.add("array", array);
    JsonObject nested = new JsonObject();
    nested.addProperty("value", "x");
    schema.add("nested", nested);
    schema.add("nothing", null);
    config.setSchema(schema);

    Map<String, Object> map = config.toMap();
    @SuppressWarnings("unchecked")
    Map<String, Object> mappedSchema = (Map<String, Object>) map.get("schema");

    assertEquals(true, mappedSchema.get("boolean"));
    assertEquals(7, mappedSchema.get("int"));
    assertEquals(3_000_000_000L, mappedSchema.get("long"));
    assertEquals(1.25d, (Double) mappedSchema.get("decimal"), 0.0d);
    assertEquals(List.of(1, "two"), mappedSchema.get("array"));
    @SuppressWarnings("unchecked")
    Map<String, Object> mappedNested = (Map<String, Object>) mappedSchema.get("nested");
    assertEquals("x", mappedNested.get("value"));
    assertNull(mappedSchema.get("nothing"));
  }

  @Test
  void baseClassFactoryMethodsRemainEmptyByDesign() {
    SchemaConfig config = new SchemaConfig();

    assertNull(config.getInstance(config));
    assertNull(config.getMimeType());
  }
}
