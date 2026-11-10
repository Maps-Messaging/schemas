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
package io.mapsmessaging.schemas.formatters.impl.cdr;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.formatters.ParsedObject;

import java.math.BigInteger;
import java.util.List;

/**
 * Preserves unsigned 64-bit field values in structured access without changing the shared resolvers.
 * Other field types continue to use their existing resolution behaviour.
 */
public final class CdrUint64ParsedObject implements ParsedObject {
  private final ParsedObject delegate;
  private final JsonObject decoded;
  private final List<CdrField> fields;

  public CdrUint64ParsedObject(ParsedObject delegate, JsonObject decoded, List<CdrField> fields) {
    this.delegate = delegate;
    this.decoded = decoded;
    this.fields = fields;
  }

  @Override
  public Object get(String path) {
    BigInteger unsigned = resolveUnsigned(path);
    return unsigned != null ? unsigned : delegate.get(path);
  }

  @Override
  public List<String> getKeys() {
    return delegate.getKeys();
  }

  @Override
  public Object getReferenced() {
    return delegate.getReferenced();
  }

  private BigInteger resolveUnsigned(String path) {
    if (path == null || path.isEmpty()) {
      return null;
    }
    String[] segments = path.split("\\.", -1);
    List<CdrField> members = fields;
    CdrField field = null;
    JsonElement value = decoded;

    for (int part = 0; part < segments.length; part++) {
      String segment = segments[part];
      int bracket = segment.indexOf('[');
      String name = bracket < 0 ? segment : segment.substring(0, bracket);
      if (!value.isJsonObject()) {
        return null;
      }
      field = find(members, name);
      if (field == null || !value.getAsJsonObject().has(name)) {
        return null;
      }
      value = value.getAsJsonObject().get(name);

      int offset = bracket;
      while (offset >= 0) {
        if (!field.type().equals("array") && !field.type().equals("sequence")) {
          return null;
        }
        int close = segment.indexOf(']', offset + 1);
        if (close < 0) {
          return null;
        }
        int index;
        try {
          index = Integer.parseInt(segment.substring(offset + 1, close));
        } catch (NumberFormatException exception) {
          return null;
        }
        if (!value.isJsonArray() || index < 0 || index >= value.getAsJsonArray().size()) {
          return null;
        }
        value = value.getAsJsonArray().get(index);
        field = field.element();
        offset = close + 1;
        if (offset == segment.length()) {
          break;
        }
        if (segment.charAt(offset) != '[') {
          return null;
        }
      }

      if (part + 1 < segments.length) {
        if (!field.type().equals("struct")) {
          return null;
        }
        members = field.children();
      }
    }

    if (field == null || (!field.type().equals("uint64") && !field.type().equals("ulonglong"))
        || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber()) {
      return null;
    }
    return value.getAsBigInteger();
  }

  private CdrField find(List<CdrField> members, String name) {
    for (CdrField member : members) {
      if (name.equals(member.name())) {
        return member;
      }
    }
    return null;
  }
}
