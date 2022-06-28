package io.mapsmessaging.schemas.formatters.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig.TYPE;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

public class NativeFormatter implements MessageFormatter {

  private final NativeEncoderDecoder encoderDecoder;

  public NativeFormatter() {
    encoderDecoder = null;
  }

  public NativeFormatter(TYPE type) {
    switch(type){
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
      default:
        encoderDecoder = null;
        break;
    }
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    return new ParsedObject() {
      @Override
      public Object getReferenced() {
        return encoderDecoder != null ? encoderDecoder.decode(payload) : null;
      }

      @Override
      public Object get(String s) {
        return payload;
      }
    };
  }

  @Override
  public JSONObject parseToJson(byte[] payload) throws IOException {
    return (JSONObject) parse(payload);
  }

  @Override
  public byte[] pack(Object object) throws IOException {
    if (encoderDecoder != null) {
      return encoderDecoder.encode(object);
    }
    return new byte[0];
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

  private static long readFromByteArray(byte[] payload, int size){
    long val =0;
    int x=0;
    while(x< payload.length && x < size){
      val = val << 8;
      val = val | payload[x];
      x++;
    }
    return val;
  }

  private static void writeToByteArray(long val, byte[] payload, int size){
    int x=0;
    while(x< payload.length && x < size){
      payload[x] = (byte) (val & 0xff);
      val = val >> 8;
      x++;
    }
  }

  interface NativeEncoderDecoder{
    Object decode(byte[] payload);
    byte[] encode(Object obj);
  }

  static class StringEncoderDecoder implements NativeEncoderDecoder{
    @Override
    public Object decode(byte[] payload) {
      return new String(payload);
    }

    @Override
    public byte[] encode(Object obj) {
      return ((String)obj).getBytes(StandardCharsets.UTF_8);
    }
  }

  static class StringNumericEncoderDecoder implements NativeEncoderDecoder{
    @Override
    public Object decode(byte[] payload) {
      String val = new String(payload).trim();
      if(val.contains(".")){
        return Double.parseDouble(val);
      }
      return Long.parseLong(val);
    }

    @Override
    public byte[] encode(Object obj) {
      return ((String)obj).getBytes(StandardCharsets.UTF_8);
    }
  }

  static class IntEncoderDecoder implements NativeEncoderDecoder{

    private final int size;

    public IntEncoderDecoder(int size){
      this.size = size;
    }

    @Override
    public Object decode(byte[] payload) {
      return readFromByteArray(payload, size);
    }

    @Override
    public byte[] encode(Object obj) {
      byte[] tmp = new byte[size];
      writeToByteArray( (Long)obj, tmp, size);
      return tmp;
    }
  }

  static class FloatEncoderDecoder implements NativeEncoderDecoder{
    @Override
    public Object decode(byte[] payload) {
      long val = readFromByteArray(payload, 4);
      return Float.intBitsToFloat((int)val);
    }

    @Override
    public byte[] encode(Object obj) {
      byte[] tmp = new byte[4];
      long val = Float.floatToIntBits((Float)obj);
      writeToByteArray( val, tmp, 4);
      return tmp;
    }
  }
  static class DoubleEncoderDecoder implements NativeEncoderDecoder{
    @Override
    public Object decode(byte[] payload) {
      long val = readFromByteArray(payload, 8);
      return Double.longBitsToDouble(val);
    }

    @Override
    public byte[] encode(Object obj) {
      byte[] tmp = new byte[8];
      long val = Double.doubleToLongBits((Double)obj);
      writeToByteArray( val, tmp, 8);
      return tmp;
    }
  }


}
