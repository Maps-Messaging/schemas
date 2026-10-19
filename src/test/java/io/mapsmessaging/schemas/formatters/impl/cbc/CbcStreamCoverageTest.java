/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 */
package io.mapsmessaging.schemas.formatters.impl.cbc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CbcStreamCoverageTest {

  private final TestStream stream = new TestStream();

  @Test
  void requiredSizeAndEnumDetectionCoverValidAndMissingCases() {
    FieldSpecification sized = FieldSpecification.builder()
        .name("value")
        .type("uint")
        .size(8)
        .enumTable(Map.of(1, "ONE"))
        .build();

    assertEquals(8, stream.size(sized));
    assertTrue(stream.enumPresent(sized));

    FieldSpecification plain = FieldSpecification.builder()
        .name("plain")
        .type("uint")
        .build();

    assertFalse(stream.enumPresent(plain));
    assertThrows(IllegalStateException.class, () -> stream.size(plain));
  }

  @Test
  void bitFitValidationCoversBoundsAndSixtyFourBitPath() {
    FieldSpecification field = FieldSpecification.builder()
        .name("code")
        .type("enum")
        .size(8)
        .build();

    stream.fits(0, 8, field);
    stream.fits(255, 8, field);
    stream.fits(Long.MIN_VALUE, 64, field);
    stream.fits(Long.MAX_VALUE, 64, field);

    assertThrows(IllegalArgumentException.class, () -> stream.fits(-1, 8, field));
    assertThrows(IllegalArgumentException.class, () -> stream.fits(256, 8, field));
  }

  @Test
  void encalcLongSupportsAllOperatorsAndRejectsUnknownExpression() {
    assertEquals(5.0, stream.evalLong("v/2", 10), 0.0);
    assertEquals(20.0, stream.evalLong("v*2", 10), 0.0);
    assertEquals(12.0, stream.evalLong("v+2", 10), 0.0);
    assertEquals(8.0, stream.evalLong("v-2", 10), 0.0);
    assertThrows(IllegalArgumentException.class, () -> stream.evalLong("x+1", 10));
  }

  @Test
  void encalcDoubleSupportsAllOperatorsAndRejectsUnknownExpression() {
    assertEquals(2.5, stream.evalDouble("v/2", 5.0), 0.0);
    assertEquals(10.0, stream.evalDouble("v*2", 5.0), 0.0);
    assertEquals(7.0, stream.evalDouble("v+2", 5.0), 0.0);
    assertEquals(3.0, stream.evalDouble("v-2", 5.0), 0.0);
    assertThrows(IllegalArgumentException.class, () -> stream.evalDouble("x+1", 5.0));
  }

  private static final class TestStream extends CbcStream {
    int size(FieldSpecification field) {
      return reqSizeBits(field);
    }

    void fits(long code, int bits, FieldSpecification field) {
      requireFitsBits(code, bits, field);
    }

    boolean enumPresent(FieldSpecification field) {
      return hasEnum(field);
    }

    double evalLong(String expression, long value) {
      return evalEncalc(expression, value);
    }

    double evalDouble(String expression, double value) {
      return evalEncalc(expression, value);
    }
  }
}
