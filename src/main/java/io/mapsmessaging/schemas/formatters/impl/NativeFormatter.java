/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package io.mapsmessaging.schemas.formatters.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig.TYPE;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import org.json.JSONObject;

import java.io.IOException;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

/**
 * The type Native formatter.
 */
public class NativeFormatter extends MessageFormatter {

  private final NativeEncoderDecoder encoderDecoder;
  private final TYPE type;

  /**
   * Instantiates a new Native formatter.
   */
  public NativeFormatter() {
    encoderDecoder = null;
    type = null;
  }

  /**
   * Instantiates a new Native formatter.
   *
   * @param type the type
   * @throws IOException the io exception
   */
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

  /**
   * The interface Native encoder decoder.
   */
  interface NativeEncoderDecoder {

    /**
     * Decode object.
     *
     * @param payload the payload
     * @return the object
     */
    Object decode(byte[] payload);
  }

  /**
   * The type String encoder decoder.
   */
  static class StringEncoderDecoder implements NativeEncoderDecoder {

    @Override
    public Object decode(byte[] payload) {
      return new String(payload);
    }

  }

  /**
   * The type String numeric encoder decoder.
   */
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

  /**
   * The type Int encoder decoder.
   */
  static class IntEncoderDecoder implements NativeEncoderDecoder {

    private final int size;

    /**
     * Instantiates a new Int encoder decoder.
     *
     * @param size the size
     */
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

  /**
   * The type Float encoder decoder.
   */
  static class FloatEncoderDecoder implements NativeEncoderDecoder {

    @Override
    public Object decode(byte[] payload) {
      long val = readFromByteArray(payload, 4);
      return Float.intBitsToFloat((int) val);
    }
  }

  /**
   * The type Double encoder decoder.
   */
  static class DoubleEncoderDecoder implements NativeEncoderDecoder {

    @Override
    public Object decode(byte[] payload) {
      long val = readFromByteArray(payload, 8);
      return Double.longBitsToDouble(val);
    }
  }
}
