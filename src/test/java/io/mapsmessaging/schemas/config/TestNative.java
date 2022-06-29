package io.mapsmessaging.schemas.config;

import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig.TYPE;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Assertions;

public class TestNative extends GeneralBaseTest {

  Map<String, Object> getProperties(){
    Map<String, Object> props = new LinkedHashMap<>();
    props.put("format", "Native");
    props.put("type", TYPE.DOUBLE.toString());
    return props;
  }

  @Override
  void validate(SchemaConfig schemaConfig) {
    Assertions.assertTrue(schemaConfig instanceof NativeSchemaConfig);
    NativeSchemaConfig config = (NativeSchemaConfig) schemaConfig;
    Assertions.assertEquals(TYPE.DOUBLE, config.getType());
  }
}