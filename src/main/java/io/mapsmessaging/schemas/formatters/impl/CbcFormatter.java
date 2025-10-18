/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2025 ] MapsMessaging B.V.
 *
 *  Licensed under the Apache License, Version 2.0 with the Commons Clause
 *  (the "License"); you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *      https://commonsclause.com/
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package io.mapsmessaging.schemas.formatters.impl;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CbcSchemaConfig;
import io.mapsmessaging.schemas.config.impl.cbc.BitCursor;
import io.mapsmessaging.schemas.config.impl.cbc.CrcType;
import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.impl.cbc.PresenceEvaluator;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

public class CbcFormatter extends MessageFormatter {

  private final CbcSchemaConfig schema;

  // Required for ServiceLoader
  public CbcFormatter() {
    super();
    this.schema = null;
  }

  // Concrete instance used by getInstance(SchemaConfig)
  CbcFormatter(CbcSchemaConfig schema) {
    super();
    this.schema = schema;
  }

  public String getName() {
    return "CBC";
  }

  @Override
  public Map<String, Object> getFormat() {
    if (schema == null || schema.getFieldSpecificationList() == null) {
      return Map.of();
    }
    Map<String, Object> format = new LinkedHashMap<>();
    for (FieldSpecification f : schema.getFieldSpecificationList()) {
      Map<String, Object> fieldInfo = new LinkedHashMap<>();
      fieldInfo.put("primitiveType", f.getPrimitiveType().name());
      fieldInfo.put("bitWidth", f.getBitWidth());
      fieldInfo.put("signed", f.isSigned());
      if (f.getScale() != 1.0d) fieldInfo.put("scale", f.getScale());
      if (f.getOffset() != 0.0d) fieldInfo.put("offset", f.getOffset());
      if (f.getUnit() != null) fieldInfo.put("unit", f.getUnit());
      if (f.getPresenceCondition() != null) fieldInfo.put("presence", f.getPresenceCondition());
      if (f.isByteAlignAfter()) fieldInfo.put("byteAlignAfter", true);
      format.put(f.getFieldName(), fieldInfo);
    }
    return format;
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    try {
      Map<String, Object> map = decode(payload);
      ParsedObject parsed = new MapResolver(map);
      return new StructuredResolver(parsed, map);
    } catch (Exception e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      return new DefaultParser(payload);
    }
  }

  @Override
  public JsonObject parseToJson(byte[] payload) throws IOException {
    if (schema == null) {
      throw new IllegalStateException("CBC SchemaConfig not set on formatter");
    }
    Map<String, Object> map = decode(payload);
    return new Gson().toJsonTree(map).getAsJsonObject();
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    if (!(config instanceof CbcSchemaConfig c)) {
      throw new IllegalArgumentException("Expected CbcSchemaConfig");
    }
    return new CbcFormatter(c);
  }

  private Map<String, Object> decode(byte[] data) {
    if (schema == null) {
      throw new IllegalStateException("CBC SchemaConfig not set on formatter");
    }

    BitCursor cursor = new BitCursor(data, schema.isLittleEndian());
    Map<String, Object> out = new LinkedHashMap<>();

    if (schema.getMessageTypeId() > 0) {
      out.put("messageTypeId", cursor.readUnsigned(16));
    }

    for (FieldSpecification f : schema.getFieldSpecificationList()) {
      if (!PresenceEvaluator.isPresent(f, out)) {
        continue;
      }

      Object value;
      int bits = f.getBitWidth();
      boolean signed = f.isSigned();

      switch (f.getPrimitiveType()) {
        case BOOLEAN: {
          value = cursor.readUnsigned(1) != 0L;
          break;
        }
        case UNSIGNED_INTEGER: {
          long v = cursor.readUnsigned(bits);
          value = applyScale(v, f);
          break;
        }
        case SIGNED_INTEGER: {
          long v = signed ? cursor.readSigned(bits) : cursor.readUnsigned(bits);
          value = applyScale(v, f);
          break;
        }
        case FLOAT_FIXED: {
          long raw = signed ? cursor.readSigned(bits) : cursor.readUnsigned(bits);
          value = (raw * f.getScale()) + f.getOffset();
          break;
        }
        case BYTES: {
          int byteCount = bits / 8;
          value = cursor.readBytes(byteCount);
          break;
        }
        case RESERVED: {
          cursor.skip(bits);
          value = null;
          break;
        }
        default:
          throw new IllegalArgumentException("Unsupported primitive type: " + f.getPrimitiveType());
      }

      if (value != null) {
        out.put(f.getFieldName(), value);
      }
      if (f.isByteAlignAfter()) {
        cursor.alignToNextByte();
      }
    }

    if (schema.isIncludeHeaderChecksum() && schema.getChecksumType() != CrcType.NONE) {
      // Placeholder: validation can be added if checksum position is specified in schema
      // Crc.computeChecksum(data, schema.getChecksumType());
    }

    return out;
  }

  private Object applyScale(long v, FieldSpecification f) {
    if (f.getScale() != 1.0d || f.getOffset() != 0.0d) {
      return (v * f.getScale()) + f.getOffset();
    }
    return v;
  }
}
