package io.mapsmessaging.schemas.formatters;

import com.github.javafaker.Faker;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig.TYPE;
import io.mapsmessaging.schemas.formatters.impl.NativeFormatter;
import java.io.IOException;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestNativeFormatter {

  NativeFormatter getFormatter(TYPE type) throws IOException {
    NativeSchemaConfig config = new NativeSchemaConfig();
    config.setType(type);
    return (NativeFormatter) MessageFormatterFactory.getInstance().getFormatter(config);
  }

  @Test
  void constructorExceptions() {
    Assertions.assertThrowsExactly(IOException.class, () -> new NativeFormatter(null));
  }

  @Test
  void testStringDecoders() throws IOException {
    NativeFormatter formatter = getFormatter(TYPE.STRING);
    Assertions.assertEquals("This is a string", formatter.parse("This is a string".getBytes()).get("val"));
  }

  @Test
  void testNumericIntegerStringDecoders() throws IOException {
    NativeFormatter formatter = getFormatter(TYPE.NUMERIC_STRING);
    Assertions.assertEquals(1L, formatter.parse("1".getBytes()).get("val"));
    Assertions.assertEquals(Long.MAX_VALUE, formatter.parse(("" + Long.MAX_VALUE).getBytes()).get("val"));
    Assertions.assertEquals(Long.MIN_VALUE, formatter.parse(("" + Long.MIN_VALUE).getBytes()).get("val"));
    Assertions.assertEquals((long) (Integer.MAX_VALUE), formatter.parse(("" + Integer.MAX_VALUE).getBytes()).get("val"));
    Assertions.assertEquals((long) (Integer.MIN_VALUE), formatter.parse(("" + Integer.MIN_VALUE).getBytes()).get("val"));
    Faker faker = new Faker();
    for (int x = 0; x < 1000; x++) {
      long nextRandom = faker.random().nextLong();
      Assertions.assertEquals(nextRandom, formatter.parse(("" + nextRandom).getBytes()).get("val"));
    }
    Assertions.assertNull(formatter.parse(("This isn't a number").getBytes()).get("val"));
  }

  @Test
  void testNumericFloatStringDecoders() throws IOException {
    NativeFormatter formatter = getFormatter(TYPE.NUMERIC_STRING);
    Assertions.assertEquals(1.1, formatter.parse("1.1".getBytes()).get("val"));
    Assertions.assertEquals(Double.MAX_VALUE, formatter.parse(("" + Double.MAX_VALUE).getBytes()).get("val"));
    Assertions.assertEquals(Double.MIN_VALUE, formatter.parse(("" + Double.MIN_VALUE).getBytes()).get("val"));
    Assertions.assertEquals(Double.NaN, formatter.parse(("" + Double.NaN).getBytes()).get("val"));
    Faker faker = new Faker();
    for (int x = 0; x < 1000; x++) {
      double nextRandom = faker.random().nextDouble();
      Assertions.assertEquals(nextRandom, formatter.parse(("" + nextRandom).getBytes()).get("val"));
    }
    Assertions.assertNull(formatter.parse(("This isn't a number").getBytes()).get("val"));
  }

  @Test
  void testNumericDoubleDecoders() throws IOException {

    NativeFormatter formatter = getFormatter(TYPE.DOUBLE);
    double value = 1.1;
    Assertions.assertEquals(1.1, formatter.parse(pack(value)).get("val"));
    Assertions.assertEquals(Double.MAX_VALUE, formatter.parse(pack(Double.MAX_VALUE)).get("val"));
    Assertions.assertEquals(Double.MIN_VALUE, formatter.parse(pack(Double.MIN_VALUE)).get("val"));
    Faker faker = new Faker();
    for (int x = 0; x < 1000; x++) {
      double nextRandom = faker.random().nextDouble();
      Assertions.assertEquals(nextRandom, formatter.parse(pack(nextRandom)).get("val"));
    }

    formatter = getFormatter(TYPE.FLOAT);
    float fValue = 1.1f;
    Assertions.assertEquals(fValue, formatter.parse(pack(fValue)).get("val"));
    Assertions.assertEquals(Float.MAX_VALUE, formatter.parse(pack(Float.MAX_VALUE)).get("val"));
    Assertions.assertEquals(Float.MIN_VALUE, formatter.parse(pack(Float.MIN_VALUE)).get("val"));
    for (int x = 0; x < 1000; x++) {
      float nextRandom = (float) faker.random().nextDouble();
      Assertions.assertEquals(nextRandom, formatter.parse(pack(nextRandom)).get("val"));
    }
  }

  @Test
  void testNumericIntegerDecoders() throws IOException {
    NativeFormatter formatter = getFormatter(TYPE.INT64);
    Assertions.assertEquals(0L, formatter.parse(packLong(0L, 8)).get("val"));
    Assertions.assertEquals(Long.MAX_VALUE, formatter.parse(packLong(Long.MAX_VALUE, 8)).get("val"));
    Assertions.assertEquals(Long.MIN_VALUE, formatter.parse(packLong(Long.MIN_VALUE, 8)).get("val"));
    Faker faker = new Faker();
    for (int x = 0; x < 1000; x++) {
      long nextRandom = faker.random().nextLong();
      Assertions.assertEquals(nextRandom, formatter.parse(packLong(nextRandom, 8)).get("val"));
    }

    formatter = getFormatter(TYPE.INT32);
    Assertions.assertEquals(0, formatter.parse(packLong(0, 4)).get("val"));
    Assertions.assertEquals(Integer.MAX_VALUE, formatter.parse(packLong(Integer.MAX_VALUE, 4)).get("val"));
    Assertions.assertEquals(Integer.MIN_VALUE, formatter.parse(packLong(Integer.MIN_VALUE, 4)).get("val"));
    for (int x = 0; x < 1000; x++) {
      int nextRandom = (int) faker.random().nextLong();
      Assertions.assertEquals(nextRandom, formatter.parse(packLong(nextRandom, 4)).get("val"));
    }

    formatter = getFormatter(TYPE.INT16);
    Assertions.assertEquals((short) 0, formatter.parse(packLong(0, 2)).get("val"));
    Assertions.assertEquals(Short.MAX_VALUE, formatter.parse(packLong(Short.MAX_VALUE, 2)).get("val"));
    Assertions.assertEquals(Short.MIN_VALUE, formatter.parse(packLong(Short.MIN_VALUE, 2)).get("val"));
    for (int x = 0; x < 1000; x++) {
      short nextRandom = (short) faker.random().nextLong();
      Assertions.assertEquals(nextRandom, formatter.parse(packLong(nextRandom, 2)).get("val"));
    }

    formatter = getFormatter(TYPE.INT8);
    Assertions.assertEquals((byte) 0, formatter.parse(packLong(0, 1)).get("val"));
    Assertions.assertEquals(Byte.MAX_VALUE, formatter.parse(packLong(Byte.MAX_VALUE, 1)).get("val"));
    Assertions.assertEquals(Byte.MIN_VALUE, formatter.parse(packLong(Byte.MIN_VALUE, 1)).get("val"));
    for (int x = 0; x < 1000; x++) {
      byte nextRandom = (byte) faker.random().nextLong();
      Assertions.assertEquals(nextRandom, formatter.parse(packLong(nextRandom, 1)).get("val"));
    }
  }

  @Test
  void toJson() throws IOException {
    NativeFormatter formatter = getFormatter(TYPE.INT64);
    JSONObject jsonObject = formatter.parseToJson(packLong(0L, 8));
    Assertions.assertEquals(0L, jsonObject.getLong("value"));
    jsonObject = formatter.parseToJson(packLong(Long.MAX_VALUE, 8));
    Assertions.assertEquals(Long.MAX_VALUE, jsonObject.getLong("value"));

  }

  private byte[] pack(float val) {
    return packLong(Float.floatToIntBits(val), 4);
  }

  private byte[] pack(double val) {
    return packLong(Double.doubleToLongBits(val), 8);
  }

  private byte[] packLong(long val, int size) {
    byte[] ret = new byte[size];
    long t = val;
    for (int x = 0; x < size; x++) {
      ret[x] = (byte) (t & 0xff);
      t = t >> 8;
    }
    return ret;
  }
}
