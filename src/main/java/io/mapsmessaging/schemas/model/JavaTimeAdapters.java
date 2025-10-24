/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.schemas.model;


import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

public final class JavaTimeAdapters {

  // Optional helpers if you need Instant/LocalDateTime too
  public static final TypeAdapter<Instant> INSTANT_ADAPTER = new TypeAdapter<>() {
    @Override
    public void write(JsonWriter out, Instant value) throws java.io.IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.value(value.toString()); // ISO-8601
    }

    @Override
    public Instant read(JsonReader in) throws java.io.IOException {
      JsonToken t = in.peek();
      if (t == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      if (t == JsonToken.NUMBER) {
        long n = in.nextLong();
        return Math.abs(n) < 1_000_000_000_000L ? Instant.ofEpochSecond(n) : Instant.ofEpochMilli(n);
      }
      return Instant.parse(in.nextString().trim());
    }
  };
  private static final DateTimeFormatter ISO_OFFSET = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
  private static final DateTimeFormatter ISO_LOCAL = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
  private static final DateTimeFormatter SPACE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  public static final TypeAdapter<OffsetDateTime> OFFSET_DATE_TIME_ADAPTER = new TypeAdapter<>() {
    @Override
    public void write(JsonWriter out, OffsetDateTime value) throws java.io.IOException {
      if (value == null) {
        out.nullValue();
        return;
      }
      out.value(value.withOffsetSameInstant(ZoneOffset.UTC).format(ISO_OFFSET));
    }

    @Override
    public OffsetDateTime read(JsonReader in) throws java.io.IOException {
      JsonToken t = in.peek();
      if (t == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      if (t == JsonToken.NUMBER) {
        long n = in.nextLong();
        // Heuristic: seconds vs millis
        Instant inst = Math.abs(n) < 1_000_000_000_000L ? Instant.ofEpochSecond(n) : Instant.ofEpochMilli(n);
        return inst.atOffset(ZoneOffset.UTC);
      }
      String s = in.nextString().trim();
      if (s.isEmpty()) return null;
      // Try ISO with offset
      try {
        return OffsetDateTime.parse(s, ISO_OFFSET);
      } catch (Exception ignored) {
      }
      // Try ISO local -> assume UTC
      try {
        return LocalDateTime.parse(s, ISO_LOCAL).atOffset(ZoneOffset.UTC);
      } catch (Exception ignored) {
      }
      // Try legacy "yyyy-MM-dd HH:mm:ss" -> UTC
      try {
        return LocalDateTime.parse(s, SPACE_FMT).atOffset(ZoneOffset.UTC);
      } catch (Exception ignored) {
      }
      // Fallback: epoch as string
      try {
        long n = Long.parseLong(s);
        Instant inst = Math.abs(n) < 1_000_000_000_000L ? Instant.ofEpochSecond(n) : Instant.ofEpochMilli(n);
        return inst.atOffset(ZoneOffset.UTC);
      } catch (Exception e) {
        throw new JsonParseException("Cannot parse OffsetDateTime: " + s, e);
      }
    }
  };

  private JavaTimeAdapters() {
  }

  public static GsonBuilder registerJavaTime(GsonBuilder b) {
    return b.registerTypeAdapter(TypeToken.get(OffsetDateTime.class).getType(), OFFSET_DATE_TIME_ADAPTER)
        .registerTypeAdapter(TypeToken.get(Instant.class).getType(), INSTANT_ADAPTER);
  }
}
