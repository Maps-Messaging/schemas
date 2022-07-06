package io.mapsmessaging.schemas.formatters;

import io.mapsmessaging.schemas.formatters.impl.RawFormatter;
import java.util.Base64;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestRawFormatter {

  @Test
  void testJSONpacker() {
    RawFormatter rawFormatter = new RawFormatter();
    JSONObject jsonObject = rawFormatter.parseToJson("Hi there".getBytes());
    Assertions.assertEquals("Hi there", new String(Base64.getDecoder().decode(jsonObject.getString("payload"))));
  }

  @Test
  void testNullResponse() {
    RawFormatter rawFormatter = new RawFormatter();
    Assertions.assertNull(rawFormatter.parse("what ever is here can not be parsed".getBytes()).get("whatEver"));
  }
}
