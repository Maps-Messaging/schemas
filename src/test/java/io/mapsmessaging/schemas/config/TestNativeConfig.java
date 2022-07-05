package io.mapsmessaging.schemas.config;

import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig.TYPE;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Assertions;

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
}