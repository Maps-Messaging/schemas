package io.mapsmessaging.schemas.config.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public class NativeSchemaConfig extends SimpleSchemaConfig {

  private static String NAME = "Native";

  public enum TYPE {STRING, NUMERIC_STRING, INT8, INT16, INT32, INT64, FLOAT, DOUBLE }

  @Getter
  @Setter
  private TYPE type;

  public NativeSchemaConfig() {
    super(NAME);
  }

  protected NativeSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
    String typeName = config.get("type").toString();
    switch (typeName.toUpperCase()){
      case "STRING":
        type  = TYPE.STRING;
        break;
      case "NUMERIC_STRING":
        type  = TYPE.NUMERIC_STRING;
        break;
      case "INT8":
        type  = TYPE.INT8;
        break;
      case "INT16":
        type  = TYPE.INT16;
        break;
      case "INT32":
        type  = TYPE.INT32;
        break;
      case "INT64":
        type  = TYPE.INT64;
        break;
      case "FLOAT":
        type  = TYPE.FLOAT;
        break;
      case "DOUBLE":
        type  = TYPE.DOUBLE;
        break;

      default:
        type = TYPE.STRING;

    }
  }

  @Override
  protected JSONObject packData() throws IOException {
    if(type == null){
      throw new IOException("No type defined specified");
    }
    JSONObject data = new JSONObject();
    packData(data);
    data.put("type", type.toString());
    return data;
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new NativeSchemaConfig(config);
  }
}

