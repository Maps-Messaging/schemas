package io.mapsmessaging.schemas.config;

import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig.TYPE;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TestNativeConfig extends GeneralBaseTest {

  Map<String, Object> getProperties() {
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "Native");
    props.put("type", TYPE.DOUBLE.toString());
    return props;
  }

  @Override
  SchemaConfig buildConfig() {
    NativeSchemaConfig config = new NativeSchemaConfig();
    config.setType(TYPE.DOUBLE);
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(LocalDateTime.now().plusDays(10));
    config.setNotBefore(LocalDateTime.now().minusDays(10));
    return config;
  }

  @Override
  void validate(SchemaConfig schemaConfig) {
    Assertions.assertTrue(schemaConfig instanceof NativeSchemaConfig);
    NativeSchemaConfig config = (NativeSchemaConfig) schemaConfig;
    Assertions.assertEquals(TYPE.DOUBLE, config.getType());
  }


  @Test
  void invalidConfig() {
    NativeSchemaConfig config = new NativeSchemaConfig();
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(LocalDateTime.now().plusDays(10));
    config.setNotBefore(LocalDateTime.now().minusDays(10));
    Assertions.assertThrowsExactly(IOException.class, () -> config.pack());
  }

  @Test
  void checkAllTypes() throws IOException {
    for (TYPE type : NativeSchemaConfig.TYPE.values()) {
      NativeSchemaConfig config = new NativeSchemaConfig();
      config.setType(type);
      config.setUniqueId(UUID.randomUUID());
      config.setExpiresAfter(LocalDateTime.now().plusDays(10));
      config.setNotBefore(LocalDateTime.now().minusDays(10));
      JSONObject jsonObject = new JSONObject(config.pack());
      Assertions.assertEquals(type.toString(), jsonObject.getJSONObject("schema").getString("type"));
    }

    for (TYPE type : NativeSchemaConfig.TYPE.values()) {

      Map<String, Object> props = new LinkedHashMap<>();
      props.put("format", "Native");
      props.put("type", type.toString());
      props.put("uuid", UUID.randomUUID());
      Map<String, Object> schema = new LinkedHashMap<>();
      schema.put("schema", props);

      SchemaConfig config = SchemaConfigFactory.getInstance().constructConfig(schema);
      JSONObject jsonObject = new JSONObject(config.pack());
      Assertions.assertEquals(type.toString(), jsonObject.getJSONObject("schema").getString("type"));
    }
  }
}