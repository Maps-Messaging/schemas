/*
 *
 *     Copyright [ 2020 - 2026 ] [Matthew Buckton]
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package io.mapsmessaging.schemas.formatters.impl;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.protobuf.ByteString;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig;
import io.mapsmessaging.schemas.config.impl.protobuf.DescriptorLoader;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParseException;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import io.mapsmessaging.schemas.repository.SchemaResolver;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

/**
 * The type Proto buf formatter.
 */
public class ProtoBufFormatter extends MessageFormatter {

  private final String messageName;
  private final Map<String, Descriptors.FileDescriptor> descriptors;

  /**
   * Instantiates a new Proto buf formatter.
   */
  public ProtoBufFormatter() {
    messageName = "";
    descriptors = new HashMap<>();
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
      DescriptorLoader loader = new DescriptorLoader();
      this.descriptors = loader.loadDescFiles(descriptorImage);
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
    if (descriptors == null || descriptors.isEmpty() || messageName == null || messageName.isEmpty()) {
      return Map.of();
    }

    Descriptors.Descriptor messageDescriptor = findMessageDescriptor(messageName);
    if (messageDescriptor == null) {
      return Map.of();
    }

    return buildMessageFormat(messageDescriptor, 0, 1, new LinkedHashSet<>());
  }

  private Map<String, Object> buildMessageFormat(
      Descriptors.Descriptor descriptor,
      int currentDepth,
      int maximumDepth,
      Set<String> visitedMessageTypes) {

    Map<String, Object> result = new LinkedHashMap<>();
    List<Map<String, Object>> fieldList = new ArrayList<>();
    List<Map<String, Object>> oneOfList = new ArrayList<>();

    result.put("messageName", descriptor.getFullName());
    result.put("fields", fieldList);

    for (Descriptors.OneofDescriptor oneofDescriptor : descriptor.getRealOneofs()) {
      Map<String, Object> oneOfInfo = new LinkedHashMap<>();
      List<String> oneOfFields = new ArrayList<>();

      oneOfInfo.put("name", oneofDescriptor.getName());
      for (FieldDescriptor fieldDescriptor : oneofDescriptor.getFields()) {
        oneOfFields.add(fieldDescriptor.getName());
      }
      oneOfInfo.put("fields", oneOfFields);

      oneOfList.add(oneOfInfo);
    }

    if (!oneOfList.isEmpty()) {
      result.put("oneOfs", oneOfList);
    }

    visitedMessageTypes.add(descriptor.getFullName());

    for (FieldDescriptor fieldDescriptor : descriptor.getFields()) {
      fieldList.add(buildFieldFormat(fieldDescriptor, currentDepth, maximumDepth, visitedMessageTypes));
    }

    visitedMessageTypes.remove(descriptor.getFullName());

    return result;
  }

  private Map<String, Object> buildFieldFormat(
      FieldDescriptor fieldDescriptor,
      int currentDepth,
      int maximumDepth,
      Set<String> visitedMessageTypes) {

    Map<String, Object> fieldInfo = new LinkedHashMap<>();
    fieldInfo.put("name", fieldDescriptor.getName());
    fieldInfo.put("number", fieldDescriptor.getNumber());
    fieldInfo.put("repeated", fieldDescriptor.isRepeated());
    fieldInfo.put("map", fieldDescriptor.isMapField());
    fieldInfo.put("label", resolveLabel(fieldDescriptor));
    fieldInfo.put("type", fieldDescriptor.getType().name().toLowerCase());

    if (fieldDescriptor.getContainingOneof() != null) {
      fieldInfo.put("oneOf", fieldDescriptor.getContainingOneof().getName());
    }

    if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.ENUM) {
      fieldInfo.put("enumType", fieldDescriptor.getEnumType().getFullName());
      fieldInfo.put("enumValues", buildEnumValues(fieldDescriptor.getEnumType()));
    }

    if (fieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
      Descriptors.Descriptor nestedDescriptor = fieldDescriptor.getMessageType();
      String nestedMessageName = nestedDescriptor.getFullName();

      fieldInfo.put("messageType", nestedMessageName);

      if (fieldDescriptor.isMapField()) {
        fieldInfo.put("mapKeyType", fieldDescriptor.getMessageType().findFieldByName("key").getType().name().toLowerCase());

        FieldDescriptor valueFieldDescriptor = fieldDescriptor.getMessageType().findFieldByName("value");
        fieldInfo.put("mapValueType", valueFieldDescriptor.getType().name().toLowerCase());

        if (valueFieldDescriptor.getJavaType() == FieldDescriptor.JavaType.MESSAGE) {
          fieldInfo.put("mapValueMessageType", valueFieldDescriptor.getMessageType().getFullName());
        }

        if (valueFieldDescriptor.getJavaType() == FieldDescriptor.JavaType.ENUM) {
          fieldInfo.put("mapValueEnumType", valueFieldDescriptor.getEnumType().getFullName());
          fieldInfo.put("mapValueEnumValues", buildEnumValues(valueFieldDescriptor.getEnumType()));
        }
      } else if (currentDepth < maximumDepth && !visitedMessageTypes.contains(nestedMessageName)) {
        fieldInfo.put(
            "structure",
            buildMessageFormat(
                nestedDescriptor,
                currentDepth + 1,
                maximumDepth,
                new LinkedHashSet<>(visitedMessageTypes)
            )
        );
      }
    }

