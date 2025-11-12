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

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.*;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.Map.Entry;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

/**
 * The type Proto buf formatter.
 */
public class ProtoBufFormatter extends MessageFormatter {

  private final String messageName;
  private final FileDescriptor descriptor;

  /**
   * Instantiates a new Proto buf formatter.
   */
  public ProtoBufFormatter() {
    messageName = "";
    descriptor = null;
  }

  /**
   * Instantiates a new Proto buf formatter.
   *
   * @param messageName     the message name
   * @param descriptorImage the descriptor image
   * @throws IOException the io exception
   */
  ProtoBufFormatter(String messageName, byte[] descriptorImage) throws IOException {
    try {
      this.descriptor = loadDescFile(descriptorImage);
      this.messageName = messageName;
    } catch (DescriptorValidationException e) {
      throw new IOException(e);
    }
  }

  public String getName() {
    return "ProtoBuf";
  }

  @Override
  public Map<String, Object> getFormat() {
    if (descriptor == null || messageName == null || messageName.isEmpty()) {
      return Map.of();
    }

    Descriptors.Descriptor messageDescriptor = descriptor.findMessageTypeByName(messageName);
    if (messageDescriptor == null) {
      return Map.of();
    }

    Map<String, Object> format = new LinkedHashMap<>();
    for (FieldDescriptor field : messageDescriptor.getFields()) {
      Map<String, Object> fieldInfo = new LinkedHashMap<>();
      fieldInfo.put("type", field.getType().name());
      fieldInfo.put("label", field.isRepeated() ? "repeated" : "optional");
      fieldInfo.put("number", field.getNumber());
      format.put(field.getName(), fieldInfo);
    }

    return format;
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    try {
      DynamicMessage message = DynamicMessage.parseFrom(descriptor.findMessageTypeByName(messageName), payload);
      ParsedObject parsed = new MapResolver(convertToMap(message));
      return new StructuredResolver(parsed, message);
    } catch (InvalidProtocolBufferException e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      return new DefaultParser(payload);
    }
  }

  @Override
  public JsonObject parseToJson(byte[] payload) {
    DynamicMessage dynamicMessage = (DynamicMessage) (parse(payload)).getReferenced();
    return convertToJson(dynamicMessage);
  }

  @Override
  public byte[] parseFromJson(JsonObject jsonObject) throws IOException {
    if (descriptor == null || messageName == null || messageName.isEmpty()) return new byte[0];

    Descriptors.Descriptor messageDescriptor = descriptor.findMessageTypeByName(messageName);
    if (messageDescriptor == null) return new byte[0];

    @SuppressWarnings("unchecked")
    Map<String, Object> map = gson.fromJson(jsonObject, Map.class);

    DynamicMessage.Builder builder = DynamicMessage.newBuilder(messageDescriptor);
    for (FieldDescriptor field : messageDescriptor.getFields()) {
      Object raw = map.get(field.getName());
      if (raw == null) continue;

      if (field.isRepeated() && raw instanceof Collection<?> coll) {
        for (Object element : coll) {
          builder.addRepeatedField(field, coerceForField(field, element));
        }
      } else {
        builder.setField(field, coerceForField(field, raw));
      }
    }
    return builder.build().toByteArray();
  }

