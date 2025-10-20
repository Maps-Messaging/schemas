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

package io.mapsmessaging.schemas.formatters.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CbcSchemaConfig;
import io.mapsmessaging.schemas.config.impl.cbc.BitReader;
import io.mapsmessaging.schemas.config.impl.cbc.BitWriter;
import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

public class CbcFormatter extends MessageFormatter {

  private final CbcSchemaConfig schema;

  // Required for ServiceLoader
  public CbcFormatter() {
    super();
    this.schema = null;
  }

  // Concrete instance used by getInstance(SchemaConfig)
  CbcFormatter(CbcSchemaConfig schema) {
    super();
    this.schema = schema;
  }

  public String getName() {
    return "CBC";
  }

  @Override
  public Map<String, Object> getFormat() {
    if (schema == null || schema.getFieldSpecificationList() == null) {
      return Map.of();
    }
    Map<String, Object> out = new LinkedHashMap<>();
    for (io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f : schema.getFieldSpecificationList()) {
      out.put(f.getName(), toFieldMap(f));
    }
    return out;
  }

  private static Map<String, Object> toFieldMap(io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f) {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("type", f.getType());
    if (f.getSize() != null) m.put("size", f.getSize());
    if (Boolean.TRUE.equals(f.getOptional())) m.put("optional", true);
    if (!Boolean.TRUE.equals(f.getFixed())) m.put("fixed", false);
    if (f.getDescription() != null) m.put("description", f.getDescription());
    if (f.getDecalc() != null) m.put("decalc", f.getDecalc());
    if (f.getEncalc() != null) m.put("encalc", f.getEncalc());
    if (f.getMin() != null) m.put("min", f.getMin());
    if (f.getMax() != null) m.put("max", f.getMax());

    if (f.getEnumTable() != null && !f.getEnumTable().isEmpty()) {
      Map<String, String> e = new LinkedHashMap<>();
      f.getEnumTable().entrySet().stream()
          .sorted(Map.Entry.comparingByKey())
          .forEach(en -> e.put(String.valueOf(en.getKey()), en.getValue()));
      m.put("enum", e);
    }

    if (f.getFields() != null && !f.getFields().isEmpty()) {
      List<Map<String, Object>> children = new ArrayList<>();
      for (io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification c : f.getFields()) {
        Map<String, Object> child = new LinkedHashMap<>();
        child.put("name", c.getName());
        child.putAll(toFieldMap(c));
        children.add(child);
      }
      m.put("fields", children);
    }
    return m;
  }
  @Override
  public ParsedObject parse(byte[] payload) {
    try {
      Map<String, Object> map = decode(payload);
      ParsedObject parsed = new MapResolver(map);
      return new StructuredResolver(parsed, map);
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      return new DefaultParser(payload);
    }
  }

  @Override
  public JsonObject parseToJson(byte[] payload) throws IOException {
    if (schema == null) {
      throw new IllegalStateException("CBC SchemaConfig not set on formatter");
    }
    Map<String, Object> map = decode(payload);
    return new Gson().toJsonTree(map).getAsJsonObject();
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    if (!(config instanceof CbcSchemaConfig c)) {
      throw new IllegalArgumentException("Expected CbcSchemaConfig");
    }
    return new CbcFormatter(c);
  }

  @SuppressWarnings("unchecked")
  public byte[] toBytes(Object data) {
    if (schema == null) {
      throw new IllegalStateException("CBC SchemaConfig not set on formatter");
    }
    final Map<String, Object> values;
    if (data instanceof Map<?, ?> m) {
      values = (Map<String, Object>) m;
    } else if (data instanceof JsonObject json) {
      values = new Gson().fromJson(json, Map.class);
    } else {
      throw new IllegalArgumentException("Unsupported data type for CBC encode: " + data);
    }
    return encode(values);
  }

  // ------------------------ Decode ------------------------
  private Map<String, Object> decode(byte[] data) {
    if (schema == null) throw new IllegalStateException("CBC SchemaConfig not set on formatter");

    BitReader cursor = new BitReader(data);
    Map<String, Object> out = new LinkedHashMap<>();

    // header
    if (schema.getMessageKey() > 0) {
      out.put("messageKey", cursor.readUnsigned(16));
    }

    for (io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f : schema.getFieldSpecificationList()) {
      Object v = readField(cursor, f);
      if (v != null) out.put(f.getName(), v);
    }

    // CRC handling deferred until placement defined in schema
    return out;
  }

