package io.mapsmessaging.schemas.config.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;

public class NativeSchemaConfig extends SimpleSchemaConfig {

  public enum TYPE {STRING, NUMERIC_STRING, INT8, INT16, INT32, INT64, FLOAT, DOUBLE }

  @Getter
  @Setter
  private TYPE type;

  public NativeSchemaConfig() {
    super("Native");
  }

  protected NativeSchemaConfig(TYPE type) {
    super("Native");
    this.type = type;
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
    String typeName = config.get("type").toString();
    TYPE typeLoad;
    switch (typeName.toUpperCase()){
      case "STRING":
        typeLoad  = TYPE.STRING;
        break;
      case "NUMERIC_STRING":
        typeLoad  = TYPE.NUMERIC_STRING;
        break;
      case "INT8":
        typeLoad  = TYPE.INT8;
        break;
      case "INT16":
        typeLoad  = TYPE.INT16;
        break;
      case "INT32":
        typeLoad  = TYPE.INT32;
        break;
      case "INT64":
        typeLoad  = TYPE.INT64;
        break;
      case "FLOAT":
        typeLoad  = TYPE.FLOAT;
        break;
      case "DOUBLE":
        typeLoad  = TYPE.DOUBLE;
        break;

      default:
        typeLoad = TYPE.STRING;

    }
    return new NativeSchemaConfig(typeLoad);
  }
}

