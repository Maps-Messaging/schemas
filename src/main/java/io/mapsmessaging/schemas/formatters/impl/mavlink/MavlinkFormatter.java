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

import com.google.gson.*;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.impl.mavlink.message.*;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class MavlinkFormatter extends MessageFormatter {

  private final String dialectName;
  private final MavlinkPayloadPacker packer;
  private final MavlinkPayloadParser parser;

  public MavlinkFormatter(String dialect, MavlinkPayloadPacker packer, MavlinkPayloadParser parser) {
    this.dialectName = dialect;
    this.packer = packer;
    this.parser = parser;
  }

  @Override
  public String getName() {
    return "mavlink";
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    String dialect = config.getFormat();
    if (dialect == null || dialect.isEmpty() || dialect.equalsIgnoreCase("common")) {
      return MavlinkMessageFormatLoader.getInstance().getDialect("common");
    }
    return null;
  }

  @Override
  public JsonObject parseToJson(byte[] payload) throws IOException {
    MavlinkFrame frame = buildFrame(payload);
    Map<String, Object> map = convertToMap(frame);
    return JsonParser.parseString(new Gson().toJson(map)).getAsJsonObject();
  }

  @Override
  public byte[] parseFromJson(JsonObject jsonObject) throws IOException {
    if (jsonObject == null) {
      throw new IOException("JSON object is null");
    }

    // Required: messageId (MAVLink message id)
    if (!jsonObject.has("messageId")) {
      throw new IOException("Missing required field 'messageId' in MAVLink JSON");
    }
    int messageId = jsonObject.get("messageId").getAsInt();

    // Optional metadata, provide sane defaults
    int systemId = jsonObject.has("systemId") ? jsonObject.get("systemId").getAsInt() : 1;
    int componentId = jsonObject.has("componentId") ? jsonObject.get("componentId").getAsInt() : 1;
    int sequence = jsonObject.has("sequence") ? jsonObject.get("sequence").getAsInt() : 0;

    // We need the compiled message definition to know which fields exist
    MavlinkMessageRegistry registry = packer.getMessageRegistry();
    MavlinkCompiledMessage compiledMessage = registry.getCompiledMessagesById().get(messageId);
    if (compiledMessage == null) {
      throw new IOException("Unknown MAVLink message id: " + messageId + " for dialect " + dialectName);
    }

    Map<String, Object> values = new HashMap<>();

    for (MavlinkCompiledField compiledField : compiledMessage.getCompiledFields()) {
      String fieldName = compiledField.getFieldDefinition().getName();
      if (!jsonObject.has(fieldName)) {
        // Missing → zero-filled by packer
        continue;
      }

      if (jsonObject.get(fieldName).isJsonArray()) {
        JsonArray array = jsonObject.getAsJsonArray(fieldName);
        List<Object> list = new ArrayList<>(array.size());
        for (int index = 0; index < array.size(); index++) {
          if (array.get(index).isJsonPrimitive()) {
            JsonPrimitive primitive = array.get(index).getAsJsonPrimitive();
            if (primitive.isNumber()) {
              list.add(primitive.getAsNumber());
            } else if (primitive.isString()) {
              list.add(primitive.getAsString());
            } else if (primitive.isBoolean()) {
              list.add(primitive.getAsBoolean() ? 1 : 0);
            }
          }
        }
        values.put(fieldName, list);
      } else if (jsonObject.get(fieldName).isJsonPrimitive()) {
        JsonPrimitive primitive = jsonObject.get(fieldName).getAsJsonPrimitive();
        Object value;
        if (primitive.isNumber()) {
          value = primitive.getAsNumber();
        } else if (primitive.isString()) {
          value = primitive.getAsString();
        } else if (primitive.isBoolean()) {
          value = primitive.getAsBoolean() ? 1 : 0;
        } else {
          continue;
        }
        values.put(fieldName, value);
      }
    }

    // Build MAVLink payload using your packer
    byte[] payload = packer.packPayload(messageId, values);

    // Wrap into a MAVLink 2 frame (no signing, CRC set to 0 for now)
    int payloadLength = payload.length;
    int headerLength = 10;       // STX + LEN + incompat + compat + seq + sysId + compId + 3-byte msgId
    int checksumLength = 2;
    int frameLength = headerLength + payloadLength + checksumLength;

    ByteBuffer buffer = ByteBuffer.allocate(frameLength);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    buffer.put((byte) 0xFD);                 // MAVLink 2 STX
    buffer.put((byte) payloadLength);        // payload length
    buffer.put((byte) 0x00);                 // incompat flags
    buffer.put((byte) 0x00);                 // compat flags
    buffer.put((byte) (sequence & 0xFF));    // sequence
    buffer.put((byte) (systemId & 0xFF));    // system id
    buffer.put((byte) (componentId & 0xFF)); // component id
    buffer.put((byte) (messageId & 0xFF));           // msgid low
    buffer.put((byte) ((messageId >> 8) & 0xFF));    // msgid mid
    buffer.put((byte) ((messageId >> 16) & 0xFF));   // msgid high
    buffer.put(payload);

    // Compute CRC here
    buffer.putShort((short) 0);

    return buffer.array();
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    MavlinkFrame frame = buildFrame(payload);
    return new StructuredResolver(new MapResolver(convertToMap(frame)), frame);
  }

  @Override
  public Map<String, Object> getFormat() {
    return Map.of("dialect", dialectName);
  }

  private MavlinkFrame buildFrame(byte[] payload) {
    MavlinkFrameParser frameParser = new MavlinkFrameParser();
    return frameParser.parse(payload);
  }

  private Map<String, Object> convertToMap(MavlinkFrame frame) {
    return parser.parsePayload(frame.getMessageId(), frame.getPayload());

  }
}
