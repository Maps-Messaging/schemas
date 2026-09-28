/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.JsonObject;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SchemaConfigCopyTest {

  @Test
  void copyConstructorCopiesEveryEligibleField() {
    SchemaConfig source = new SchemaConfig();
    source.setVersionId("v1");
    source.setEpoch(7L);
    source.setName("name");
    source.setDescription("description");
    source.setDocumentation("https://example.invalid/docs");
    source.setLabels(new LinkedHashMap<>(Map.of("key", "value")));
    source.setAncestor("ancestor");
    source.setFormat("json");
    source.setSchemaUrl("https://example.invalid/schema");
    JsonObject schema = new JsonObject();
    schema.addProperty("type", "object");
    source.setSchema(schema);
    source.setSchemaBase64("base64");
    source.setParentUuid("parent");
    OffsetDateTime timestamp = OffsetDateTime.of(2026, 9, 28, 10, 0, 0, 0, ZoneOffset.UTC);
    source.setCreatedAt(timestamp);
    source.setModifiedAt(timestamp.plusMinutes(1));
    source.setNotBefore(timestamp.plusMinutes(2));
    source.setExpiresAfter(timestamp.plusMinutes(3));

    SchemaConfig copy = new SchemaConfig(source);

    assertEquals(source.getVersionId(), copy.getVersionId());
    assertEquals(source.getEpoch(), copy.getEpoch());
    assertEquals(source.getName(), copy.getName());
    assertEquals(source.getDescription(), copy.getDescription());
    assertEquals(source.getDocumentation(), copy.getDocumentation());
    assertEquals(source.getLabels(), copy.getLabels());
    assertNotSame(source.getLabels(), copy.getLabels());
    assertEquals(source.getAncestor(), copy.getAncestor());
    assertEquals(source.getFormat(), copy.getFormat());
    assertEquals(source.getSchemaUrl(), copy.getSchemaUrl());
    assertEquals(source.getSchema(), copy.getSchema());
    assertEquals(source.getSchemaBase64(), copy.getSchemaBase64());
    assertEquals(source.getParentUuid(), copy.getParentUuid());
    assertEquals(source.getCreatedAt(), copy.getCreatedAt());
    assertEquals(source.getModifiedAt(), copy.getModifiedAt());
    assertEquals(source.getNotBefore(), copy.getNotBefore());
    assertEquals(source.getExpiresAfter(), copy.getExpiresAfter());
  }

  @Test
  void copyConstructorPreservesNullLabelsExplicitly() {
    SchemaConfig source = new SchemaConfig();

    SchemaConfig copy = new SchemaConfig(source);

    assertNull(copy.getLabels());
  }
}
