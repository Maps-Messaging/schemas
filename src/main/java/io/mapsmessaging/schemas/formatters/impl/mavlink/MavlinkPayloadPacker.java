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

    int size = compiledMessage.getPayloadSizeBytes();
    ByteBuffer buffer = ByteBuffer.allocate(size);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    for (MavlinkCompiledField compiledField : compiledMessage.getCompiledFields()) {
      MavlinkFieldDefinition field = compiledField.getFieldDefinition();
      AbstractMavlinkFieldCodec codec = compiledField.getFieldCodec();

      Object value = values.get(field.getName());

      // Missing value: zero-fill the whole field (MAVLink semantics)
      if (value == null) {
        encodeZero(compiledField, buffer);
        continue;
      }

      if (!field.isArray()) {
        // Scalar
        codec.encode(buffer, value);
      } else {
        int len = field.getArrayLength();

        if (field.getWireType() == MavlinkWireType.CHAR) {
          // MAVLink strings: fixed-size, null-terminated, null-padded
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
          for (int i = copyLen; i < len; i++) {
            buffer.put((byte) 0);
          }
        } else {
          // Numeric / non-char arrays: expect List<?> or primitive array
          java.util.List<?> elements;

          if (value instanceof java.util.List<?> list) {
            elements = list;
          } else if (value.getClass().isArray()) {
            int arrayLen = java.lang.reflect.Array.getLength(value);
            java.util.List<Object> tmp = new java.util.ArrayList<>(arrayLen);
            for (int i = 0; i < arrayLen; i++) {
              tmp.add(java.lang.reflect.Array.get(value, i));
            }
            elements = tmp;
          } else {
            throw new IllegalArgumentException(
                "Array field '" + field.getName() + "' expects List or array, got: "
                    + value.getClass().getName());
          }

          int count = Math.min(len, elements.size());
          for (int i = 0; i < count; i++) {
            codec.encode(buffer, elements.get(i));
          }

          // Zero-fill remaining elements if list is shorter than declared array length
          int remaining = len - count;
          if (remaining > 0) {
            int elementSize = field.getWireType().getSizeInBytes();
            int bytesToZero = remaining * elementSize;
            for (int i = 0; i < bytesToZero; i++) {
              buffer.put((byte) 0);
            }
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