    return fieldInfo;
  }

  private List<String> buildEnumValues(Descriptors.EnumDescriptor enumDescriptor) {
    List<String> enumValues = new ArrayList<>();
    for (Descriptors.EnumValueDescriptor enumValueDescriptor : enumDescriptor.getValues()) {
      enumValues.add(enumValueDescriptor.getName());
    }
    return enumValues;
  }

  private String resolveLabel(FieldDescriptor fieldDescriptor) {
    if (fieldDescriptor.isRequired()) {
      return "required";
    }
    if (fieldDescriptor.isRepeated()) {
      return "repeated";
    }
    return "optional";
  }

  @Override
  public ParsedObject parse(byte[] payload, ParseMode parseMode) throws ParseException {
    try {
      DynamicMessage message = DynamicMessage.parseFrom(findMessageDescriptor(messageName), payload);
      ParsedObject parsed = new MapResolver(convertToMap(message));
      return new StructuredResolver(parsed, message);
    } catch (InvalidProtocolBufferException e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      if (parseMode == ParseMode.IGNORE) {
        return new DefaultParser(payload);
      }
      throw new ParseException(e.getMessage(), e);
    }
  }

  @Override
  public JsonObject parseToJson(byte[] payload, ParseMode parseMode) throws ParseException {
    DynamicMessage dynamicMessage;
    try {
      dynamicMessage = (DynamicMessage) (parse(payload, ParseMode.IGNORE)).getReferenced();
    } catch (ParseException e) {
      throw new ParseException(e.getMessage(), e);
    }
    return convertToJson(dynamicMessage);
  }

  @Override
  public byte[] parseFromJson(JsonObject jsonObject) throws IOException {
    if (descriptors == null || messageName == null || messageName.isEmpty()) return new byte[0];

    Descriptors.Descriptor messageDescriptor = findMessageDescriptor(messageName);
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

  @Override
  public MessageFormatter getInstance(SchemaConfig config, SchemaResolver schemaResolver) throws IOException {
    ProtoBufSchemaConfig protoBufSchemaConfig = (ProtoBufSchemaConfig) config;
    ProtoBufSchemaConfig.ProtobufConfig protobufConfig = protoBufSchemaConfig.getProtobufConfig();
    String name = protobufConfig.getMessageName();
    if (config.isChild()) {
      SchemaConfig parent = schemaResolver.resolveParent(config);
      protobufConfig = ((ProtoBufSchemaConfig) parent).getProtobufConfig();
    }
    return new ProtoBufFormatter(name, protobufConfig.getDescriptorValue());
  }

  private JsonObject convertToJson(DynamicMessage message) throws ParseException {
    JsonObject jsonObject = new JsonObject();
    for (Map.Entry<FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
      try {
        FieldDescriptor field = entry.getKey();
        Object value = entry.getValue();
        if (field.isRepeated()) {
          jsonObject.add(field.getName(), convertRepeatedToJson(field, (Collection<?>) value));
        } else {
          jsonObject.add(field.getName(), toJsonElement(field, value));
        }
      } catch (Exception e) {
        throw new ParseException("Error converting message to JSON", e);
      }
    }
    return jsonObject;
  }

  private JsonArray convertRepeatedToJson(FieldDescriptor field, Collection<?> values) throws ParseException {
    JsonArray jsonArray = new JsonArray();
    for (Object value : values) {
      jsonArray.add(toJsonElement(field, value));
    }
    return jsonArray;
  }

  // New leaf serializer that avoids gson reflection traps:
  private JsonElement toJsonElement(FieldDescriptor field, Object value) throws ParseException {
    if (value == null) return com.google.gson.JsonNull.INSTANCE;

    return switch (field.getJavaType()) {
      case STRING -> new JsonPrimitive((String) value);
      case INT -> new JsonPrimitive(((Number) value).intValue());
      case LONG -> new JsonPrimitive(((Number) value).longValue());
      case FLOAT -> new JsonPrimitive(((Number) value).floatValue());
      case DOUBLE -> new JsonPrimitive(((Number) value).doubleValue());
      case BOOLEAN -> new JsonPrimitive((Boolean) value);
      case BYTE_STRING -> {
        String b64 = Base64.getEncoder().encodeToString(((ByteString) value).toByteArray());
        yield new JsonPrimitive(b64);
      }
      case ENUM ->
          new JsonPrimitive(field.getEnumType().findValueByNumber(((Descriptors.EnumValueDescriptor) value).getNumber()).getName());
      case MESSAGE -> convertToJson((DynamicMessage) value);
      default -> new JsonPrimitive(String.valueOf(value));
    };
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

  private Descriptors.Descriptor findMessageDescriptor(String messageName) {
    if (messageName == null || messageName.isEmpty()) {
      return null;
    }

    for (Descriptors.FileDescriptor fileDescriptor : descriptors.values()) {
      Descriptors.Descriptor descriptor = findMessageDescriptor(fileDescriptor, messageName);
      if (descriptor != null) {
        return descriptor;
      }
    }
    return null;
  }

  private Descriptors.Descriptor findMessageDescriptor(Descriptors.FileDescriptor fileDescriptor, String messageName) {
    for (Descriptors.Descriptor descriptor : fileDescriptor.getMessageTypes()) {
      Descriptors.Descriptor match = findMessageDescriptor(descriptor, messageName);
      if (match != null) {
        return match;
      }
    }
    return null;
  }

  private Descriptors.Descriptor findMessageDescriptor(Descriptors.Descriptor descriptor, String messageName) {
    if (messageName.equals(descriptor.getFullName()) || messageName.equals(descriptor.getName())) {
      return descriptor;
    }

    for (Descriptors.Descriptor nestedDescriptor : descriptor.getNestedTypes()) {
      Descriptors.Descriptor match = findMessageDescriptor(nestedDescriptor, messageName);
      if (match != null) {
        return match;
      }
    }
    return null;
  }
}