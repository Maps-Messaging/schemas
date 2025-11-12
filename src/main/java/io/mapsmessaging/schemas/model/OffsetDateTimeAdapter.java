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

public final class OffsetDateTimeAdapter extends com.google.gson.TypeAdapter<java.time.OffsetDateTime> {
  private static final java.time.format.DateTimeFormatter ISO = java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME;

  @Override
  public void write(com.google.gson.stream.JsonWriter out, java.time.OffsetDateTime value) throws java.io.IOException {
    if (value == null) {
      out.nullValue();
      return;
    }
    out.value(value.withOffsetSameInstant(java.time.ZoneOffset.UTC).format(ISO));
  }

  @Override
  public java.time.OffsetDateTime read(com.google.gson.stream.JsonReader in) throws java.io.IOException {
    var t = in.peek();
    if (t == com.google.gson.stream.JsonToken.NULL) {
      in.nextNull();
      return null;
    }
    if (t == com.google.gson.stream.JsonToken.NUMBER) {
      long n = in.nextLong();
      var inst = Math.abs(n) < 1_000_000_000_000L ? java.time.Instant.ofEpochSecond(n) : java.time.Instant.ofEpochMilli(n);
      return inst.atOffset(java.time.ZoneOffset.UTC);
    }
    var s = in.nextString().trim();
    if (s.isEmpty()) return null;
    try {
      return java.time.OffsetDateTime.parse(s, ISO);
    } catch (Exception ignored) {
    }
    try {
      return java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME).atOffset(java.time.ZoneOffset.UTC);
    } catch (Exception ignored) {
    }
    try {
      var dt = java.time.LocalDateTime.parse(s, java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
      return dt.atOffset(java.time.ZoneOffset.UTC);
    } catch (Exception e) {
      try {
        long n = Long.parseLong(s);
        var inst = Math.abs(n) < 1_000_000_000_000L ? java.time.Instant.ofEpochSecond(n) : java.time.Instant.ofEpochMilli(n);
        return inst.atOffset(java.time.ZoneOffset.UTC);
      } catch (Exception ex) {
        throw new com.google.gson.JsonParseException("Cannot parse OffsetDateTime: " + s, ex);
      }
    }
  }
}
