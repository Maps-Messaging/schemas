package io.mapsmessaging.schemas.formatters;

import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.impl.CbcSchemaConfig;
import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;
import io.mapsmessaging.schemas.formatters.impl.CbcFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class TestCbcFormatter {

  private static final int STRING_BYTES = 32;   // fixed-width for stringId
  private static final double FLOAT_SCALE = 1e6;  // encode float as fixed-point
  private static final double DOUBLE_SCALE = 1e8; // encode double as fixed-point

  private CbcSchemaConfig buildSchema() {
    CbcSchemaConfig cfg = new CbcSchemaConfig();
    cfg.setMimeType("application/x-cbc");
    cfg.setMessageKey(0);

    List<FieldSpecification> fields = new ArrayList<>();

    // fixed-length string
    fields.add(FieldSpecification.builder()
        .name("stringId")
        .type("string")
        .size(STRING_BYTES)
        .fixed(true)
        .build());

    // 64-bit and 32-bit signed ints
    fields.add(FieldSpecification.builder().name("longId").type("int").size(64).build());
    fields.add(FieldSpecification.builder().name("intId").type("int").size(32).build());

    // fixed-point ints with decalc/encalc
    fields.add(FieldSpecification.builder()
        .name("floatId").type("int").size(32)
        .decalc("v*" + (1.0 / FLOAT_SCALE))   // value = raw * 1/FLOAT_SCALE
        .encalc("v*" + (FLOAT_SCALE))         // raw   = value * FLOAT_SCALE
        .build());

    fields.add(FieldSpecification.builder()
        .name("doubleId").type("int").size(64)
        .decalc("v*" + (1.0 / DOUBLE_SCALE))
        .encalc("v*" + (DOUBLE_SCALE))
        .build());

    cfg.setFieldSpecificationList(fields);
    return cfg;
  }

  private Map<String, Object> sample() {
    Map<String, Object> m = new LinkedHashMap<>();
    m.put("stringId", "alpha-beta-yamma-d"); // will be padded/truncated to 32 bytes
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
    byte[] payload = formatter.toBytes(src);   // use formatter, not a hand-rolled BitWriter

    // ParsedObject view
    ParsedObject parsed = formatter.parse(payload);

    // stringId comes back as raw bytes of fixed size; trim trailing 0
    String trimmed = (String) parsed.get("stringId");
    Assertions.assertEquals(truncateUtf8(src.get("stringId").toString(), STRING_BYTES), trimmed);

    Assertions.assertEquals(((Number) src.get("longId")).longValue(), ((Number) parsed.get("longId")).longValue());
    Assertions.assertEquals(((Number) src.get("intId")).intValue(), ((Number) parsed.get("intId")).intValue());
    Assertions.assertEquals(((Number) src.get("floatId")).doubleValue(),
        ((Number) parsed.get("floatId")).doubleValue(), 1e-4);
    Assertions.assertEquals(((Number) src.get("doubleId")).doubleValue(),
        ((Number) parsed.get("doubleId")).doubleValue(), 1e-4);

    // JSON view (stringId as byte array)
    JsonObject json = formatter.parseToJson(payload);
    String jsonAsString = json.get("stringId").getAsString();

    Assertions.assertEquals(jsonAsString, src.get("stringId").toString());

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

    Assertions.assertTrue(fmt.containsKey("stringId"));
    Assertions.assertTrue(fmt.containsKey("longId"));
    Assertions.assertTrue(fmt.containsKey("intId"));
    Assertions.assertTrue(fmt.containsKey("floatId"));
    Assertions.assertTrue(fmt.containsKey("doubleId"));
  }

  private static String truncateUtf8(String s, int maxBytes) {
    byte[] b = s.getBytes(StandardCharsets.UTF_8);
    int n = Math.min(b.length, maxBytes);
    return new String(b, 0, n, StandardCharsets.UTF_8);
  }

}
