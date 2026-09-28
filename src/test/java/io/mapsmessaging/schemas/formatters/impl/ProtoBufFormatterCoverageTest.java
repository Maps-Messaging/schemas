/*
 *
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 *
 */

package io.mapsmessaging.schemas.formatters.impl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.DescriptorProtos;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProtoBufFormatterCoverageTest {

  @Test
  void emptyFormatterHasNoFormatAndCannotEncode() throws Exception {
    ProtoBufFormatter formatter = new ProtoBufFormatter();

    assertEquals("ProtoBuf", formatter.getName());
    assertTrue(formatter.getFormat().isEmpty());
    assertArrayEquals(new byte[0], formatter.parseFromJson(new JsonObject()));
  }

  @Test
  void formatDescribesEnumsNestedRepeatedOneofMapAndRecursion() throws Exception {
    ProtoBufFormatter formatter = new ProtoBufFormatter("Root", descriptorSet());

    Map<String, Object> format = formatter.getFormat();

    assertEquals("test.Root", format.get("messageName"));
    @SuppressWarnings("unchecked")
    List<Map<String, Object>> fields = (List<Map<String, Object>>) format.get("fields");
    assertFalse(fields.isEmpty());

    Map<String, Object> status = field(fields, "status");
    assertEquals("test.Status", status.get("enumType"));
    assertEquals(List.of("UNKNOWN", "READY"), status.get("enumValues"));

    Map<String, Object> nested = field(fields, "nested");
    assertEquals("test.Nested", nested.get("messageType"));
    assertTrue(nested.containsKey("structure"));

    Map<String, Object> values = field(fields, "values");
    assertEquals(true, values.get("repeated"));

    Map<String, Object> tags = field(fields, "tags");
    assertEquals(true, tags.get("map"));
    assertEquals("string", tags.get("mapKeyType"));
    assertEquals("string", tags.get("mapValueType"));

    Map<String, Object> child = field(fields, "child");
    assertEquals("test.Root", child.get("messageType"));
    assertFalse(child.containsKey("structure"));

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> oneOfs = (List<Map<String, Object>>) format.get("oneOfs");
    assertEquals("choice", oneOfs.getFirst().get("name"));
  }

  @Test
  void jsonRoundTripExercisesScalarAndStructuredCoercion() throws Exception {
    ProtoBufFormatter formatter = new ProtoBufFormatter("Root", descriptorSet());
    JsonObject json = fullJson();

    byte[] bytes = formatter.parseFromJson(json);
    JsonObject decoded = formatter.parseToJson(bytes, io.mapsmessaging.schemas.formatters.ParseMode.STRICT);

    assertEquals("123", decoded.get("text").getAsString());
    assertEquals(5, decoded.get("count").getAsInt());
    assertEquals(
        Instant.parse("2026-09-28T10:15:30Z").toEpochMilli(),
        decoded.get("timestamp").getAsLong()
    );
    assertEquals(1.5f, decoded.get("ratio").getAsFloat(), 0.0f);
    assertEquals(2.5d, decoded.get("amount").getAsDouble(), 0.0d);
    assertTrue(decoded.get("enabled").getAsBoolean());
    assertEquals(
        Base64.getEncoder().encodeToString("abc".getBytes(StandardCharsets.UTF_8)),
        decoded.get("data").getAsString()
    );
    assertEquals("READY", decoded.get("status").getAsString());
    assertEquals("nested", decoded.getAsJsonObject("nested").get("value").getAsString());
    assertEquals(2, decoded.getAsJsonArray("values").size());
    assertEquals(2, decoded.getAsJsonArray("children").size());
    assertEquals("chosen", decoded.get("choiceName").getAsString());
  }

  @Test
  void bytesAcceptUtf8FallbackAndNumericArrays() throws Exception {
    ProtoBufFormatter formatter = new ProtoBufFormatter("Root", descriptorSet());

    JsonObject utf8 = minimumJson();
    utf8.addProperty("data", "not-valid-base64!");
    JsonObject decodedUtf8 = formatter.parseToJson(
        formatter.parseFromJson(utf8),
        io.mapsmessaging.schemas.formatters.ParseMode.STRICT
    );
    assertEquals(
        Base64.getEncoder().encodeToString("not-valid-base64!".getBytes(StandardCharsets.UTF_8)),
        decodedUtf8.get("data").getAsString()
    );

    JsonObject numbers = minimumJson();
    JsonArray bytes = new JsonArray();
    bytes.add(65);
    bytes.add(66);
    numbers.add("data", bytes);
    JsonObject decodedNumbers = formatter.parseToJson(
        formatter.parseFromJson(numbers),
        io.mapsmessaging.schemas.formatters.ParseMode.STRICT
    );
    assertEquals(
        Base64.getEncoder().encodeToString(new byte[]{65, 66}),
        decodedNumbers.get("data").getAsString()
    );
  }

  @Test
  void enumsAcceptNumbersAndNumericStringsAndRejectUnknownValues() throws Exception {
    ProtoBufFormatter formatter = new ProtoBufFormatter("Root", descriptorSet());

    JsonObject numeric = minimumJson();
    numeric.addProperty("status", 1);
    assertEquals(
        "READY",
        formatter.parseToJson(
            formatter.parseFromJson(numeric),
            io.mapsmessaging.schemas.formatters.ParseMode.STRICT
        ).get("status").getAsString()
    );

    JsonObject numericString = minimumJson();
    numericString.addProperty("status", "1");
    assertEquals(
        "READY",
        formatter.parseToJson(
            formatter.parseFromJson(numericString),
            io.mapsmessaging.schemas.formatters.ParseMode.STRICT
        ).get("status").getAsString()
    );

    JsonObject badName = minimumJson();
    badName.addProperty("status", "MISSING");
    assertThrows(IllegalArgumentException.class, () -> formatter.parseFromJson(badName));

    JsonObject badNumber = minimumJson();
    badNumber.addProperty("status", 99);
    assertThrows(IllegalArgumentException.class, () -> formatter.parseFromJson(badNumber));
  }

  @Test
  void integerAndLongCoercionHandleDurationsDatesAndFailures() throws Exception {
    ProtoBufFormatter formatter = new ProtoBufFormatter("Root", descriptorSet());

    JsonObject localDateTime = minimumJson();
    localDateTime.addProperty("count", "7");
    localDateTime.addProperty("timestamp", "2026-09-28T10:15:30");
    assertEquals(
        Instant.parse("2026-09-28T10:15:30Z").toEpochMilli(),
        formatter.parseToJson(
            formatter.parseFromJson(localDateTime),
            io.mapsmessaging.schemas.formatters.ParseMode.STRICT
        ).get("timestamp").getAsLong()
    );

    JsonObject dateOnly = minimumJson();
    dateOnly.addProperty("timestamp", "2026-09-28");
    assertEquals(
        Instant.parse("2026-09-28T00:00:00Z").toEpochMilli(),
        formatter.parseToJson(
            formatter.parseFromJson(dateOnly),
            io.mapsmessaging.schemas.formatters.ParseMode.STRICT
        ).get("timestamp").getAsLong()
    );

    JsonObject badInt = minimumJson();
    badInt.addProperty("count", "not-an-int");
    assertThrows(IllegalArgumentException.class, () -> formatter.parseFromJson(badInt));

    JsonObject fractionalDuration = minimumJson();
    fractionalDuration.addProperty("count", "PT0.5S");
    assertThrows(IllegalArgumentException.class, () -> formatter.parseFromJson(fractionalDuration));

    JsonObject badDuration = minimumJson();
    badDuration.addProperty("count", "P-BAD");
    assertThrows(IllegalArgumentException.class, () -> formatter.parseFromJson(badDuration));

    JsonObject badLong = minimumJson();
    badLong.addProperty("timestamp", "not-a-time");
    assertThrows(IllegalArgumentException.class, () -> formatter.parseFromJson(badLong));
  }

  @Test
  void messageFieldsRequireObjects() throws Exception {
    ProtoBufFormatter formatter = new ProtoBufFormatter("Root", descriptorSet());
    JsonObject json = minimumJson();
    json.addProperty("nested", "not-an-object");

    assertThrows(IllegalArgumentException.class, () -> formatter.parseFromJson(json));
  }

  private JsonObject fullJson() {
    JsonObject json = minimumJson();
    json.addProperty("text", 123);
    json.addProperty("count", "PT5S");
    json.addProperty("timestamp", "2026-09-28T10:15:30Z");
    json.addProperty("ratio", "1.5");
    json.addProperty("amount", 2.5);
    json.addProperty("enabled", "1");
    json.addProperty("data", Base64.getEncoder().encodeToString("abc".getBytes(StandardCharsets.UTF_8)));
    json.addProperty("status", "READY");

    JsonObject nested = new JsonObject();
    nested.addProperty("value", "nested");
    json.add("nested", nested);

    JsonArray values = new JsonArray();
    values.add(1);
    values.add("2");
    json.add("values", values);

    JsonArray children = new JsonArray();
    JsonObject first = new JsonObject();
    first.addProperty("value", "a");
    JsonObject second = new JsonObject();
    second.addProperty("value", "b");
    children.add(first);
    children.add(second);
    json.add("children", children);

    json.addProperty("choiceName", "chosen");
    return json;
  }

  private JsonObject minimumJson() {
    JsonObject json = new JsonObject();
    json.addProperty("text", "required");
    return json;
  }

  private Map<String, Object> field(List<Map<String, Object>> fields, String name) {
    return fields.stream()
        .filter(field -> name.equals(field.get("name")))
        .findFirst()
        .orElseThrow();
  }

  private byte[] descriptorSet() {
    DescriptorProtos.EnumDescriptorProto status = DescriptorProtos.EnumDescriptorProto.newBuilder()
        .setName("Status")
        .addValue(enumValue("UNKNOWN", 0))
        .addValue(enumValue("READY", 1))
        .build();

    DescriptorProtos.DescriptorProto nested = DescriptorProtos.DescriptorProto.newBuilder()
        .setName("Nested")
        .addField(field("value", 1, DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING))
        .build();

    DescriptorProtos.DescriptorProto tagsEntry = DescriptorProtos.DescriptorProto.newBuilder()
        .setName("TagsEntry")
        .setOptions(DescriptorProtos.MessageOptions.newBuilder().setMapEntry(true))
        .addField(field("key", 1, DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING))
        .addField(field("value", 2, DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING))
        .build();

    DescriptorProtos.DescriptorProto.Builder root = DescriptorProtos.DescriptorProto.newBuilder()
        .setName("Root")
        .addNestedType(tagsEntry)
        .addOneofDecl(DescriptorProtos.OneofDescriptorProto.newBuilder().setName("choice"))
        .addField(field("text", 1, DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING))
        .addField(field("count", 2, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32))
        .addField(field("timestamp", 3, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT64))
        .addField(field("ratio", 4, DescriptorProtos.FieldDescriptorProto.Type.TYPE_FLOAT))
        .addField(field("amount", 5, DescriptorProtos.FieldDescriptorProto.Type.TYPE_DOUBLE))
        .addField(field("enabled", 6, DescriptorProtos.FieldDescriptorProto.Type.TYPE_BOOL))
        .addField(field("data", 7, DescriptorProtos.FieldDescriptorProto.Type.TYPE_BYTES))
        .addField(typedField("status", 8, DescriptorProtos.FieldDescriptorProto.Type.TYPE_ENUM, ".test.Status"))
        .addField(typedField("nested", 9, DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE, ".test.Nested"))
        .addField(repeatedField("values", 10, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32, null))
        .addField(repeatedField("children", 11, DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE, ".test.Nested"))
        .addField(oneofField("choiceName", 12, DescriptorProtos.FieldDescriptorProto.Type.TYPE_STRING, 0))
        .addField(oneofField("choiceCode", 13, DescriptorProtos.FieldDescriptorProto.Type.TYPE_INT32, 0))
        .addField(repeatedField("tags", 14, DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE, ".test.Root.TagsEntry"))
        .addField(typedField("child", 15, DescriptorProtos.FieldDescriptorProto.Type.TYPE_MESSAGE, ".test.Root"));

    DescriptorProtos.FileDescriptorProto file = DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("coverage.proto")
        .setPackage("test")
        .setSyntax("proto3")
        .addEnumType(status)
        .addMessageType(nested)
        .addMessageType(root)
        .build();

    return DescriptorProtos.FileDescriptorSet.newBuilder().addFile(file).build().toByteArray();
  }

  private DescriptorProtos.EnumValueDescriptorProto enumValue(String name, int number) {
    return DescriptorProtos.EnumValueDescriptorProto.newBuilder()
        .setName(name)
        .setNumber(number)
        .build();
  }

  private DescriptorProtos.FieldDescriptorProto field(
      String name,
      int number,
      DescriptorProtos.FieldDescriptorProto.Type type) {
    return DescriptorProtos.FieldDescriptorProto.newBuilder()
        .setName(name)
        .setNumber(number)
        .setType(type)
        .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_OPTIONAL)
        .build();
  }

  private DescriptorProtos.FieldDescriptorProto typedField(
      String name,
      int number,
      DescriptorProtos.FieldDescriptorProto.Type type,
      String typeName) {
    return field(name, number, type).toBuilder().setTypeName(typeName).build();
  }

  private DescriptorProtos.FieldDescriptorProto repeatedField(
      String name,
      int number,
      DescriptorProtos.FieldDescriptorProto.Type type,
      String typeName) {
    DescriptorProtos.FieldDescriptorProto.Builder builder = field(name, number, type).toBuilder()
        .setLabel(DescriptorProtos.FieldDescriptorProto.Label.LABEL_REPEATED);
    if (typeName != null) {
      builder.setTypeName(typeName);
    }
    return builder.build();
  }

  private DescriptorProtos.FieldDescriptorProto oneofField(
      String name,
      int number,
      DescriptorProtos.FieldDescriptorProto.Type type,
      int oneofIndex) {
    return field(name, number, type).toBuilder().setOneofIndex(oneofIndex).build();
  }
}