  private Object readField(BitReader c, io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f) {
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
        return applyDecalcUint(raw, f);
      }
      case "int" -> {
        long raw = c.readSigned(reqSizeBits(f));
        return applyDecalcInt(raw, f);
      }
      case "string" -> {
        return readString(f, c);
      }
      case "data" -> {
        return readData(f, c);
      }
      default -> throw new IllegalArgumentException("Unsupported CBC type in decode: " + f.getType());
    }
  }

  private Map<String, Object> readStruct(FieldSpecification f, BitReader c) {
    Map<String, Object> m = new LinkedHashMap<>();
    if (f.getFields() != null) {
      for (io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification child : f.getFields()) {
        Object cv = readField(c, child);
        if (cv != null) m.put(child.getName(), cv);
      }
    }
    return m;
  }

  private byte[] readData(FieldSpecification f, BitReader c) {
    if (!Boolean.TRUE.equals(f.getFixed())) {
      throw new IllegalArgumentException("data with fixed=false not supported without length semantics");
    }
    int n = reqSizeBits(f) / 8;
    return c.readBytes(n);
  }

  private String readString(FieldSpecification f, BitReader c) {
    boolean fixed = Boolean.TRUE.equals(f.getFixed());
    int size = f.getSize();
    if (fixed) {
      byte[] data = c.readBytes(size);
      int end = size;
      while (end > 0 && data[end - 1] == 0) end--;
      return new String(data, 0, end, StandardCharsets.US_ASCII);
    } else {
      int len = (int) c.readUnsigned(8);
      byte[] data = c.readBytes(len);
      return new String(data, StandardCharsets.US_ASCII);
    }
  }

  private static int reqSizeBits(io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f) {
    Integer s = f.getSize();
    if (s == null) throw new IllegalStateException("Field '" + f.getName() + "' requires size");
    return s;
  }

  private Number applyDecalcUint(long raw, io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f) {
    return applyDecalcInt(raw, f);
  }

  private Number applyDecalcInt(long raw, io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f) {
    String expr = f.getDecalc();
    if (expr == null) return raw;
    double d = raw;
    Double out = evalDecalc(expr, d);
    long li = (long) out.doubleValue();
    return (out == li) ? li : out;
  }

  private static Double evalDecalc(String expr, double v) {
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

  // ------------------------ Encode ------------------------
  @SuppressWarnings("unchecked")
  private byte[] encode(Map<String, Object> fieldValues) {
    BitWriter w = new BitWriter();

    if (schema.getMessageKey() > 0) {
      w.writeUnsigned(schema.getMessageKey(), 16);
    }

    for (io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f : schema.getFieldSpecificationList()) {
      writeField(w, f, fieldValues.get(f.getName()));
    }

    // CRC append deferred until placement defined in schema
    return w.toByteArray();
  }

  @SuppressWarnings("unchecked")
  private void writeField(BitWriter w, io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f, Object value) {
    boolean present = value != null;

    if (Boolean.TRUE.equals(f.getOptional())) {
      w.writeUnsigned(present ? 1 : 0, 1);
      if (!present) return;
    }

    String t = f.getType().toLowerCase();
    switch (t) {
      case "struct" -> writeStruct(f, w, value);
      case "uint", "bitmask" -> {
        long raw = toUintRaw(value, f);
        w.writeUnsigned(raw, reqSizeBits(f));
      }
      case "int" -> {
        long raw = toIntRaw(value, f);
        w.writeSigned(raw, reqSizeBits(f));
      }
      case "string" -> writeString(f, w, value);
      case "data" -> writeData(f, w, value);
      default -> throw new IllegalArgumentException("Unsupported CBC type in encode: " + f.getType());
    }
  }

  private void writeStruct(FieldSpecification f, BitWriter w, Object value) {
    if (!(value instanceof Map)) {
      if (value == null) return; // already handled as optional
      throw new IllegalArgumentException("Struct '" + f.getName() + "' expects Map value");
    }
    Map<String, Object> map = (Map<String, Object>) value;
    if (f.getFields() != null) {
      for (FieldSpecification child : f.getFields()) {
        writeField(w, child, map.get(child.getName()));
      }
    }
  }


  private void writeString(FieldSpecification f, BitWriter w, Object value) {
    boolean fixed = Boolean.TRUE.equals(f.getFixed());
    int size = f.getSize();
    String s = Objects.toString(value, "");
    byte[] ascii = s.getBytes(StandardCharsets.US_ASCII);
    if (fixed) {
      byte[] out = new byte[size];
      System.arraycopy(ascii, 0, out, 0, Math.min(ascii.length, size));
      w.writeRawBytes(out);
    } else {
      int len = Math.min(ascii.length, size);
      w.writeUnsigned(len, 8);           // length prefix
      for (int i = 0; i < len; i++) {
        w.writeUnsigned(ascii[i] & 0x7F, 8);
      }
    }
  }

  private void writeData(FieldSpecification f, BitWriter w, Object value) {
    int n = reqSizeBits(f) / 8;
    if (!Boolean.TRUE.equals(f.getFixed())) {
      throw new IllegalArgumentException("data with fixed=false not supported without length semantics");
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

  private static double evalEncalc(String expr, long v) {
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
    throw new IllegalArgumentException("Unsupported encalc expression: " + expr);
  }

  private static double evalEncalc(String expr, double v) {
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
    throw new IllegalArgumentException("Unsupported encalc expression: " + expr);
  }

}
