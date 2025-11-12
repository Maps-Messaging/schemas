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

package io.mapsmessaging.schemas.formatters.impl.cbc;

import io.mapsmessaging.schemas.config.impl.cbc.CbcFormat;
import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;

import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class CbcOutputStream extends CbcStream {
  // ------------------------ Encode ------------------------
  @SuppressWarnings("unchecked")
  public byte[] encode(CbcFormat schema, Map<String, Object> fieldValues) {
    BitWriter w = new BitWriter();

    if (schema.getMessageKey() > 0) {
      w.writeUnsigned(schema.getMessageKey(), 16);
    }

    for (io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f : schema.getFields()) {
      writeField(w, f, fieldValues.get(f.getName()));
    }

    // CRC append deferred until placement defined in schema
    return w.toByteArray();
  }


  @SuppressWarnings("unchecked")
  private void writeField(BitWriter w, FieldSpecification f, Object value) {
    boolean present = value != null;

    if (Boolean.TRUE.equals(f.getOptional())) {
      w.writeUnsigned(present ? 1 : 0, 1);
      if (!present) return;
    }

    String t = f.getType().toLowerCase();
    switch (t) {
      case "struct" -> writeStruct(f, w, value);
      case "uint", "bitmask" -> {
        long raw = hasEnum(f) ? enumToCode(value, f) : toUintRaw(value, f);
        requireFitsBits(raw, reqSizeBits(f), f);
        w.writeUnsigned(raw, reqSizeBits(f));
      }
      case "int" -> {
        long raw = toIntRaw(value, f);
        w.writeSigned(raw, reqSizeBits(f));
      }
      case "string" -> w.writeString(f, w, value);
      case "data" -> writeData(f, w, value);
      case "enum" -> {
        long code = toEnumRaw(value, f);
        w.writeUnsigned(code, reqSizeBits(f));
      }
      default -> throw new IllegalArgumentException("Unsupported CBC type in encode: " + f.getType());
    }
  }

  // --- helpers ---
  private long toEnumRaw(Object value, FieldSpecification f) {
    var table = f.getEnumTable();
    if (table == null || table.isEmpty()) {
      throw new IllegalArgumentException("Enum table missing for field '" + f.getName() + "'");
    }

    if (value instanceof Number n) {
      long code = n.longValue();
      if (!table.containsKey((int) code)) {
        throw new IllegalArgumentException("Unknown enum code " + code + " for '" + f.getName() + "'");
      }
      return code;
    }

    String symbol = Objects.toString(value, null);
    if (symbol == null) {
      throw new IllegalArgumentException("Null enum value for '" + f.getName() + "'");
    }

    // find code by symbol
    for (Map.Entry<Integer, String> e : table.entrySet()) {
      if (symbol.equals(e.getValue())) return e.getKey();
    }
    throw new IllegalArgumentException("Unknown enum symbol '" + symbol + "' for '" + f.getName() + "'");
  }


  private long enumToCode(Object value, FieldSpecification field) {
    var table = field.getEnumTable();
    if (value instanceof Number number) {
      long code = number.longValue();
      if (!table.containsKey((int) code)) {
        throw new IllegalArgumentException("Unknown enum code " + code + " for '" + field.getName() + "'");
      }
      return code;
    }
    String text = Objects.toString(value, null);
    if (text == null) {
      throw new IllegalArgumentException("Null enum value for '" + field.getName() + "'");
    }
    // match by symbol
    for (Map.Entry<Integer, String> entry : table.entrySet()) {
      if (text.equals(entry.getValue())) return entry.getKey();
    }
    // allow numeric-in-string if present in table
    try {
      long parsed = Long.parseLong(text);
      if (table.containsKey((int) parsed)) return parsed;
    } catch (NumberFormatException ignore) {
    }
    throw new IllegalArgumentException("Unknown enum symbol '" + text + "' for '" + field.getName() + "'");
  }


  @SuppressWarnings("unchecked")
  private void writeStruct(FieldSpecification f, BitWriter w, Object value) {
    if (value == null) return; // optional already handled

    // 1) Map-based struct (existing)
    if (value instanceof Map<?, ?> map) {
      if (f.getFields() != null) {
        for (FieldSpecification child : f.getFields()) {
          writeField(w, child, map.get(child.getName()));
        }
      }
      return;
    }

    // 2) Array/List-based struct
    if (value instanceof List<?> list) {
      writeArrayStruct(f, w, list);
      return;
    }
    if (value.getClass().isArray()) {
      int len = java.lang.reflect.Array.getLength(value);
      java.util.ArrayList<Object> list = new java.util.ArrayList<>(len);
      for (int i = 0; i < len; i++) list.add(java.lang.reflect.Array.get(value, i));
      writeArrayStruct(f, w, list);
      return;
    }

    throw new IllegalArgumentException("Struct '" + f.getName() + "' expects Map or List/array");
  }

  private void writeArrayStruct(FieldSpecification f, BitWriter w, java.util.List<?> list) {
    var children = f.getFields();
    if (children == null || children.isEmpty()) {
      throw new IllegalArgumentException("Struct '" + f.getName() + "' has no child fields for array data");
    }

    if (children.size() == list.size()) {
      // Fixed array encoded as aN child fields
      for (int i = 0; i < children.size(); i++) {
        writeField(w, children.get(i), list.get(i));
      }
      return;
    }

    if (children.size() == 1) {
      // Homogeneous array encoded by repeating the single child spec in order
      FieldSpecification elem = children.get(0);
      for (Object o : list) {
        writeField(w, elem, o);
      }
      return;
    }

    throw new IllegalArgumentException(
        "Struct '" + f.getName() + "' array length " + list.size() +
            " doesn't match child spec count " + children.size());
  }


  private void writeData(FieldSpecification f, BitWriter w, Object value) {
    int n = reqSizeBits(f) / 8;
    if (!Boolean.TRUE.equals(f.getFixed())) {
      throw new IllegalArgumentException("data with fixed=false not supported without length semantics");
    }
    if (value instanceof String encoded) {
      value = Base64.getDecoder().decode(encoded);
    }
    if (!(value instanceof byte[] bytes)) {
      throw new IllegalArgumentException("Field '" + f.getName() + "' expects byte[]");
    }
    if (bytes.length != n) {
      byte[] out = new byte[n];
      System.arraycopy(bytes, 0, out, 0, Math.min(bytes.length, n));
      w.writeRawBytes(out);
    } else {
      w.writeRawBytes(bytes);
    }
  }

  private long toUintRaw(Object v, io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f) {
    if (v instanceof Boolean bool) {
      return bool ? 1 : 0;
    }
    if (!(v instanceof Number n)) throw new IllegalArgumentException("Field '" + f.getName() + "' expects numeric");
    double dv = n.doubleValue();
    String expr = f.getEncalc();
    if (expr != null) dv = evalEncalc(expr, dv);
    if (dv < 0) dv = 0; // clip at 0 for uint
    long raw = Math.round(dv);
    int bits = reqSizeBits(f);
    long mask = (bits == 64) ? -1L : ((1L << bits) - 1L);
    return raw & mask;
  }

  private long toIntRaw(Object v, io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f) {
    if (!(v instanceof Number n)) throw new IllegalArgumentException("Field '" + f.getName() + "' expects numeric");
    if (n instanceof Long l) {
      String expr = f.getEncalc();
      if (expr != null) {
        return Math.round(evalEncalc(expr, l));
      }
      return l;
    } else if (n instanceof Integer i) {
      String expr = f.getEncalc();
      if (expr != null) {
        return Math.round(evalEncalc(expr, i));
      }
      return i;
    } else {
      double dv = n.doubleValue();
      String expr = f.getEncalc();
      if (expr != null) dv = evalEncalc(expr, dv);
      return Math.round(dv);
    }
  }
}
