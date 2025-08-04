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
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.Descriptors.DescriptorValidationException;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FileDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.InvalidProtocolBufferException;
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
   * @param messageName the message name
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
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    ProtoBufSchemaConfig protoBufSchemaConfig = (ProtoBufSchemaConfig) config;
    return new ProtoBufFormatter(protoBufSchemaConfig.getMessageName(), protoBufSchemaConfig.getDescriptorValue());
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

  private JsonObject convertToJson(DynamicMessage message) {
    JsonObject jsonObject = new JsonObject();
    for (Map.Entry<FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
      String key = entry.getKey().getName();
      Object value = entry.getValue();
      if (value instanceof Collection) {
        jsonObject.add(key, convertToJson((Collection<?>) value));
      } else if (value instanceof DynamicMessage) {
        jsonObject.add(key, convertToJson((DynamicMessage) value));
      } else {
        jsonObject.add(key, gson.toJsonTree(value));
      }
    }
    return jsonObject;
  }

  private JsonArray convertToJson(Collection<?> collection) {
    JsonArray jsonArray = new JsonArray();
    for (Object obj : collection) {
      if (obj instanceof DynamicMessage) {
        jsonArray.add(convertToJson((DynamicMessage) obj));
      } else {
        jsonArray.add(gson.toJsonTree(obj));
      }
    }
    return jsonArray;
  }


  private Map<String, Object> convertToMap(DynamicMessage message) {
    Map<String, Object> map = new LinkedHashMap<>();
    for (Entry<FieldDescriptor, Object> entry : message.getAllFields().entrySet()) {
      if (entry.getValue() instanceof Collection) {
        map.put(entry.getKey().getName(), createMap((Collection) entry.getValue()));
      } else {
        map.put(entry.getKey().getName(), entry.getValue());
      }
    }
    return map;
  }

  private List<Map<String, Object>> createMap(Collection<Object> collection) {
    List<Map<String, Object>> list = new ArrayList<>();
    for (Object obj : collection) {
      if (obj instanceof DynamicMessage) {
        list.add(convertToMap((DynamicMessage) obj));
      }
    }
    return list;
  }
}