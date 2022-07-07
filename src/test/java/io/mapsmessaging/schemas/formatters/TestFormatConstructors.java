package io.mapsmessaging.schemas.formatters;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.RawSchemaConfig;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestFormatConstructors {

  @Test
  void validSchemaLoad() throws IOException {
    SchemaConfig good = new RawSchemaConfig();
    good.setUniqueId(UUID.randomUUID());
    Assertions.assertNotNull(MessageFormatterFactory.getInstance().getFormatter(good));
  }


  @Test
  void invalidSchemaLoad() {
    SchemaConfig bad = new BadSchema();
    bad.setUniqueId(UUID.randomUUID());
    Assertions.assertThrowsExactly(IOException.class, () -> MessageFormatterFactory.getInstance().getFormatter(bad));
  }

  static class BadSchema extends SchemaConfig {

    protected BadSchema() {
      super("BAD");
      uniqueId = UUID.randomUUID().toString();
    }

    protected BadSchema(String format, Map<String, Object> config) {
      super(format, config);
    }

    @Override
    protected JSONObject packData() {
      JSONObject data = new JSONObject();
      packData(data);
      return data;
    }

    @Override
    protected SchemaConfig getInstance(Map<String, Object> config) {
      return this;
    }
  }

}
