/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package io.mapsmessaging.schemas.config;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class LocalDateTimeSerializerTest {


  private LocalDateTimeDeserializer deserializer;
  private LocalDateTimeSerializer serializer;

  @BeforeEach
  void setUp() {
    deserializer = new LocalDateTimeDeserializer();
    serializer = new LocalDateTimeSerializer();
  }

  @Test
  void testDeserialize() throws IOException {
    JsonParser jsonParser = mock(JsonParser.class);
    DeserializationContext deserializationContext = mock(DeserializationContext.class);

    when(jsonParser.getText()).thenReturn("2024-09-13 12:34:56");

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    LocalDateTime expected = LocalDateTime.parse("2024-09-13 12:34:56", formatter);

    LocalDateTime result = deserializer.deserialize(jsonParser, deserializationContext);

    assertEquals(expected, result);
  }

  @Test
  void testSerialize() throws IOException {
    JsonGenerator jsonGenerator = mock(JsonGenerator.class);
    SerializerProvider serializerProvider = mock(SerializerProvider.class);

    LocalDateTime dateTime = LocalDateTime.of(2024, 9, 13, 12, 34, 56);

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    String expected = "2024-09-13 12:34:56";

    serializer.serialize(dateTime, jsonGenerator, serializerProvider);

    verify(jsonGenerator, times(1)).writeString(expected);
  }
}
