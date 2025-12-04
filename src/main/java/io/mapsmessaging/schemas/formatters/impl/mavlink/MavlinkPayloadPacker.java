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
import io.mapsmessaging.schemas.formatters.impl.mavlink.parser.MavlinkFieldDefinition;
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
      if (value == null) {
        // MAVLink expects zero-fill for missing values
        encodeZero(compiledField, buffer);
        continue;
      }

      codec.encode(buffer, value);
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
