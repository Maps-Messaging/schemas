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
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CdrSchemaConfig;
import io.mapsmessaging.schemas.formatters.impl.CdrFormatter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class TestCdrFormatter {

  private MessageFormatter formatter(String json) throws IOException {
    CdrSchemaConfig config = new CdrSchemaConfig();
    config.setSchema(JsonParser.parseString(json).getAsJsonObject());
    return MessageFormatterFactory.getInstance().getFormatter(config);
  }

  @Test
  void factoryDiscoversFormatterAndSchemaConfig() throws IOException {
    assertTrue(MessageFormatterFactory.getInstance().getFormatters().contains("CDR"));
    SchemaConfig config = SchemaConfig.gson.fromJson(
        "{\"format\":\"cdr\",\"schema\":{\"fields\":[]}}", SchemaConfig.class);
    assertInstanceOf(CdrSchemaConfig.class, config);
    assertInstanceOf(CdrFormatter.class,
        MessageFormatterFactory.getInstance().getFormatter(config));
  }

  @Test
  void bigEndianKnownDdsVector() throws IOException {
    MessageFormatter formatter = formatter("""
        {"littleEndian":false,"fields":[
          {"name":"a","type":"long"},
          {"name":"b","type":"array","length":4,"element":"char"}
        ]}""");
    JsonObject value = JsonParser.parseString(
        "{\"a\":1,\"b\":[\"a\",\"b\",\"c\",\"d\"]}").getAsJsonObject();
    byte[] bytes = formatter.parseFromJson(value);
    assertArrayEquals(new byte[]{
        0, 0, 0, 0, 0, 0, 0, 1, 97, 98, 99, 100
    }, bytes);
    assertEquals(value, formatter.parseToJson(bytes, ParseMode.STRICT));
  }

  @Test
  void littleEndianAlignmentAndStringVector() throws IOException {
    MessageFormatter formatter = formatter("""
        {"fields":[
          {"name":"enabled","type":"boolean"},
          {"name":"number","type":"double"},
          {"name":"label","type":"string"}
        ]}""");
    JsonObject value = JsonParser.parseString(
        "{\"enabled\":true,\"number\":1.0,\"label\":\"Hi\"}").getAsJsonObject();
    byte[] bytes = formatter.parseFromJson(value);
    assertEquals(0, bytes[0]);
    assertEquals(1, bytes[1]);
    assertEquals(1, bytes[4]);
    for (int i = 5; i < 12; i++) {
      assertEquals(0, bytes[i], "CDR 8-byte alignment at " + i);
    }
    assertArrayEquals(new byte[]{0, 0, 0, 0, 0, 0, (byte) 0xf0, 0x3f},
        Arrays.copyOfRange(bytes, 12, 20));
    assertArrayEquals(new byte[]{3, 0, 0, 0, 72, 105, 0},
        Arrays.copyOfRange(bytes, 20, 27));
    assertEquals(value, formatter.parseToJson(bytes, ParseMode.STRICT));
  }

  @Test
  void nestedStructSequenceAndUnsigned64RoundTrip() throws IOException {
    MessageFormatter formatter = formatter("""
        {"fields":[
          {"name":"position","type":"struct","fields":[
            {"name":"x","type":"float"},{"name":"y","type":"float"}]},
          {"name":"samples","type":"sequence","element":"int16"},
          {"name":"id","type":"uint64"}
        ]}""");
    JsonObject value = JsonParser.parseString("""
        {"position":{"x":1.5,"y":-2.25},
         "samples":[-123,456,789],
         "id":18446744073709551615}""").getAsJsonObject();
    byte[] data = formatter.parseFromJson(value);
    assertEquals(value, formatter.parseToJson(data, ParseMode.STRICT));
    assertEquals(3, formatter.parse(data, ParseMode.STRICT).getKeys().size());
    assertEquals(-123, ((Number) formatter.parse(data, ParseMode.STRICT).get("samples[0]")).intValue());
  }

  @Test
  void decodeUsesHeaderEndiannessNotEncodingDefault() throws IOException {
    MessageFormatter big = formatter("""
        {"littleEndian":false,"fields":[{"name":"number","type":"uint32"}]}""");
    MessageFormatter little = formatter("""
        {"fields":[{"name":"number","type":"uint32"}]}""");
    JsonObject value = JsonParser.parseString("{\"number\":305419896}").getAsJsonObject();
    assertEquals(value, little.parseToJson(big.parseFromJson(value), ParseMode.STRICT));
    assertEquals(value, big.parseToJson(little.parseFromJson(value), ParseMode.STRICT));
  }

  @Test
  void badInputStrictAndIgnore() throws IOException {
    MessageFormatter formatter = formatter("""
        {"fields":[{"name":"value","type":"long"}]}""");
    byte[] valid = formatter.parseFromJson(
        JsonParser.parseString("{\"value\":123}").getAsJsonObject());
    byte[] truncated = Arrays.copyOf(valid, valid.length - 1);
    assertThrows(ParseException.class, () -> formatter.parse(truncated, ParseMode.STRICT));
    assertArrayEquals(truncated,
        (byte[]) formatter.parse(truncated, ParseMode.IGNORE).getReferenced());
    assertThrows(ParseException.class, () -> formatter.parseToJson(new byte[]{0, 2, 0, 0}, ParseMode.STRICT));
    assertThrows(ParseException.class, () -> formatter.parseToJson(new byte[]{0, 3, 0, 0}, ParseMode.STRICT));
    assertThrows(ParseException.class, () -> formatter.parseToJson(
        Arrays.copyOf(valid, valid.length + 1), ParseMode.STRICT));
  }

  @Test
  void rejectInvalidSchemaValuesAndCollections() throws IOException {
    CdrSchemaConfig invalid = new CdrSchemaConfig();
    assertThrows(IOException.class,
        () -> MessageFormatterFactory.getInstance().getFormatter(invalid));
    MessageFormatter formatter = formatter("""
        {"fields":[
          {"name":"numbers","type":"array","length":2,"element":"uint16"}
        ]}""");
    assertThrows(IOException.class, () -> formatter.parseFromJson(
        JsonParser.parseString("{\"numbers\":[1]}").getAsJsonObject()));
    assertThrows(IOException.class, () -> formatter.parseFromJson(
        JsonParser.parseString("{\"numbers\":[1,65536]}").getAsJsonObject()));
    assertThrows(IOException.class, () -> formatter.parseFromJson(
        JsonParser.parseString("{}").getAsJsonObject()));
    assertThrows(IOException.class, () -> formatter("{\"fields\":[{\"name\":\"x\",\"type\":\"union\"}]}"));
  }

  @Test
  void rejectsInvalidCdrStringLengthAndTerminator() throws IOException {
    MessageFormatter formatter = formatter("""
        {"fields":[{"name":"message","type":"string"}]}""");
    byte[] bytes = formatter.parseFromJson(JsonParser.parseString(
        "{\"message\":\"DDS\"}").getAsJsonObject());
    byte[] length = bytes.clone();
    length[4] = 100;
    assertThrows(ParseException.class, () -> formatter.parseToJson(length, ParseMode.STRICT));
    byte[] terminator = bytes.clone();
    terminator[terminator.length - 1] = 1;
    assertThrows(ParseException.class, () -> formatter.parseToJson(terminator, ParseMode.STRICT));
  }
}
