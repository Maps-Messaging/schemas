/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 */
package io.mapsmessaging.schemas.formatters.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import io.mapsmessaging.schemas.formatters.ParseException;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.repository.SchemaResolver;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class JsonFormatterCoverageTest {

  @TempDir
  Path tempDirectory;

  private static final String SCHEMA = """
      {
        "$schema": "https://json-schema.org/draft/2020-12/schema",
        "$id": "urn:test",
        "type": "object",
        "required": ["name"],
        "properties": {
          "name": { "type": "string" }
        },
        "$defs": {
          "Child": {
            "type": "object",
            "properties": {
              "value": { "type": "integer" }
            }
          }
        }
      }
      """;

  @Test
  void defaultFormatterExposesEmptyMetadataAndRoundTripsJson() throws Exception {
    JsonFormatter formatter = new JsonFormatter();

    assertEquals("JSON", formatter.getName());
    assertTrue(formatter.getFormat().isEmpty());
    assertTrue(formatter.getJsonSchema().isEmpty());

    JsonObject json = JsonParser.parseString("{\"name\":\"value\"}").getAsJsonObject();
    byte[] encoded = formatter.parseFromJson(json);

    assertEquals(json, formatter.parseToJson(encoded, ParseMode.STRICT));
  }

  @Test
  void schemaConstructorValidatesStrictAndFallsBackInIgnoreMode() throws Exception {
    JsonFormatter formatter = new JsonFormatter(SCHEMA);

    var valid = formatter.parse(
        "{\"name\":\"value\"}".getBytes(StandardCharsets.UTF_8),
        ParseMode.STRICT
    );
    assertNotNull(valid);

    assertThrows(
        ParseException.class,
        () -> formatter.parse("{}".getBytes(StandardCharsets.UTF_8), ParseMode.STRICT)
    );

    var ignored = formatter.parse("{}".getBytes(StandardCharsets.UTF_8), ParseMode.IGNORE);
    assertNotNull(ignored);

    assertThrows(
        ParseException.class,
        () -> formatter.parse("not-json".getBytes(StandardCharsets.UTF_8), ParseMode.STRICT)
    );
    assertNotNull(
        formatter.parse("not-json".getBytes(StandardCharsets.UTF_8), ParseMode.IGNORE)
    );
  }

  @Test
  void getFormatAndExpandedSchemaExposeSelectedProperties() throws Exception {
    JsonFormatter root = new JsonFormatter(SCHEMA);
    assertTrue(root.getFormat().containsKey("name"));
    assertFalse(root.getJsonSchema().has("$defs"));

    JsonFormatter child = new JsonFormatter(SCHEMA, "#/$defs/Child");
    assertTrue(child.getFormat().containsKey("value"));
    JsonObject expanded = child.getJsonSchema();
    assertEquals(
        "integer",
        expanded.getAsJsonObject("properties")
            .getAsJsonObject("value")
            .get("type")
            .getAsString()
    );
  }

  @Test
  void schemaPathConstructorChecksArgumentsAndReadsFile() throws Exception {
    assertThrows(IllegalArgumentException.class, () -> new JsonFormatter((Path) null));
    assertThrows(
        java.io.IOException.class,
        () -> new JsonFormatter(tempDirectory.resolve("missing.json"))
    );

    Path directory = Files.createDirectory(tempDirectory.resolve("directory"));
    assertThrows(java.io.IOException.class, () -> new JsonFormatter(directory));

    Path schema = tempDirectory.resolve("schema.json");
    Files.writeString(schema, SCHEMA);

    JsonFormatter formatter = new JsonFormatter(schema);
    assertTrue(formatter.getFormat().containsKey("name"));
  }

  @Test
  void getInstanceHandlesPrimaryChildMissingAndInvalidSchemas() {
    JsonFormatter factory = new JsonFormatter();

    JsonSchemaConfig primary = new JsonSchemaConfig(SCHEMA);
    assertInstanceOf(JsonFormatter.class, factory.getInstance(primary, null));

    SchemaConfig empty = new SchemaConfig();
    empty.setFormat("json");
    assertNull(factory.getInstance(empty, null));

    JsonSchemaConfig child = new JsonSchemaConfig("{\"type\":\"object\"}");
    child.setParentUuid("parent");
    child.setSource("#/$defs/Child");

    SchemaResolver resolver = config -> primary;
    assertInstanceOf(JsonFormatter.class, factory.getInstance(child, resolver));

    SchemaResolver missingParent = config -> null;
    assertNull(factory.getInstance(child, missingParent));
  }

  @Test
  void emptyAndMalformedSchemaStringsAreRejected() {
    assertThrows(Exception.class, () -> new JsonFormatter(""));
    assertThrows(Exception.class, () -> new JsonFormatter("not-json"));
  }
}
