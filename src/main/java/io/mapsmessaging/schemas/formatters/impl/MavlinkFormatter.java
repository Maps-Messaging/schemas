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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.mavlink.MavlinkMessageFormatLoader;
import io.mapsmessaging.mavlink.codec.MavlinkCodec;
import io.mapsmessaging.mavlink.codec.MavlinkFrameCodec;
import io.mapsmessaging.mavlink.message.Frame;
import io.mapsmessaging.mavlink.message.Version;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.MavlinkSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import lombok.Getter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Optional;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

@Getter
public class MavlinkFormatter extends MessageFormatter {

  private static final String DEFAULT_DIALECT = "common";

  private final String dialectName;
  private final MavlinkCodec codec;
  private final MavlinkFrameCodec frameCodec;

  private final Gson gson;

  public MavlinkFormatter() {
    dialectName = null;
    codec = null;
    frameCodec = null;
    gson = null;
  }

  private MavlinkFormatter(String dialectName, MavlinkCodec codec) {
    this.dialectName = dialectName;
    this.codec = codec;
    this.frameCodec = new MavlinkFrameCodec(codec);

    this.gson = new GsonBuilder().disableHtmlEscaping().create();
  }

  @Override
  public String getName() {
    return "mavlink";
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    if (!(config instanceof MavlinkSchemaConfig mavlinkSchemaConfig)) {
      throw new IOException("Invalid config type for MAVLink formatter: " + config.getClass().getName());
    }

    String dialect = mavlinkSchemaConfig.getDialect();
    if (dialect == null || dialect.isBlank()) {
      dialect = DEFAULT_DIALECT;
    }

    MavlinkCodec codec = MavlinkMessageFormatLoader.getInstance().getDialectOrThrow(dialect);

    return new MavlinkFormatter(codec.getName(), codec);
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    try {
      Frame frame = parseFrame(payload);
      Map<String, Object> map = codec.parsePayload(frame.getMessageId(), frame.getPayload());
      return new StructuredResolver(new MapResolver(map), frame);
    } catch (Exception exception) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      return new DefaultParser(payload);
    }
  }

  @Override
  public JsonObject parseToJson(byte[] payload) throws IOException {
    try {
      Frame frame = parseFrame(payload);
      Map<String, Object> map = codec.parsePayload(frame.getMessageId(), frame.getPayload());

      JsonObject json = new JsonObject();
      json.addProperty("dialect", dialectName);

      JsonObject header = new JsonObject();
      header.addProperty("version", frame.getVersion().name());
      header.addProperty("sequence", frame.getSequence());
      header.addProperty("systemId", frame.getSystemId());
      header.addProperty("componentId", frame.getComponentId());
      header.addProperty("messageId", frame.getMessageId());
      header.addProperty("signed", frame.isSigned());
      header.addProperty("incompatibilityFlags", frame.getIncompatibilityFlags() & 0xFF);
      header.addProperty("compatibilityFlags", frame.getCompatibilityFlags() & 0xFF);
      json.add("header", header);

      json.add("payload", JsonParser.parseString(gson.toJson(map)));

      return json;
    } catch (Exception exception) {
      exception.printStackTrace();
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      throw new IOException("Failed to parse MAVLink payload to JSON", exception);
    }
  }

  @Override
  public byte[] parseFromJson(JsonObject jsonObject) throws IOException {
    try {
      JsonObject headerObject = jsonObject.has("header")
          ? jsonObject.getAsJsonObject("header")
          : jsonObject;

      JsonObject payloadObject = jsonObject.has("payload")
          ? jsonObject.getAsJsonObject("payload")
          : jsonObject;

      if (!headerObject.has("messageId")) {
        throw new IOException("Missing required field: messageId");
      }
      if (!headerObject.has("systemId")) {
        throw new IOException("Missing required field: systemId");
      }
      if (!headerObject.has("componentId")) {
        throw new IOException("Missing required field: componentId");
      }
      if (!headerObject.has("sequence")) {
        throw new IOException("Missing required field: sequence");
      }

      int messageId = headerObject.get("messageId").getAsInt();

      @SuppressWarnings("unchecked")
      Map<String, Object> values = gson.fromJson(payloadObject, Map.class);

      byte[] payload = codec.encodePayload(messageId, values);

      Frame frame = new Frame();
      frame.setVersion(Version.V2);
      frame.setSequence(headerObject.get("sequence").getAsInt());
      frame.setSystemId(headerObject.get("systemId").getAsInt());
      frame.setComponentId(headerObject.get("componentId").getAsInt());
      frame.setMessageId(messageId);
      frame.setPayloadLength(payload.length);
      frame.setPayload(payload);

      boolean signed = headerObject.has("signed") && headerObject.get("signed").getAsBoolean();
      frame.setSigned(signed);

      if (headerObject.has("incompatibilityFlags")) {
        frame.setIncompatibilityFlags((byte) headerObject.get("incompatibilityFlags").getAsInt());
      }
      if (headerObject.has("compatibilityFlags")) {
        frame.setCompatibilityFlags((byte) headerObject.get("compatibilityFlags").getAsInt());
      }

      ByteBuffer out = ByteBuffer.allocate(payload.length + 64);
      frameCodec.packFrame(out, frame);
      out.flip();

      byte[] bytes = new byte[out.remaining()];
      out.get(bytes);
      return bytes;

    } catch (IOException exception) {
      throw exception;
    } catch (Exception exception) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), exception.getMessage());
      throw new IOException("Failed to parse JSON to MAVLink payload", exception);
    }
  }

  @Override
  public Map<String, Object> getFormat() {
    return Map.of("dialect", dialectName);
  }

  private Frame parseFrame(byte[] bytes) throws IOException {
    ByteBuffer buffer = ByteBuffer.wrap(bytes);
    Optional<Frame> frameOpt = frameCodec.tryUnpackFrame(buffer); // flip makes sense here
    if (frameOpt.isEmpty()) {
      throw new IOException("Not a valid MAVLink frame");
    }
    return frameOpt.get();
  }
}
