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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.impl.CdrSchemaConfig;
import io.mapsmessaging.schemas.formatters.impl.cdr.CdrIdlParser;
import io.mapsmessaging.schemas.formatters.impl.cdr.CdrJsonSchemaParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TestCdrSchemaParsing {

  private MessageFormatter formatter(JsonObject definition) throws IOException {
    CdrSchemaConfig config = new CdrSchemaConfig();
    config.setSchema(definition);
    return MessageFormatterFactory.getInstance().getFormatter(config);
  }

  private JsonObject json(String value) {
    return JsonParser.parseString(value).getAsJsonObject();
  }

  private MessageFormatter idl(String source, String typeName) throws IOException {
    JsonObject config = new JsonObject();
    config.addProperty("idl", source);
    if (typeName != null) {
      config.addProperty("typeName", typeName);
    }
    return formatter(config);
  }

  @Test
  void idlPrimitiveRoundTripAndBinaryCompatibility() throws IOException {
    MessageFormatter formatter = idl("struct Reading { unsigned long id; double temperature; string status; };", null);
    JsonObject data = json("{\"id\":4294967295,\"temperature\":25.5,\"status\":\"OK\"}");
    byte[] wire = formatter.parseFromJson(data);
    assertArrayEquals(new byte[]{0, 1, 0, 0, -1, -1, -1, -1}, Arrays.copyOf(wire, 8));
    assertEquals(data, formatter.parseToJson(wire, ParseMode.STRICT));
    assertEquals(3, formatter.getFormat().size());
  }

  @Test
  void moduleTypedefNestedStructAndSequence() throws IOException {
    String idl = """
        // Declarations can appear in a module
        module telemetry {
          typedef unsigned long SensorId;
          struct Coordinate { float latitude; float longitude; };
          struct Reading { SensorId id; Coordinate location; sequence<short> samples; };
        };
        """;
    MessageFormatter formatter = idl(idl, "telemetry::Reading");
    JsonObject data = json("""
        {"id":314159,"location":{"latitude":1.25,"longitude":-2.5},"samples":[-15,0,99]}
        """);
    assertEquals(data, formatter.parseToJson(formatter.parseFromJson(data), ParseMode.STRICT));
    assertEquals(3, formatter.getFormat().size());
  }

  @Test
  void idlArraysNestedSequencesAndComments() throws IOException {
    String idl = """
        /* Nested CDR collections */
        struct Samples {
          octet bytes[3];
          long matrix[2][2];
          sequence<sequence<unsigned short>> rows;
        };
        """;
    MessageFormatter formatter = idl(idl, null);
    JsonObject data = json("""
        {"bytes":[1,2,255],"matrix":[[1,2],[3,4]],"rows":[[1,2],[],[65535]]}
        """);
    assertEquals(data, formatter.parseToJson(formatter.parseFromJson(data), ParseMode.STRICT));
  }

  @Test
  void idlTypedefArrays() throws IOException {
    MessageFormatter formatter = idl("""
        typedef unsigned short Pair[2];
        struct Sample { Pair values; };
        """, null);
    JsonObject data = json("{\"values\":[0,65535]}");
    assertEquals(data, formatter.parseToJson(formatter.parseFromJson(data), ParseMode.STRICT));
  }

  @Test
  void explicitEndianSelectionForIdl() throws IOException {
    JsonObject config = json("""
        {"littleEndian":false,"idl":"struct Sample { unsigned long value; };","typeName":"Sample"}
        """);
    MessageFormatter formatter = formatter(config);
    assertArrayEquals(new byte[]{0, 0, 0, 0, 0x12, 0x34, 0x56, 0x78},
        formatter.parseFromJson(json("{\"value\":305419896}")));
  }

  @Test
  void jsonSchemaRoundTripWithExplicitFieldOrdering() throws IOException {
    JsonObject schema = json("""
        {
          "type":"object",
          "x-cdr-order":["name","id","position","samples"],
          "properties":{
            "samples":{"type":"array","items":{"type":"integer","format":"int16"}},
            "id":{"type":"integer","format":"uint32"},
            "name":{"type":"string"},
            "position":{
              "type":"object","x-cdr-order":["x","y"],
              "properties":{
                "y":{"type":"number","format":"float64"},
                "x":{"type":"number","format":"float64"}
              }
            }
          }
        }
        """);
    MessageFormatter formatter = formatter(schema);
    JsonObject data = json("""
        {"name":"reading","id":17,"position":{"x":1.5,"y":2.25},"samples":[-1,2,3]}
        """);
    byte[] wire = formatter.parseFromJson(data);
    assertEquals(data, formatter.parseToJson(wire, ParseMode.STRICT));
    assertEquals("name", formatter.getFormat().keySet().iterator().next());
  }

  @Test
  void fixedJsonSchemaArray() throws IOException {
    MessageFormatter formatter = formatter(json("""
        {"type":"object","x-cdr-order":["values"],"properties":{
          "values":{"type":"array","minItems":2,"maxItems":2,"items":{"type":"integer","format":"uint16"}}
        }}
        """));
    JsonObject valid = json("{\"values\":[0,65535]}");
    assertEquals(valid, formatter.parseToJson(formatter.parseFromJson(valid), ParseMode.STRICT));
    assertThrows(IOException.class, () -> formatter.parseFromJson(json("{\"values\":[0]}")));
  }

  @Test
  void canonicalSchemaStillSupported() throws IOException {
    MessageFormatter formatter = formatter(json("""
        {"fields":[{"name":"value","type":"uint64"}]}
        """));
    JsonObject data = json("{\"value\":18446744073709551615}");
    assertEquals(data, formatter.parseToJson(formatter.parseFromJson(data), ParseMode.STRICT));
  }

  @Test
  void rejectsInvalidOrUnsupportedIdl() {
    String[] bad = {
        "",
        "struct Broken { long number; ",
        "enum State { OK, BAD };",
        "struct Broken { union X {}; };",
        "struct Broken { Unknown value; };",
        "struct A { B b; }; struct B { A a; };",
        "struct Broken { long values[0]; };",
        "struct Broken { long values[9999999999999]; };",
        "struct Broken { string<8> text; };",
        "struct Broken { sequence<short, 4> items; };",
        "struct Broken { long value @; };",
        "struct Broken { long value; }; struct Broken { long next; };",
        "struct A { long value; }; struct B { long value; };"
    };
    for (String input : bad) {
      assertThrows(IOException.class, () -> CdrIdlParser.parse(input, null), input);
    }
  }

  @Test
  void rejectsBadJsonSchemasAndAmbiguousLayouts() {
    String[] bad = {
        "{}",
        "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\",\"format\":\"uint32\"}}}",
        "{\"type\":\"object\",\"x-cdr-order\":[\"id\",\"id\"],\"properties\":{\"id\":{\"type\":\"integer\",\"format\":\"uint32\"}}}",
        "{\"type\":\"object\",\"x-cdr-order\":[\"missing\"],\"properties\":{\"id\":{\"type\":\"integer\",\"format\":\"uint32\"}}}",
        "{\"type\":\"object\",\"x-cdr-order\":[\"number\"],\"properties\":{\"number\":{\"type\":\"number\"}}}",
        "{\"type\":\"object\",\"x-cdr-order\":[\"number\"],\"properties\":{\"number\":{\"type\":\"number\",\"format\":\"int32\"}}}",
        "{\"type\":\"object\",\"x-cdr-order\":[\"array\"],\"properties\":{\"array\":{\"type\":\"array\"," +
        "\"minItems\":1,\"maxItems\":2,\"items\":{\"type\":\"integer\",\"format\":\"uint16\"}}}}",
        "{\"type\":\"object\",\"x-cdr-order\":[\"array\"],\"properties\":{\"array\":{\"type\":\"array\",\"items\":[{\"type\":\"integer\",\"format\":\"uint16\"}]}}}"
    };
    for (String input : bad) {
      assertThrows(IOException.class, () -> CdrJsonSchemaParser.parse(json(input)), input);
    }
  }

  @Test
  void rejectsMissingAndConflictingSchemaSources() {
    assertThrows(IOException.class, () -> formatter(new JsonObject()));
    assertThrows(IOException.class, () -> formatter(json("""
        {"idl":"struct A { long a; };","fields":[{"name":"a","type":"int32"}]}
        """)));
    assertThrows(IOException.class, () -> formatter(json("""
        {"idl":"struct A { long a; };","typeName":"Missing"}
        """)));
  }

  @Test
  void malformedCdrPayloadsFailStrictAndRetainIgnoreFallback() throws IOException {
    MessageFormatter formatter = idl("struct Sample { string label; unsigned long id; };", null);
    byte[] valid = formatter.parseFromJson(json("{\"label\":\"Hello\",\"id\":10}"));
    for (int count = 0; count < valid.length; count++) {
      byte[] truncated = Arrays.copyOf(valid, count);
      assertThrows(ParseException.class, () -> formatter.parse(truncated, ParseMode.STRICT), "length " + count);
      assertArrayEquals(truncated, (byte[]) formatter.parse(truncated, ParseMode.IGNORE).getReferenced(), "length " + count);
    }
    byte[] invalid = valid.clone();
    invalid[1] = 3;
    assertThrows(ParseException.class, () -> formatter.parseToJson(invalid, ParseMode.STRICT));
  }
  @Test
  void configHelpersSelectIdlAndJsonSchema() throws IOException {
    CdrSchemaConfig config = new CdrSchemaConfig();
    config.setIdl("struct Sample { unsigned short value; };", "Sample");
    MessageFormatter idlFormatter = MessageFormatterFactory.getInstance().getFormatter(config);
    JsonObject data = json("{\"value\":42}");
    assertEquals(data, idlFormatter.parseToJson(idlFormatter.parseFromJson(data), ParseMode.STRICT));

    config.setJsonSchema(json("""
        {"type":"object","x-cdr-order":["value"],"properties":{"value":{"type":"integer","format":"uint16"}}}
        """));
    MessageFormatter jsonFormatter = MessageFormatterFactory.getInstance().getFormatter(config);
    assertEquals(data, jsonFormatter.parseToJson(jsonFormatter.parseFromJson(data), ParseMode.STRICT));
  }

  @Test
  void rejectsDuplicateIdlAndCanonicalFieldNames() {
    assertThrows(IOException.class, () -> idl("struct Sample { long value; short value; };", null));
    assertThrows(IOException.class, () -> formatter(json("""
        {"fields":[{"name":"value","type":"int32"},{"name":"value","type":"int16"}]}
        """)));
  }

  @Test
  void unresolvedTypedefAndCircularAliasFailFast() {
    assertThrows(IOException.class, () -> idl("typedef Missing Alias; struct Sample { Alias value; };", null));
    assertThrows(IOException.class, () -> idl("typedef B A; typedef A B; struct Sample { A value; };", null));
  }

  @Test
  void typeDefinitionsCannotSilentlyChangeAfterCompilation() throws IOException {
    JsonObject input = json("""
        {"idl":"struct Sample { unsigned short number; };","typeName":"Sample"}
        """);
    MessageFormatter formatter = formatter(input);
    JsonObject data = json("{\"number\":65535}");
    byte[] wire = formatter.parseFromJson(data);
    input.addProperty("idl", "struct Sample { string number; };");
    assertArrayEquals(wire, formatter.parseFromJson(data));
    assertEquals(data, formatter.parseToJson(wire, ParseMode.STRICT));
  }


  @Test
  void idlCompilerProducesTheCanonicalFieldLayout() throws IOException {
    JsonObject compiled = CdrIdlParser.parse("""
        struct Sample { unsigned long identifier; float temperature; char code[2]; };
        """, "Sample");
    assertEquals("Sample", compiled.get("name").getAsString());
    assertEquals(json("""
        {"fields":[
          {"type":"uint32","name":"identifier"},
          {"type":"float32","name":"temperature"},
          {"type":"array","length":2,"element":{"type":"char"},"name":"code"}
        ]}
        """).getAsJsonArray("fields"), compiled.getAsJsonArray("fields"));
  }

  @Test
  void jsonSchemaProducesTheSameLayoutAsIdl() throws IOException {
    JsonObject fromIdl = CdrIdlParser.parse("struct Sample { unsigned long id; double value; };", "Sample");
    JsonObject fromJson = CdrJsonSchemaParser.parse(json("""
        {"type":"object","x-cdr-order":["id","value"],"properties":{
          "value":{"type":"number","format":"float64"},
          "id":{"type":"integer","format":"uint32"}
        }}
        """));
    assertEquals(fromIdl.getAsJsonArray("fields"), fromJson.getAsJsonArray("fields"));
  }

  @Test
  void repeatedSamplesUseTheSameCompiledDefinition() throws IOException {
    MessageFormatter formatter = idl("struct Sample { long position; double speed; string name; };", null);
    for (int i = 0; i < 100; i++) {
      JsonObject sample = new JsonObject();
      sample.addProperty("position", i % 2 == 0 ? i : -i);
      sample.addProperty("speed", i / 3.0);
      sample.addProperty("name", i % 3 == 0 ? "🌊" : "");
      byte[] wire = formatter.parseFromJson(sample);
      assertEquals(sample, formatter.parseToJson(wire, ParseMode.STRICT), "sample " + i);
    }
  }

  @Test
  void tokenBudgetStopsPathologicalIdl() {
    String source = "struct Sample { long value; }; " + " ".repeat(10) + "typedef long Extra; ".repeat(20_000);
    assertThrows(IOException.class, () -> CdrIdlParser.parse(source, "Sample"));
  }


}
