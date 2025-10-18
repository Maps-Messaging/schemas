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

package io.mapsmessaging.schemas.formatters;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.impl.CbcSchemaConfig;
import io.mapsmessaging.schemas.config.impl.cbc.*;
import io.mapsmessaging.schemas.formatters.impl.CbcFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;


class TestCbcFormatter {

  private static final int STRING_BYTES = 32;      // fixed-width for stringId
  private static final double FLOAT_SCALE = 1e6;  // encode float as fixed-point
  private static final double DOUBLE_SCALE = 1e8;  // encode double as fixed-point

  private CbcSchemaConfig buildSchema() {
    CbcSchemaConfig cfg = new CbcSchemaConfig();
    cfg.setMimeType("application/x-cbc");
    cfg.setLittleEndian(true);
    cfg.setIncludeHeaderChecksum(false);
    cfg.setChecksumType(CrcType.NONE);
    cfg.setMessageTypeId(0);

    List<FieldSpecification> fields = new ArrayList<>();

    fields.add(CbcField.of("stringLen", PrimitiveType.UNSIGNED_INTEGER, 8, false));
    fields.add(CbcField.bytes("stringId", STRING_BYTES));

    fields.add(CbcField.of("longId", PrimitiveType.SIGNED_INTEGER, 64, true));
    fields.add(CbcField.of("intId", PrimitiveType.SIGNED_INTEGER, 32, true));

    fields.add(CbcField.fixed("floatId", 32, true, 1.0 / FLOAT_SCALE, 0.0));
    fields.add(CbcField.fixed("doubleId", 64, true, 1.0 / DOUBLE_SCALE, 0.0));

    cfg.setFieldSpecificationList(fields);
    return cfg;
  }

  private byte[] encode(Map<String, Object> values) {
    BitWriter w = new BitWriter(true);

    String str = Objects.toString(values.get("stringId"), "");
    byte[] raw = str.getBytes(StandardCharsets.UTF_8);
    int len = Math.min(raw.length, STRING_BYTES);

    w.writeUnsigned(len, 8);
    byte[] padded = new byte[STRING_BYTES];
    System.arraycopy(raw, 0, padded, 0, len);
    w.writeRawBytes(padded);

    long longId = ((Number) values.get("longId")).longValue();
    int intId = ((Number) values.get("intId")).intValue();

    double f32 = ((Number) values.get("floatId")).doubleValue();
    double f64 = ((Number) values.get("doubleId")).doubleValue();

    w.writeSigned(longId, 64);
    w.writeSigned(intId, 32);

    long f32fixed = Math.round(f32 * FLOAT_SCALE);
    long f64fixed = Math.round(f64 * DOUBLE_SCALE);

    w.writeSigned(f32fixed, 32);
    w.writeSigned(f64fixed, 64);

    return w.toByteArray();
  }

  private Map<String, Object> sample() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("stringId", "alpha-βeta-γamma-Δ"); // will be truncated/padded to 32 bytes
    m.put("longId", 1234567890123456789L);
    m.put("intId", -123456789);
    m.put("floatId", 1234.567891f);
    m.put("doubleId", -9876543210.123456789d);
    return m;
  }

  @Test
  void roundTrip_parse_and_parseToJson() throws Exception {
    CbcSchemaConfig schema = buildSchema();
    CbcFormatter formatter = (CbcFormatter) new CbcFormatter().getInstance(schema);

    Map<String, Object> src = sample();
    byte[] payload = encode(src);

    // ParsedObject
    ParsedObject parsed = formatter.parse(payload);
    // stringId comes back as raw bytes; convert using the recorded length
    int len = ((Number) parsed.get("stringLen")).intValue();
    byte[] sid = (byte[]) parsed.get("stringId");
    String str = new String(sid, 0, Math.min(len, STRING_BYTES), StandardCharsets.UTF_8);

    Assertions.assertEquals(truncate(src.get("stringId").toString(), STRING_BYTES), str);
    Assertions.assertEquals(((Number) src.get("longId")).longValue(), ((Number) parsed.get("longId")).longValue());
    Assertions.assertEquals(((Number) src.get("intId")).intValue(), ((Number) parsed.get("intId")).intValue());

    // fixed-point scaled back in formatter: compare with tolerance
    Assertions.assertEquals(((Number) src.get("floatId")).doubleValue(),
        ((Number) parsed.get("floatId")).doubleValue(), 1e-4);
    Assertions.assertEquals(((Number) src.get("doubleId")).doubleValue(),
        ((Number) parsed.get("doubleId")).doubleValue(), 1e-4);

    // JSON view
    JsonObject json = formatter.parseToJson(payload);
    JsonArray arr = json.getAsJsonArray("stringId");
    byte[] buf = new byte[len];
    int idx = 0;
    for (int x = 0; x < len; x++) {
      JsonElement je = arr.get(idx);
      buf[idx] = je.getAsByte();
      idx++;
    }
    Assertions.assertEquals(src.get("stringId").toString(), new String(buf));
    Assertions.assertEquals(((Number) src.get("longId")).longValue(), json.get("longId").getAsLong());
    Assertions.assertEquals(((Number) src.get("intId")).intValue(), json.get("intId").getAsInt());
    Assertions.assertEquals(((Number) src.get("floatId")).doubleValue(), json.get("floatId").getAsDouble(), 1e-6);
    Assertions.assertEquals(((Number) src.get("doubleId")).doubleValue(), json.get("doubleId").getAsDouble(), 1e-9);
  }

  @Test
  void format_map_describes_fields() throws IOException {
    CbcSchemaConfig schema = buildSchema();
    CbcFormatter formatter = (CbcFormatter) new CbcFormatter().getInstance(schema);
    Map<String, Object> fmt = formatter.getFormat();

    Assertions.assertTrue(fmt.containsKey("stringLen"));
    Assertions.assertTrue(fmt.containsKey("stringId"));
    Assertions.assertTrue(fmt.containsKey("longId"));
    Assertions.assertTrue(fmt.containsKey("intId"));
    Assertions.assertTrue(fmt.containsKey("floatId"));
    Assertions.assertTrue(fmt.containsKey("doubleId"));
  }

  private static String truncate(String s, int maxBytes) {
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    int n = Math.min(b.length, maxBytes);
    return new String(b, 0, n, StandardCharsets.UTF_8);
  }
}