  private Object normalizeForField(FieldDescriptor fd, Object v) {
    switch (fd.getJavaType()) {
      case BYTE_STRING -> {
        if (v instanceof com.google.protobuf.ByteString bs) return bs;
        if (v instanceof String s) {
          // accept standard or URL-safe, ignore whitespace
          String clean = s.replaceAll("\\s+", "");
          try {
            return com.google.protobuf.ByteString.copyFrom(java.util.Base64.getDecoder().decode(clean));
          } catch (IllegalArgumentException ignore) {
            return com.google.protobuf.ByteString.copyFrom(java.util.Base64.getUrlDecoder().decode(clean));
          }
        }
        if (v instanceof java.util.List<?> lst) {
          byte[] b = new byte[lst.size()];
          for (int i = 0; i < b.length; i++) b[i] = ((Number) lst.get(i)).byteValue();
          return com.google.protobuf.ByteString.copyFrom(b);
        }
        throw new IllegalArgumentException("Field '" + fd.getName() + "' expects bytes");
      }
      case ENUM -> {
        if (v instanceof Number n) return fd.getEnumType().findValueByNumber(n.intValue());
        String s = v.toString();
        // allow numeric-in-string too
        try {
          return fd.getEnumType().findValueByNumber(Integer.parseInt(s));
        } catch (NumberFormatException ignore) { /* fall through */ }
        Descriptors.EnumValueDescriptor ev = fd.getEnumType().findValueByName(s);
        if (ev == null) throw new IllegalArgumentException("Unknown enum: " + s + " for " + fd.getFullName());
        return ev;
      }
      case LONG -> {  // handle stringified 64-bit
        if (v instanceof Number n) return n.longValue();
        return Long.parseLong(v.toString());
      }
      case INT -> {
        if (v instanceof Number n) return n.intValue();
        return Integer.parseInt(v.toString());
      }
      case FLOAT -> {
        if (v instanceof Number n) return n.floatValue();
        return Float.parseFloat(v.toString());
      }
      case DOUBLE -> {
        if (v instanceof Number n) return n.doubleValue();
        return Double.parseDouble(v.toString());
      }
      case BOOLEAN -> {
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() != 0;
        return Boolean.parseBoolean(v.toString());
      }
      case STRING -> {
        return v.toString();
      }
      case MESSAGE -> {
        if (v instanceof Map<?, ?> m) {
          DynamicMessage.Builder child = DynamicMessage.newBuilder(fd.getMessageType());
          for (FieldDescriptor cf : fd.getMessageType().getFields()) {
            Object cv = m.get(cf.getName());
            if (cv == null) continue;
            if (cf.isRepeated() && cv instanceof Collection<?> coll) {
              for (Object e : coll) child.addRepeatedField(cf, normalizeForField(cf, e));
            } else {
              child.setField(cf, normalizeForField(cf, cv));
            }
          }
          return child.build();
        }
        throw new IllegalArgumentException("Field '" + fd.getName() + "' expects object for MESSAGE");
      }
      default -> {
        return v;
      }
    }
  }


  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    ProtoBufSchemaConfig protoBufSchemaConfig = (ProtoBufSchemaConfig) config;
    ProtoBufSchemaConfig.ProtobufConfig protobufConfig = protoBufSchemaConfig.getProtobufConfig();
    return new ProtoBufFormatter(protobufConfig.getMessageName(), protobufConfig.getDescriptorValue());
  }

  private FileDescriptor loadDescFile(byte[] descriptorImage) throws IOException, DescriptorValidationException {
    DescriptorProtos.FileDescriptorSet set;
    List<FileDescriptor> dependencyFileDescriptorList;
    try (InputStream fin = new ByteArrayInputStream(descriptorImage)) {
      set = DescriptorProtos.FileDescriptorSet.parseFrom(fin);
      dependencyFileDescriptorList = new ArrayList<>();
      for (int i = 0; i < set.getFileCount() - 1; i++) {
        dependencyFileDescriptorList.add(FileDescriptor.buildFrom(set.getFile(i), dependencyFileDescriptorList.toArray(new FileDescriptor[i])));
      }
    }
    return Descriptors.FileDescriptor.buildFrom(set.getFile(set.getFileCount() - 1), dependencyFileDescriptorList.toArray(new FileDescriptor[0]));
  }

  // Replace convertToJson(DynamicMessage) with:
  private JsonObject convertToJson(DynamicMessage message) {
    JsonObject jsonObject = new JsonObject();
    for (Map.Entry<FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
      FieldDescriptor field = entry.getKey();
      Object value = entry.getValue();
      if (field.isRepeated()) {
        jsonObject.add(field.getName(), convertRepeatedToJson(field, (Collection<?>) value));
      } else {
        jsonObject.add(field.getName(), toJsonElement(field, value));
      }
    }
    return jsonObject;
  }

  // New helper for repeated fields:
  private JsonArray convertRepeatedToJson(FieldDescriptor field, Collection<?> values) {
    JsonArray jsonArray = new JsonArray();
    for (Object value : values) {
      jsonArray.add(toJsonElement(field, value));
    }
    return jsonArray;
  }

  // New leaf serializer that avoids gson reflection traps:
  private com.google.gson.JsonElement toJsonElement(FieldDescriptor field, Object value) {
    if (value == null) return com.google.gson.JsonNull.INSTANCE;

    switch (field.getJavaType()) {
      case STRING:
        return new JsonPrimitive((String) value);

      case INT:
        return new JsonPrimitive(((Number) value).intValue());

      case LONG:
        return new JsonPrimitive(((Number) value).longValue());

      case FLOAT:
        return new JsonPrimitive(((Number) value).floatValue());

      case DOUBLE:
        return new JsonPrimitive(((Number) value).doubleValue());

      case BOOLEAN:
        return new JsonPrimitive((Boolean) value);

      case BYTE_STRING:
        // Base64 for bytes
        String b64 = Base64.getEncoder().encodeToString(((ByteString) value).toByteArray());
        return new JsonPrimitive(b64);

      case ENUM:
        // Prefer enum name
        return new JsonPrimitive(field.getEnumType().findValueByNumber(((Descriptors.EnumValueDescriptor) value).getNumber()).getName());

      case MESSAGE:
        // Nested message
        return convertToJson((DynamicMessage) value);

      default:
        // Last-resort string
        return new JsonPrimitive(String.valueOf(value));
    }
  }


  // New coercion helper for JSON→Proto types (bytes, enums, numerics, nested):
  private Object coerceForField(FieldDescriptor field, Object value) {
    if (value == null) return null;

    switch (field.getJavaType()) {
      case STRING:
        return String.valueOf(value);

      case INT:
        if (value instanceof Number n) return n.intValue();
        return Integer.parseInt(String.valueOf(value));

      case LONG:
        if (value instanceof Number n) return n.longValue();
        return Long.parseLong(String.valueOf(value));

      case FLOAT:
        if (value instanceof Number n) return n.floatValue();
        return Float.parseFloat(String.valueOf(value));

      case DOUBLE:
        if (value instanceof Number n) return n.doubleValue();
        return Double.parseDouble(String.valueOf(value));

      case BOOLEAN:
        if (value instanceof Boolean b) return b;
        String s = String.valueOf(value);
        if ("1".equals(s)) return true;
        if ("0".equals(s)) return false;
        return Boolean.parseBoolean(s);

      case BYTE_STRING:
        if (value instanceof String bs) {
          // Expect base64 string
          return ByteString.copyFrom(Base64.getDecoder().decode(bs));
        }
        if (value instanceof Collection<?> coll) {
          // Accept array of numbers as bytes
          byte[] bytes = new byte[coll.size()];
          int i = 0;
          for (Object o : coll) bytes[i++] = ((Number) o).byteValue();
          return ByteString.copyFrom(bytes);
        }
        throw new IllegalArgumentException("bytes field expects base64 string or array of numbers: " + field.getName());

      case ENUM:
        if (value instanceof Number n) {
          Descriptors.EnumValueDescriptor ev = field.getEnumType().findValueByNumber(n.intValue());
          if (ev == null) throw new IllegalArgumentException("Unknown enum number " + n + " for " + field.getFullName());
          return ev;
        } else {
          String name = String.valueOf(value);
          // Try by name first
          Descriptors.EnumValueDescriptor ev = field.getEnumType().findValueByName(name);
          if (ev != null) return ev;
          // Then try numeric-in-string
          try {
            int num = Integer.parseInt(name);
            ev = field.getEnumType().findValueByNumber(num);
            if (ev != null) return ev;
          } catch (NumberFormatException ignore) {
          }
          throw new IllegalArgumentException("Unknown enum value '" + name + "' for " + field.getFullName());
        }

      case MESSAGE:
        if (value instanceof Map<?, ?> m) {
          @SuppressWarnings("unchecked")
          Map<String, Object> nested = (Map<String, Object>) m;
          DynamicMessage.Builder nestedBuilder = DynamicMessage.newBuilder(field.getMessageType());
          for (FieldDescriptor nf : field.getMessageType().getFields()) {
            Object nv = nested.get(nf.getName());
            if (nv == null) continue;
            if (nf.isRepeated() && nv instanceof Collection<?> coll) {
              for (Object el : coll) nestedBuilder.addRepeatedField(nf, coerceForField(nf, el));
            } else {
              nestedBuilder.setField(nf, coerceForField(nf, nv));
            }
          }
          return nestedBuilder.build();
        }
        throw new IllegalArgumentException("Message field expects object for " + field.getFullName());

      default:
        throw new IllegalStateException("Unhandled type for " + field.getFullName());
    }
  }

  private Map<String, Object> convertToMap(DynamicMessage message) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (Entry<FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
      if (entry.getValue() instanceof Collection collection) {
        map.put(entry.getKey().getName(), createMap(collection));
      } else {
        map.put(entry.getKey().getName(), entry.getValue());
      }
    }
    return map;
  }

  private List<Map<String, Object>> createMap(Collection<Object> collection) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (Object obj : collection) {
      if (obj instanceof DynamicMessage dynamicMessage) {
        list.add(convertToMap(dynamicMessage));
      }
    }
    return list;
  }
}