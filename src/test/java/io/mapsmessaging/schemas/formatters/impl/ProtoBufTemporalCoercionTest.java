/*
 *
 *     Copyright [ 2020 - 2026 ] [Matthew Buckton]
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package io.mapsmessaging.schemas.formatters.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorSet;
import io.mapsmessaging.schemas.formatters.ParseMode;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProtoBufTemporalCoercionTest {

  @Test
  void shouldCoerceIsoDurationsToIntegerAndLongSeconds() throws Exception {
    ProtoBufFormatter formatter = createFormatter();
    JsonObject input = new JsonObject();
    input.addProperty("int_value", "PT1S");
    input.addProperty("long_value", "PT1M30S");

    JsonObject output = roundTrip(formatter, input);

    assertEquals(1, output.get("int_value").getAsInt());
    assertEquals(90L, output.get("long_value").getAsLong());
  }

  @Test
  void shouldCoerceRepeatedDurations() throws Exception {
    ProtoBufFormatter formatter = createFormatter();
    JsonArray values = new JsonArray();
    values.add("PT1S");
    values.add("PT1M");
    values.add("PT2H");
    JsonObject input = new JsonObject();
    input.add("repeated_values", values);

    JsonArray output = roundTrip(formatter, input).getAsJsonArray("repeated_values");

    assertEquals(1, output.get(0).getAsInt());
    assertEquals(60, output.get(1).getAsInt());
    assertEquals(7200, output.get(2).getAsInt());
  }

  @Test
  void shouldCoerceNestedDuration() throws Exception {
    ProtoBufFormatter formatter = createFormatter();
    JsonObject nested = new JsonObject();
    nested.addProperty("value", "PT2H");
    JsonObject input = new JsonObject();
    input.add("nested", nested);

    JsonObject output = roundTrip(formatter, input);

    assertEquals(7200L, output.getAsJsonObject("nested").get("value").getAsLong());
  }

  @Test
  void shouldCoerceIsoInstantAndOffsetDateTimeToEpochMillis() throws Exception {
    ProtoBufFormatter formatter = createFormatter();
    long expected = Instant.parse("2026-09-21T18:30:00Z").toEpochMilli();

    JsonObject utcInput = new JsonObject();
    utcInput.addProperty("long_value", "2026-09-21T18:30:00Z");
    JsonObject offsetInput = new JsonObject();
    offsetInput.addProperty("long_value", "2026-09-21T19:30:00+01:00");

    assertEquals(expected, roundTrip(formatter, utcInput).get("long_value").getAsLong());
    assertEquals(expected, roundTrip(formatter, offsetInput).get("long_value").getAsLong());
  }

  @Test
  void shouldTreatLocalDateTimeAndDateAsUtc() throws Exception {
    ProtoBufFormatter formatter = createFormatter();
    long dateTimeExpected = LocalDateTime.parse("2026-09-21T18:30:00")
        .toInstant(ZoneOffset.UTC)
        .toEpochMilli();
    long dateExpected = LocalDate.parse("2026-09-21")
        .atStartOfDay(ZoneOffset.UTC)
        .toInstant()
        .toEpochMilli();

    JsonObject dateTimeInput = new JsonObject();
    dateTimeInput.addProperty("long_value", "2026-09-21T18:30:00");
    JsonObject dateInput = new JsonObject();
    dateInput.addProperty("long_value", "2026-09-21");

    assertEquals(dateTimeExpected, roundTrip(formatter, dateTimeInput).get("long_value").getAsLong());
    assertEquals(dateExpected, roundTrip(formatter, dateInput).get("long_value").getAsLong());
  }

  @Test
  void shouldPreserveNumericValuesAndNumericStrings() throws Exception {
    ProtoBufFormatter formatter = createFormatter();
    JsonObject input = new JsonObject();
    input.addProperty("int_value", 42);
    input.addProperty("long_value", "1234567890123");

    JsonObject output = roundTrip(formatter, input);

    assertEquals(42, output.get("int_value").getAsInt());
    assertEquals(1234567890123L, output.get("long_value").getAsLong());
  }

  @Test
  void shouldRejectMalformedTemporalValues() throws Exception {
    ProtoBufFormatter formatter = createFormatter();
    JsonObject input = new JsonObject();
    input.addProperty("long_value", "2026-99-99T18:30:00Z");

    assertThrows(IllegalArgumentException.class, () -> formatter.parseFromJson(input));
  }

  @Test
  void shouldRejectSubSecondDurationsForIntegerFields() throws Exception {
    ProtoBufFormatter formatter = createFormatter();
    JsonObject input = new JsonObject();
    input.addProperty("int_value", "PT1.5S");

    assertThrows(IllegalArgumentException.class, () -> formatter.parseFromJson(input));
  }

  @Test
  void shouldRejectIntegerOverflow() throws Exception {
    ProtoBufFormatter formatter = createFormatter();
    JsonObject input = new JsonObject();
    input.addProperty("int_value", 2147483648L);

    assertThrows(ArithmeticException.class, () -> formatter.parseFromJson(input));
  }

  private JsonObject roundTrip(ProtoBufFormatter formatter, JsonObject input) throws Exception {
    return formatter.parseToJson(formatter.parseFromJson(input), ParseMode.IGNORE);
  }

  private ProtoBufFormatter createFormatter() throws IOException {
    DescriptorProto nested = DescriptorProto.newBuilder()
        .setName("Nested")
        .addField(field("value", 1, FieldDescriptorProto.Type.TYPE_INT64, FieldDescriptorProto.Label.LABEL_OPTIONAL))
        .build();

    DescriptorProto temporal = DescriptorProto.newBuilder()
        .setName("Temporal")
        .addNestedType(nested)
        .addField(field("int_value", 1, FieldDescriptorProto.Type.TYPE_INT32, FieldDescriptorProto.Label.LABEL_OPTIONAL))
        .addField(field("long_value", 2, FieldDescriptorProto.Type.TYPE_INT64, FieldDescriptorProto.Label.LABEL_OPTIONAL))
        .addField(field("repeated_values", 3, FieldDescriptorProto.Type.TYPE_INT32, FieldDescriptorProto.Label.LABEL_REPEATED))
        .addField(FieldDescriptorProto.newBuilder()
            .setName("nested")
            .setNumber(4)
            .setType(FieldDescriptorProto.Type.TYPE_MESSAGE)
            .setTypeName(".test.Temporal.Nested")
            .setLabel(FieldDescriptorProto.Label.LABEL_OPTIONAL))
        .build();

    FileDescriptorProto file = FileDescriptorProto.newBuilder()
        .setName("temporal.proto")
        .setPackage("test")
        .setSyntax("proto2")
        .addMessageType(temporal)
        .build();

    FileDescriptorSet descriptorSet = FileDescriptorSet.newBuilder()
        .addFile(file)
        .build();

    return new ProtoBufFormatter("Temporal", descriptorSet.toByteArray());
  }

  private FieldDescriptorProto field(
      String name,
      int number,
      FieldDescriptorProto.Type type,
      FieldDescriptorProto.Label label) {
    return FieldDescriptorProto.newBuilder()
        .setName(name)
        .setNumber(number)
        .setType(type)
        .setLabel(label)
        .build();
  }
}
