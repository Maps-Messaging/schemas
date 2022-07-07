package io.mapsmessaging.schemas.formatters.impl;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig.TYPE;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import java.io.IOException;
import org.json.JSONObject;

public class NativeFormatter extends MessageFormatter {

  private final NativeEncoderDecoder encoderDecoder;
  private final TYPE type;

  public NativeFormatter() {
    encoderDecoder = null;
    type = null;
  }

  public NativeFormatter(TYPE type) throws IOException {
    this.type = type;
    if (type == null) {
      throw new IOException("Invalid type specified");
    }
    switch (type) {
      default:
      case STRING:
        encoderDecoder = new StringEncoderDecoder();
        break;

      case NUMERIC_STRING:
        encoderDecoder = new StringNumericEncoderDecoder();
        break;

      case INT64:
        encoderDecoder = new IntEncoderDecoder(8);
        break;

      case INT32:
        encoderDecoder = new IntEncoderDecoder(4);
        break;

      case INT16:
        encoderDecoder = new IntEncoderDecoder(2);
        break;

      case INT8:
        encoderDecoder = new IntEncoderDecoder(1);
        break;

      case FLOAT:
        encoderDecoder = new FloatEncoderDecoder();
        break;

      case DOUBLE:
        encoderDecoder = new DoubleEncoderDecoder();
        break;
    }
  }

  private static long readFromByteArray(byte[] payload, int size) {
    long val = 0;
    int x = 0;
    while (x < payload.length && x < size) {
      long t = (payload[x] & 0xff);
      t = t << (x * 8);
      val = val | t;
      x++;
    }
    return val;
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    return new ParsedObject() {
      @Override
      public Object getReferenced() {
        return payload;
      }

      @Override
      public Object get(String s) {
        try {
          return encoderDecoder != null ? encoderDecoder.decode(payload) : null;
        } catch (Exception e) {
          logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
          return null;
        }
      }

    };
  }

  @Override
  public JSONObject parseToJson(byte[] payload) throws IOException {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put("value", parse(payload).get("value"));
    jsonObject.put("type", type);
    return jsonObject;
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    NativeSchemaConfig schemaConfig = (NativeSchemaConfig) config;
    return new NativeFormatter(schemaConfig.getType());
  }

  @Override
  public String getName() {
    return "Native";
  }

  interface NativeEncoderDecoder {

    Object decode(byte[] payload);
  }

  static class StringEncoderDecoder implements NativeEncoderDecoder {

    @Override
    public Object decode(byte[] payload) {
      return new String(payload);
    }

  }

  static class StringNumericEncoderDecoder implements NativeEncoderDecoder {

    @Override
    public Object decode(byte[] payload) {
      String val = new String(payload).trim();
      if (val.equals("NaN")) {
        return Double.NaN;
      }
      if (val.contains(".")) {
        return Double.parseDouble(val);
      }
      return Long.parseLong(val);
    }
  }

  static class IntEncoderDecoder implements NativeEncoderDecoder {

    private final int size;

    public IntEncoderDecoder(int size) {
      this.size = size;
    }

    @Override
    public Object decode(byte[] payload) {
      long result = readFromByteArray(payload, size);
      switch (size) {
        case 8:
          return result;
        case 4:
          return (int) result;

        case 2:
          return (short) result;

        case 1:
          return (byte) result;

        default:
          return result;
      }
    }
  }

  static class FloatEncoderDecoder implements NativeEncoderDecoder {

    @Override
    public Object decode(byte[] payload) {
      long val = readFromByteArray(payload, 4);
      return Float.intBitsToFloat((int) val);
    }
  }

  static class DoubleEncoderDecoder implements NativeEncoderDecoder {

    @Override
    public Object decode(byte[] payload) {
      long val = readFromByteArray(payload, 8);
      return Double.longBitsToDouble(val);
    }
  }
}
