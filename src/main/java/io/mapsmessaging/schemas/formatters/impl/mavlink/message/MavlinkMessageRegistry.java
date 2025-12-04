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

package io.mapsmessaging.schemas.formatters.impl.mavlink.message;

import io.mapsmessaging.schemas.formatters.impl.mavlink.message.fields.AbstractMavlinkFieldCodec;
import io.mapsmessaging.schemas.formatters.impl.mavlink.message.fields.MavlinkFieldCodecFactory;
import io.mapsmessaging.schemas.formatters.impl.mavlink.parser.MavlinkDialectDefinition;
import io.mapsmessaging.schemas.formatters.impl.mavlink.parser.MavlinkFieldDefinition;
import io.mapsmessaging.schemas.formatters.impl.mavlink.parser.MavlinkMessageDefinition;
import lombok.Data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class MavlinkMessageRegistry {

  private String dialectName;

  private List<MavlinkCompiledMessage> compiledMessages;

  private Map<Integer, MavlinkCompiledMessage> compiledMessagesById;

  public static MavlinkMessageRegistry fromDialectDefinition(MavlinkDialectDefinition dialectDefinition) {
    MavlinkMessageRegistry registry = new MavlinkMessageRegistry();
    registry.setDialectName(dialectDefinition.getName());

    List<MavlinkCompiledMessage> compiledMessageList = new ArrayList<>();
    Map<Integer, MavlinkCompiledMessage> compiledByIdMap = new HashMap<>();

    for (MavlinkMessageDefinition messageDefinition : dialectDefinition.getMessages()) {
      MavlinkCompiledMessage compiledMessage = compileMessage(messageDefinition);
      compiledMessageList.add(compiledMessage);
      compiledByIdMap.put(compiledMessage.getMessageId(), compiledMessage);
    }

    registry.setCompiledMessages(compiledMessageList);
    registry.setCompiledMessagesById(compiledByIdMap);

    return registry;
  }

  private static MavlinkCompiledMessage compileMessage(MavlinkMessageDefinition messageDefinition) {
    MavlinkCompiledMessage compiledMessage = new MavlinkCompiledMessage();
    compiledMessage.setMessageId(messageDefinition.getMessageId());
    compiledMessage.setName(messageDefinition.getName());
    compiledMessage.setMessageDefinition(messageDefinition);

    List<MavlinkCompiledField> compiledFields = new ArrayList<>();

    int currentOffset = 0;
    for (MavlinkFieldDefinition fieldDefinition : messageDefinition.getFields()) {
      AbstractMavlinkFieldCodec fieldCodec = MavlinkFieldCodecFactory.createCodec(fieldDefinition);

      MavlinkCompiledField compiledField = new MavlinkCompiledField();
      compiledField.setFieldDefinition(fieldDefinition);
      compiledField.setFieldCodec(fieldCodec);
      compiledField.setOffsetInPayload(currentOffset);
      compiledField.setSizeInBytes(fieldCodec.getSizeInBytes());

      compiledFields.add(compiledField);
      currentOffset = currentOffset + fieldCodec.getSizeInBytes();
    }

    compiledMessage.setCompiledFields(compiledFields);
    compiledMessage.setPayloadSizeBytes(currentOffset);

    return compiledMessage;
  }
}
