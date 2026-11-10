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
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigInteger;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TestProtobufUint64Precision {

  private static final List<String> BOUNDARIES = List.of(
      "0", "1", "9007199254740993", "9223372036854775807",
      "9223372036854775808", "18446744073709551615");

  private static FieldDescriptorProto field(String name, int number, FieldDescriptorProto.Type type) {
    return FieldDescriptorProto.newBuilder().setName(name).setNumber(number)
        .setType(type).setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL).build();
  }

  private MessageFormatter formatter() throws IOException {
    DescriptorProtos.DescriptorProto child = DescriptorProtos.DescriptorProto.newBuilder().setName("Child")
        .addField(field("id", 1, FieldDescriptorProto.Type.TYPE_UINT64)).build();
    DescriptorProtos.DescriptorProto root = DescriptorProtos.DescriptorProto.newBuilder().setName("Reading")
        .addField(field("id", 1, FieldDescriptorProto.Type.TYPE_UINT64))
        .addField(field("fixedId", 2, FieldDescriptorProto.Type.TYPE_FIXED64))
        .addField(field("signed", 3, FieldDescriptorProto.Type.TYPE_INT64))
        .addField(field("temperature", 4, FieldDescriptorProto.Type.TYPE_DOUBLE))
        .addField(FieldDescriptorProto.newBuilder().setName("ids").setNumber(5)
            .setType(FieldDescriptorProto.Type.TYPE_UINT64).setLabel(FieldDescriptorProto.Label.LABEL_REPEATED).build())
        .addField(FieldDescriptorProto.newBuilder().setName("child").setNumber(6)
            .setType(FieldDescriptorProto.Type.TYPE_MESSAGE).setTypeName(".maps.test.Child")
            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL).build())
        .build();
    DescriptorProtos.FileDescriptorProto proto = DescriptorProtos.FileDescriptorProto.newBuilder()
        .setName("uint64_precision.proto").setPackage("maps.test").setSyntax("proto3")
        .addMessageType(child).addMessageType(root).build();
    byte[] descriptor = DescriptorProtos.FileDescriptorSet.newBuilder().addFile(proto).build().toByteArray();

    ProtoBufSchemaConfig config = new ProtoBufSchemaConfig();
    ProtoBufSchemaConfig.ProtobufConfig properties = new ProtoBufSchemaConfig.ProtobufConfig();
    properties.setMessageName("maps.test.Reading");
    properties.setDescriptorValue(descriptor);
    config.setProtobufConfig(properties);
    return MessageFormatterFactory.getInstance().getFormatter(config);
  }

  @Test
  void descriptorDefinedUnsignedFieldsRemainExact() throws IOException {
    MessageFormatter formatter = formatter();
    for (String value : BOUNDARIES) {
      JsonObject sample = JsonParser.parseString("""
          {
            "id":%s,"fixedId":18446744073709551615,
            "signed":-12,"temperature":1.5,
            "ids":[0,9007199254740993,9223372036854775808],
            "child":{"id":18446744073709551615}
          }
          """.formatted(value)).getAsJsonObject();
      byte[] payload = formatter.parseFromJson(sample);
      JsonObject expected = sample.deepCopy();
      if ("0".equals(value)) {
        expected.remove("id"); // proto3 omits singular scalar fields at their default value.
      }
      assertEquals(expected, formatter.parseToJson(payload, ParseMode.STRICT), value);

      ParsedObject parsed = formatter.parse(payload, ParseMode.STRICT);
      if ("0".equals(value)) {
        assertNull(parsed.get("id"), "proto3 default value is not present");
      } else {
        assertEquals(new BigInteger(value), parsed.get("id"), value);
      }
      assertEquals(new BigInteger("18446744073709551615"), parsed.get("fixedId"));
      assertEquals(new BigInteger("18446744073709551615"), parsed.get("child.id"));
      assertEquals(new BigInteger("9007199254740993"), parsed.get("ids[1]"));
      assertEquals(new BigInteger("9223372036854775808"), parsed.get("ids[2]"));
      assertFalse(parsed.get("signed") instanceof BigInteger);
      assertFalse(parsed.get("temperature") instanceof BigInteger);
    }
  }

  @Test
  void rejectsUnsignedOverflowAndNonIntegerValues() throws IOException {
    MessageFormatter formatter = formatter();
    for (String invalid : List.of("-1", "18446744073709551616", "1.5", "\"12\"")) {
      JsonObject sample = JsonParser.parseString("{\"id\":" + invalid + "}").getAsJsonObject();
      assertThrows(IOException.class, () -> formatter.parseFromJson(sample), invalid);
    }
  }

  @Test
  void signed64IsNotReinterpretedAsUnsigned() throws IOException {
    MessageFormatter formatter = formatter();
    JsonObject sample = JsonParser.parseString("""
        {"signed":-9223372036854775808,"id":1}
        """).getAsJsonObject();
    byte[] payload = formatter.parseFromJson(sample);
    ParsedObject parsed = formatter.parse(payload, ParseMode.STRICT);
    assertInstanceOf(Long.class, parsed.get("signed"));
    assertEquals(Long.MIN_VALUE, parsed.get("signed"));
    assertEquals(BigInteger.ONE, parsed.get("id"));
  }
}
