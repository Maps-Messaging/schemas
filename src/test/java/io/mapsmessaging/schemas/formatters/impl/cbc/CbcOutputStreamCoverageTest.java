/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 */
package io.mapsmessaging.schemas.formatters.impl.cbc;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.schemas.config.impl.cbc.CbcFormat;
import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CbcOutputStreamCoverageTest {

  private final CbcOutputStream output = new CbcOutputStream();

  @Test
  void encodesMessageKeyUnsignedSignedAndOptionalFields() {
    CbcFormat format = format(
        0x1234,
        FieldSpecification.builder().name("flag").type("uint").size(1).build(),
        FieldSpecification.builder().name("signed").type("int").size(8).build(),
        FieldSpecification.builder().name("optional").type("uint").size(8).optional(true).build()
    );

    byte[] bytes = output.encode(
        format,
        Map.of("flag", true, "signed", -2)
    );

    assertTrue(bytes.length >= 4);
  }

  @Test
  void optionalPresentFieldIsWritten() {
    CbcFormat format = format(
        0,
        FieldSpecification.builder()
            .name("optional")
            .type("uint")
            .size(8)
            .optional(true)
            .build()
    );

    byte[] bytes = output.encode(format, Map.of("optional", 7));

    assertTrue(bytes.length > 0);
  }

  @Test
  void uintSupportsBooleanNumericEncalcAndClipping() {
    CbcFormat boolFormat = format(
        0,
        FieldSpecification.builder().name("flag").type("uint").size(1).build()
    );
    assertTrue(output.encode(boolFormat, Map.of("flag", true)).length > 0);

    CbcFormat scaled = format(
        0,
        FieldSpecification.builder()
            .name("value")
            .type("uint")
            .size(8)
            .encalc("v*2")
            .build()
    );
    assertTrue(output.encode(scaled, Map.of("value", 10)).length > 0);

    CbcFormat clipped = format(
        0,
        FieldSpecification.builder().name("value").type("uint").size(8).build()
    );
    assertTrue(output.encode(clipped, Map.of("value", -10)).length > 0);

    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(clipped, Map.of("value", "not-number"))
    );
  }

  @Test
  void signedIntSupportsLongIntegerAndDoublePaths() {
    CbcFormat longFormat = format(
        0,
        FieldSpecification.builder()
            .name("value")
            .type("int")
            .size(16)
            .encalc("v+1")
            .build()
    );
    assertTrue(output.encode(longFormat, Map.of("value", 10L)).length > 0);
    assertTrue(output.encode(longFormat, Map.of("value", 10)).length > 0);
    assertTrue(output.encode(longFormat, Map.of("value", 10.5d)).length > 0);

    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(longFormat, Map.of("value", "bad"))
    );
  }

  @Test
  void enumSupportsCodesSymbolsAndNumericStrings() {
    FieldSpecification field = FieldSpecification.builder()
        .name("mode")
        .type("enum")
        .size(8)
        .enumTable(Map.of(1, "ONE", 2, "TWO"))
        .build();

    CbcFormat format = format(0, field);

    assertTrue(output.encode(format, Map.of("mode", 1)).length > 0);
    assertTrue(output.encode(format, Map.of("mode", "TWO")).length > 0);

    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(format, Map.of("mode", 9))
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(format, Map.of("mode", "MISSING"))
    );
  }

  @Test
  void enumTypeRequiresTable() {
    CbcFormat format = format(
        0,
        FieldSpecification.builder().name("mode").type("enum").size(8).build()
    );

    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(format, Map.of("mode", 1))
    );
  }

  @Test
  void uintEnumSupportsSymbolCodeAndNumericString() {
    FieldSpecification field = FieldSpecification.builder()
        .name("mode")
        .type("uint")
        .size(8)
        .enumTable(Map.of(1, "ONE", 2, "TWO"))
        .build();

    CbcFormat format = format(0, field);

    assertTrue(output.encode(format, Map.of("mode", "ONE")).length > 0);
    assertTrue(output.encode(format, Map.of("mode", 2)).length > 0);
    assertTrue(output.encode(format, Map.of("mode", "2")).length > 0);

    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(format, Map.of("mode", "THREE"))
    );
  }

  @Test
  void dataAcceptsBytesAndBase64AndPadsOrTruncates() {
    FieldSpecification field = FieldSpecification.builder()
        .name("data")
        .type("data")
        .size(16)
        .fixed(true)
        .build();

    CbcFormat format = format(0, field);

    byte[] exact = output.encode(format, Map.of("data", new byte[]{1, 2}));
    byte[] shortValue = output.encode(format, Map.of("data", new byte[]{1}));
    byte[] longValue = output.encode(format, Map.of("data", new byte[]{1, 2, 3}));
    byte[] encoded = output.encode(
        format,
        Map.of("data", java.util.Base64.getEncoder().encodeToString(new byte[]{1, 2}))
    );

    assertArrayEquals(exact, encoded);
    assertEquals(exact.length, shortValue.length);
    assertEquals(exact.length, longValue.length);

    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(format, Map.of("data", "%%%"))
    );
  }

  @Test
  void dataRejectsVariableLengthAndWrongValueType() {
    CbcFormat variable = format(
        0,
        FieldSpecification.builder()
            .name("data")
            .type("data")
            .size(16)
            .fixed(false)
            .build()
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(variable, Map.of("data", new byte[]{1, 2}))
    );

    CbcFormat fixed = format(
        0,
        FieldSpecification.builder()
            .name("data")
            .type("data")
            .size(16)
            .fixed(true)
            .build()
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(fixed, Map.of("data", 42))
    );
  }

  @Test
  void structSupportsMapListAndArrayShapes() {
    FieldSpecification a = FieldSpecification.builder()
        .name("a")
        .type("uint")
        .size(8)
        .build();
    FieldSpecification b = FieldSpecification.builder()
        .name("b")
        .type("uint")
        .size(8)
        .build();
    FieldSpecification struct = FieldSpecification.builder()
        .name("struct")
        .type("struct")
        .fields(List.of(a, b))
        .build();

    CbcFormat format = format(0, struct);

    assertTrue(
        output.encode(format, Map.of("struct", Map.of("a", 1, "b", 2))).length > 0
    );
    assertTrue(
        output.encode(format, Map.of("struct", List.of(1, 2))).length > 0
    );
    assertTrue(
        output.encode(format, Map.of("struct", new Integer[]{1, 2})).length > 0
    );
  }

  @Test
  void homogeneousStructArrayRepeatsSingleChildSpec() {
    FieldSpecification element = FieldSpecification.builder()
        .name("value")
        .type("uint")
        .size(8)
        .build();
    FieldSpecification struct = FieldSpecification.builder()
        .name("values")
        .type("struct")
        .fields(List.of(element))
        .build();

    assertTrue(
        output.encode(format(0, struct), Map.of("values", List.of(1, 2, 3))).length > 0
    );
  }

  @Test
  void structRejectsMissingChildrenWrongLengthAndUnsupportedValueShape() {
    FieldSpecification empty = FieldSpecification.builder()
        .name("empty")
        .type("struct")
        .build();
    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(format(0, empty), Map.of("empty", List.of(1)))
    );

    FieldSpecification a = FieldSpecification.builder()
        .name("a")
        .type("uint")
        .size(8)
        .build();
    FieldSpecification b = FieldSpecification.builder()
        .name("b")
        .type("uint")
        .size(8)
        .build();
    FieldSpecification mismatch = FieldSpecification.builder()
        .name("struct")
        .type("struct")
        .fields(List.of(a, b))
        .build();

    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(format(0, mismatch), Map.of("struct", List.of(1, 2, 3)))
    );
    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(format(0, mismatch), Map.of("struct", "bad"))
    );
  }

  @Test
  void unsupportedTypeIsRejected() {
    CbcFormat format = format(
        0,
        FieldSpecification.builder().name("x").type("unknown").build()
    );

    assertThrows(
        IllegalArgumentException.class,
        () -> output.encode(format, Map.of("x", 1))
    );
  }

  private CbcFormat format(int messageKey, FieldSpecification... fields) {
    CbcFormat format = new CbcFormat();
    format.setMessageKey(messageKey);
    format.setFields(List.of(fields));
    return format;
  }
}
