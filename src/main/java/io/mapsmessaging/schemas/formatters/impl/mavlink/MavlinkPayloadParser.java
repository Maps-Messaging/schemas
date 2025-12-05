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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MavlinkPayloadParser {

  private final MavlinkMessageRegistry messageRegistry;

  public MavlinkPayloadParser(MavlinkMessageRegistry messageRegistry) {
    this.messageRegistry = messageRegistry;
  }

  public Map<String, Object> parsePayload(int messageId, byte[] payload) {
    MavlinkCompiledMessage compiledMessage = messageRegistry.getCompiledMessagesById().get(messageId);
    if (compiledMessage == null) {
      throw new IllegalArgumentException("Unknown MAVLink message id: " + messageId);
    }

    if (payload.length < compiledMessage.getPayloadSizeBytes()) {
      throw new IllegalArgumentException(
          "Payload too short for message " + compiledMessage.getName() +
              " expected=" + compiledMessage.getPayloadSizeBytes() +
              " actual=" + payload.length
      );
    }

    Map<String, Object> result = new HashMap<>();

    ByteBuffer buffer = ByteBuffer.wrap(payload);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    for (MavlinkCompiledField compiledField : compiledMessage.getCompiledFields()) {
      MavlinkFieldDefinition fieldDefinition = compiledField.getFieldDefinition();
      AbstractMavlinkFieldCodec fieldCodec = compiledField.getFieldCodec();

      String fieldName = fieldDefinition.getName();

      if (!fieldDefinition.isArray()) {
        Object value = fieldCodec.decode(buffer);
        result.put(fieldName, value);
        continue;
      }

      int len = fieldDefinition.getArrayLength();

      // Arrays
      if (Objects.requireNonNull(fieldDefinition.getWireType()) == MavlinkWireType.CHAR) {
        byte[] bytes = new byte[len];
        for (int i = 0; i < len; i++) {
          Object v = fieldCodec.decode(buffer);   // underlying codec returns Byte
          bytes[i] = (byte) v;
        }
        // strip trailing 0 / '\0'
        int end = len;
        while (end > 0 && bytes[end - 1] == 0) {
          end--;
        }
        String value = new String(bytes, 0, end, StandardCharsets.UTF_8);
        result.put(fieldName, value);
      } else {// For now: JSON-friendly List<Object>. You can later specialise to int[], float[] etc.
        List<Object> values = new ArrayList<>(len);
        for (int i = 0; i < len; i++) {
          values.add(fieldCodec.decode(buffer));
        }
        result.put(fieldName, values);
      }
    }

    return result;
  }
}
