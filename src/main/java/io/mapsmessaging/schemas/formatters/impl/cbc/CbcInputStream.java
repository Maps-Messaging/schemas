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

import java.util.*;
import java.util.regex.Pattern;

public class CbcInputStream extends CbcStream {
  private static final Pattern INDEXED_NAME = Pattern.compile("^a\\d+$");

  public Map<String, Object> decode(CbcFormat schema, byte[] data) {
    if (schema == null) throw new IllegalStateException("CBC SchemaConfig not set on formatter");

    BitReader cursor = new BitReader(data);
    Map<String, Object> out = new LinkedHashMap<>();

    // header
    if (schema.getMessageKey() > 0) {
      out.put("messageKey", cursor.readUnsigned(16));
    }

    for (io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f : schema.getFields()) {
      Object v = readField(cursor, f);
      if (v != null) out.put(f.getName(), v);
    }

    // CRC handling deferred until placement defined in schema
    return out;
  }

  private Object readField(BitReader c, FieldSpecification f) {
    boolean present;
    if (Boolean.TRUE.equals(f.getOptional())) {
      present = c.readUnsigned(1) == 1L;
      if (!present) return null;
    }

    String t = f.getType().toLowerCase();
    switch (t) {
      case "struct" -> {
        return readStruct(f, c);
      }
      case "uint", "bitmask" -> {
        long raw = c.readUnsigned(reqSizeBits(f));
        if (hasEnum(f)) return codeToEnum(raw, f);
        return applyDecalcUint(raw, f);
      }
      case "int" -> {
        long raw = c.readSigned(reqSizeBits(f));
        return applyDecalcInt(raw, f);
      }
      case "string" -> {
        return c.readString(f);
      }
      case "data" -> {
        byte[] data = readData(f, c);
        if (data == null) return data;
        return Base64.getEncoder().encodeToString(data);
      }
      case "enum" -> {
        long code = c.readUnsigned(reqSizeBits(f));
        return fromEnumRaw(code, f);
      }
      default -> throw new IllegalArgumentException("Unsupported CBC type in decode: " + f.getType());
    }
  }

  private Object readStruct(FieldSpecification field, BitReader bitReader) {
    Map<String, Object> map = new LinkedHashMap<>();
    if (field.getFields() != null) {
      for (FieldSpecification child : field.getFields()) {
        Object childValue = readField(bitReader, child);
        if (childValue != null) map.put(child.getName(), childValue);
      }
    }
    if (looksLikeIndexedArray(map)) {
      return toIndexedArray(map);
    }
    return map;
  }

  private boolean looksLikeIndexedArray(Map<String, Object> map) {
    if (map.isEmpty()) return false;
    int maxIndex = -1;
    for (String key : map.keySet()) {
      if (!INDEXED_NAME.matcher(key).matches()) return false;
      int idx = Integer.parseInt(key.substring(1));
      if (idx > maxIndex) maxIndex = idx;
    }
    for (int i = 0; i <= maxIndex; i++) {
      if (!map.containsKey("a" + i)) return false;
    }
    return true;
  }

  private List<Object> toIndexedArray(Map<String, Object> map) {
    int maxIndex = -1;
    for (String key : map.keySet()) {
      int idx = Integer.parseInt(key.substring(1));
      if (idx > maxIndex) maxIndex = idx;
    }
    ArrayList<Object> list = new ArrayList<>(maxIndex + 1);
    for (int i = 0; i <= maxIndex; i++) {
      list.add(map.get("a" + i));
    }
    return list;
  }

  private byte[] readData(FieldSpecification f, BitReader c) {
    if (!Boolean.TRUE.equals(f.getFixed())) {
      throw new IllegalArgumentException("data with fixed=false not supported without length semantics");
    }
    int n = reqSizeBits(f) / 8;
    return c.readBytes(n);
  }

  private Number applyDecalcUint(long raw, io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f) {
    return applyDecalcInt(raw, f);
  }

  private Object fromEnumRaw(long code, FieldSpecification f) {
    var table = f.getEnumTable();
    if (table == null || table.isEmpty()) return code;  // fallback: return numeric if no table
    String symbol = table.get((int) code);
    return (symbol != null) ? symbol : code;            // prefer symbol, fallback to code
  }


  private Double evalDecalc(String expr, double v) {
    String e = expr.trim();
    if (e.startsWith("v/")) {
      double denom = Double.parseDouble(e.substring(2).trim());
      return v / denom;
    } else if (e.startsWith("v*")) {
      double mult = Double.parseDouble(e.substring(2).trim());
      return v * mult;
    } else if (e.startsWith("v+")) {
      double add = Double.parseDouble(e.substring(2).trim());
      return v + add;
    } else if (e.startsWith("v-")) {
      double sub = Double.parseDouble(e.substring(2).trim());
      return v - sub;
    }
    throw new IllegalArgumentException("Unsupported decalc expression: " + expr);
  }


  private Number applyDecalcInt(long raw, io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f) {
    String expr = f.getDecalc();
    if (expr == null) return raw;
    Double out = evalDecalc(expr, raw);
    long li = (long) out.doubleValue();
    return (out == li) ? li : out;
  }


  private Object codeToEnum(long code, FieldSpecification field) {
    var table = field.getEnumTable();
    String symbol = table.get((int) code);
    return symbol != null ? symbol : code; // prefer symbol, fall back to numeric
  }


}
