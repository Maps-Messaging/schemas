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

package io.mapsmessaging.schemas.formatters.impl.mavlink;


import io.mapsmessaging.schemas.formatters.impl.mavlink.message.MavlinkCompiledField;
import io.mapsmessaging.schemas.formatters.impl.mavlink.message.MavlinkCompiledMessage;
import io.mapsmessaging.schemas.formatters.impl.mavlink.message.MavlinkMessageRegistry;
import io.mapsmessaging.schemas.formatters.impl.mavlink.message.fields.AbstractMavlinkFieldCodec;
import io.mapsmessaging.schemas.formatters.impl.mavlink.message.fields.MavlinkFieldDefinition;
import io.mapsmessaging.schemas.formatters.impl.mavlink.message.fields.MavlinkWireType;
import lombok.Getter;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MavlinkPayloadPacker {

  @Getter
  private final MavlinkMessageRegistry messageRegistry;

  public MavlinkPayloadPacker(MavlinkMessageRegistry messageRegistry) {
    this.messageRegistry = messageRegistry;
  }

  public byte[] packPayload(int messageId, Map<String, Object> values) {
    MavlinkCompiledMessage compiledMessage =
        messageRegistry.getCompiledMessagesById().get(messageId);

    if (compiledMessage == null) {
      throw new IllegalArgumentException("Unknown MAVLink message id: " + messageId);
    }

    List<MavlinkCompiledField> compiledFields = compiledMessage.getCompiledFields();

    // 1) Find the last extension field that actually has a non-null value
    int lastExtensionIndex = -1;
    for (int i = 0; i < compiledFields.size(); i++) {
      MavlinkCompiledField compiledField = compiledFields.get(i);
      MavlinkFieldDefinition field = compiledField.getFieldDefinition();
      if (!field.isExtension()) {
        continue;
      }
      Object value = values.get(field.getName());
      if (value != null) {
        lastExtensionIndex = i;
      }
    }

    // 2) Compute payload size: all base fields, plus extension fields up to lastExtensionIndex
    int size = 0;
    for (int i = 0; i < compiledFields.size(); i++) {
      MavlinkCompiledField compiledField = compiledFields.get(i);
      MavlinkFieldDefinition field = compiledField.getFieldDefinition();

      if (!field.isExtension()) {
        size += compiledField.getSizeInBytes();
      } else if (i <= lastExtensionIndex) {
        size += compiledField.getSizeInBytes();
      } else {
        // trailing extensions with no values are omitted entirely
        break;
      }
    }

    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    // 3) Encode fields
    for (int i = 0; i < compiledFields.size(); i++) {
      MavlinkCompiledField compiledField = compiledFields.get(i);
      MavlinkFieldDefinition field = compiledField.getFieldDefinition();
      AbstractMavlinkFieldCodec codec = compiledField.getFieldCodec();

      // Stop once we are past the last extension we decided to include
      if (field.isExtension() && i > lastExtensionIndex) {
        break;
      }

      Object value = values.get(field.getName());

      // Base fields: always encoded, null => all zeros
      // Extension fields up to lastExtensionIndex: always present, null => all zeros
      if (value == null) {
        encodeZero(compiledField, buffer);
        continue;
      }

      if (!field.isArray()) {
        codec.encode(buffer, value);
        continue;
      }

      int len = field.getArrayLength();

      if (field.getWireType() == MavlinkWireType.CHAR) {
        byte[] src;
        if (value instanceof String s) {
          src = s.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        } else if (value instanceof byte[] b) {
          src = b;
        } else {
          throw new IllegalArgumentException(
              "CHAR array field '" + field.getName() + "' expects String or byte[], got: "
                  + value.getClass().getName());
        }

        int copyLen = Math.min(len, src.length);
        buffer.put(src, 0, copyLen);
        for (int j = copyLen; j < len; j++) {
          buffer.put((byte) 0);
        }
      } else {
        List<?> elements;
        if (value instanceof List<?> list) {
          elements = list;
        } else if (value.getClass().isArray()) {
          int arrayLen = java.lang.reflect.Array.getLength(value);
          List<Object> tmp = new ArrayList<>(arrayLen);
          for (int j = 0; j < arrayLen; j++) {
            tmp.add(java.lang.reflect.Array.get(value, j));
          }
          elements = tmp;
        } else {
          throw new IllegalArgumentException(
              "Array field '" + field.getName() + "' expects List or array, got: "
                  + value.getClass().getName());
        }

        int count = Math.min(len, elements.size());
        for (int j = 0; j < count; j++) {
          codec.encode(buffer, elements.get(j));
        }

        int remaining = len - count;
        if (remaining > 0) {
          int elementSize = field.getWireType().getSizeInBytes();
          int bytesToZero = remaining * elementSize;
          for (int j = 0; j < bytesToZero; j++) {
            buffer.put((byte) 0);
          }
        }
      }
    }

    return buffer.array();
  }

  private void encodeZero(MavlinkCompiledField compiledField, ByteBuffer buffer) {
    int size = compiledField.getSizeInBytes();
    for (int i = 0; i < size; i++) {
      buffer.put((byte) 0);
    }
  }

}
