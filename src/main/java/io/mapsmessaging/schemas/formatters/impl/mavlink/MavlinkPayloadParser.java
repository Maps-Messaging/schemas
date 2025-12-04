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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.HashMap;
import java.util.Map;

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

      Object value = fieldCodec.decode(buffer);
      result.put(fieldDefinition.getName(), value);
    }

    return result;
  }
}
