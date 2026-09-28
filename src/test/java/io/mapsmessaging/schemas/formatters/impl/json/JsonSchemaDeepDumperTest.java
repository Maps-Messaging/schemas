/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.formatters.impl.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

class JsonSchemaDeepDumperTest {

  private static final ObjectMapper MAPPER = new ObjectMapper();

  @Test
  void nullRootProducesObjectSchema() {
    ObjectNode result = JsonSchemaDeepDumper.dumpSchema(null, null, null);

    assertEquals("object", result.get("type").asText());
  }

  @Test
  void expandsLocalDefinitionAndPreservesRootMetadata() throws Exception {
    JsonNode root = MAPPER.readTree("""
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "$id": "urn:test",
          "$defs": {
            "Address": {
              "type": "object",
              "properties": {
                "city": { "type": "string" }
              }
            }
          },
          "type": "object",
          "properties": {
            "address": { "$ref": "#/$defs/Address" }
          }
        }
        """);

    ObjectNode result = JsonSchemaDeepDumper.dumpSchema(root, null, null);

    assertEquals("https://json-schema.org/draft/2020-12/schema", result.get("$schema").asText());
    assertEquals("urn:test", result.get("$id").asText());
    assertFalse(result.has("$defs"));
    assertFalse(result.has("definitions"));
    assertEquals(
        "object",
        result.path("properties").path("address").path("type").asText()
    );
    assertEquals(
        "string",
        result.path("properties").path("address").path("properties").path("city").path("type").asText()
    );
  }

  @Test
  void unresolvedReferenceIsPreserved() throws Exception {
    JsonNode root = MAPPER.readTree("""
        {
          "type": "object",
          "properties": {
            "value": { "$ref": "external.json" }
          }
        }
        """);

    ObjectNode result = JsonSchemaDeepDumper.dumpSchema(root, null, null);

    assertEquals(
        "external.json",
        result.path("properties").path("value").path("$ref").asText()
    );
  }

  @Test
  void recursiveReferenceStopsExpansion() throws Exception {
    JsonNode root = MAPPER.readTree("""
        {
          "$defs": {
            "Node": {
              "type": "object",
              "properties": {
                "child": { "$ref": "#/$defs/Node" }
              }
            }
          },
          "$ref": "#/$defs/Node"
        }
        """);

    ObjectNode result = JsonSchemaDeepDumper.dumpSchema(root, null, null);

    assertEquals(
        "#/$defs/Node",
        result.path("properties").path("child").path("$ref").asText()
    );
  }

  @Test
  void referenceSiblingFieldsOverrideExpandedDefinition() throws Exception {
    JsonNode root = MAPPER.readTree("""
        {
          "$defs": {
            "Value": { "type": "string", "description": "base" }
          },
          "properties": {
            "value": {
              "$ref": "#/$defs/Value",
              "description": "override"
            }
          }
        }
        """);

    ObjectNode result = JsonSchemaDeepDumper.dumpSchema(root, null, null);

    assertEquals("string", result.path("properties").path("value").path("type").asText());
    assertEquals("override", result.path("properties").path("value").path("description").asText());
  }

  @Test
  void expandsReferencesInsideArraysAndEscapedPointers() throws Exception {
    JsonNode root = MAPPER.readTree("""
        {
          "$defs": {
            "a/b": { "type": "integer" },
            "til~de": { "type": "boolean" }
          },
          "allOf": [
            { "$ref": "#/$defs/a~1b" },
            { "$ref": "#/$defs/til~0de" }
          ]
        }
        """);

    ObjectNode result = JsonSchemaDeepDumper.dumpSchema(root, null, null);

    assertEquals("integer", result.path("allOf").get(0).path("type").asText());
    assertEquals("boolean", result.path("allOf").get(1).path("type").asText());
  }

  @Test
  void selectedPrimitiveBecomesConst() throws Exception {
    JsonNode root = MAPPER.readTree("{\"type\":\"object\"}");
    JsonNode selected = MAPPER.readTree("42");

    ObjectNode result = JsonSchemaDeepDumper.dumpSchema(root, selected, null);

    assertEquals(42, result.get("const").asInt());
  }

  @Test
  void externalReferenceFragmentCanResolveLocalPointer() throws Exception {
    JsonNode root = MAPPER.readTree("""
        {
          "$defs": {
            "Value": { "type": "number" }
          },
          "$ref": "other.json#/$defs/Value"
        }
        """);

    ObjectNode result = JsonSchemaDeepDumper.dumpSchema(root, null, null);

    assertEquals("number", result.get("type").asText());
    assertFalse(result.has("$ref"));
  }

  @Test
  void arrayIndexPointersAndInvalidIndexesAreHandled() throws Exception {
    JsonNode root = MAPPER.readTree("""
        {
          "items": [
            { "type": "string" },
            { "type": "number" }
          ],
          "properties": {
            "ok": { "$ref": "#/items/1" },
            "bad": { "$ref": "#/items/x" },
            "range": { "$ref": "#/items/9" }
          }
        }
        """);

    ObjectNode result = JsonSchemaDeepDumper.dumpSchema(root, null, null);

    assertEquals("number", result.path("properties").path("ok").path("type").asText());
    assertTrue(result.path("properties").path("bad").has("$ref"));
    assertTrue(result.path("properties").path("range").has("$ref"));
  }
}
