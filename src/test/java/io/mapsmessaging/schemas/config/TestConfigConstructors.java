package io.mapsmessaging.schemas.config;

import io.mapsmessaging.schemas.config.impl.RawSchemaConfig;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestConfigConstructors {

  @Test
  void validSchemaLoad() throws IOException {
    SchemaConfig good = new RawSchemaConfig();
    good.setUniqueId(UUID.randomUUID());
    JSONObject schema = new JSONObject();
    schema.put("schema", good.packData());
    String config = schema.toString(2);

    Assertions.assertNotNull(SchemaConfigFactory.getInstance().constructConfig(config));
    Assertions.assertNotNull(SchemaConfigFactory.getInstance().constructConfig(config.getBytes()));
    Assertions.assertNotNull(SchemaConfigFactory.getInstance().constructConfig(schema.toMap()));
  }


  @Test
  void invalidSchemaLoad() throws IOException {
    SchemaConfig bad = new BadSchema();
    JSONObject schema = new JSONObject();
    schema.put("schema", bad.packData());
    String config = schema.toString(2);
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(config));
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(config.getBytes()));
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(schema.toMap()));
  }

  @Test
  void invalidDataLoad() {
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(new JSONObject().toString()));

    JSONObject schema = new JSONObject();
    schema.put("schema", 2);
    String config = schema.toString(2);
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(config));
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(schema.toMap()));

    JSONObject schema1 = new JSONObject();
    schema1.put("schema", new JSONObject());
    String config1 = schema1.toString(2);
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(config1));
    Assertions.assertThrowsExactly(IOException.class, () -> SchemaConfigFactory.getInstance().constructConfig(schema1.toMap()));

  }


  static class BadSchema extends SchemaConfig {

    protected BadSchema() {
      super("BAD");
      uniqueId = UUID.randomUUID();
    }

    protected BadSchema(String format, Map<String, Object> config) {
      super(format, config);
    }

    @Override
    protected JSONObject packData() throws IOException {
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
