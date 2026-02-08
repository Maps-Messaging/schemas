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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.canbus.j1939.CanId;
import io.mapsmessaging.canbus.j1939.n2k.N2kParserFactory;
import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.canbus.j1939.n2k.compile.N2kCompiledMessage;
import io.mapsmessaging.canbus.j1939.n2k.compile.N2kCompiledRegistry;
import io.mapsmessaging.canbus.j1939.n2k.compile.N2kCompiler;
import io.mapsmessaging.canbus.j1939.n2k.parser.N2kXmlDialectParser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

public class CanbusFormatter extends MessageFormatter {

  private final N2kMessageParser parser;

  public CanbusFormatter() {
    parser = null;
  }

  public CanbusFormatter(String databasePath) throws IOException {
    N2kCompiledRegistry registry;
    try {
      if (databasePath != null && !databasePath.isEmpty()) {
        registry = N2kCompiler.compile(N2kXmlDialectParser.parseFromFile(Path.of(databasePath)));
      }
      else {
        registry = N2kParserFactory.getN2kParser();
      }
    }
    catch (Exception e) {
      throw new IOException(e);
    }
    parser = new N2kMessageParser(registry);
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    JsonObject json = parseToJson(payload);
    Map<String, Object> map = gson.fromJson(json, Map.class);
    return new StructuredResolver(new MapResolver(map), json);
  }

  @Override
  public Map<String, Object> getFormat() {
    return Map.of();
  }

  @Override
  public JsonObject parseToJson(byte[] payload) {
    CanFrame frame = CanFrame.fromBytes(payload);
    int id = frame.canIdentifier();
    CanId canId = CanId.parse(id);

    JsonObject json;
    if (parser != null) {
      N2kCompiledMessage compiledMessage = parser.getRegistry().getMessagesByPgn().get(canId.getPgn());
      if (compiledMessage != null) {
        JsonObject n2kJson = parser.decodeToJson(canId.getPgn(), frame.data());
        json = buildEnvelope(n2kJson, canId, frame);
        return json;
      }
    }
    json = processRawPacket(frame);
    return json;
  }

  @Override
  public byte[] parseFromJson(JsonObject json) throws IOException {
    CanFrame frame = fromJson(json);
    return frame.getRawData();
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) {
    return new CanbusFormatter();
  }

  private JsonObject buildEnvelope(JsonObject n2kJson, CanId canId, CanFrame frame) {
    JsonObject envelope = new JsonObject();

    envelope.addProperty("canId", frame.canIdentifier());
    envelope.addProperty("dlc", frame.dataLengthCode());
    envelope.addProperty("extended", frame.extendedFrame());
    envelope.addProperty("data", Base64.getEncoder().encodeToString(frame.data()));

    JsonObject j1939 = new JsonObject();
    j1939.addProperty("source", canId.getSourceAddress());
    j1939.addProperty("destination", canId.isPdu1() ? canId.getDestinationAddress() : 255);
    j1939.addProperty("priority", canId.getPriority());
    j1939.addProperty("pgn", canId.getPgn());
    j1939.addProperty("pdu1", canId.isPdu1());

    if (n2kJson != null) {
      j1939.add("n2k", n2kJson);
    }

    envelope.add("j1939", j1939);
    return envelope;
  }

  private static JsonObject processRawPacket(CanFrame frame) {
    JsonObject envelope = new JsonObject();
    envelope.addProperty("canId", frame.canIdentifier());
    envelope.addProperty("dlc", frame.dataLengthCode());
    envelope.addProperty("extended", frame.extendedFrame());
    envelope.addProperty("data", Base64.getEncoder().encodeToString(frame.data()));
    return envelope;
  }

  @Override
  public String getName() {
    return "canbus";
  }

  private static CanFrame fromJson(JsonObject json) {
    if (json == null) {
      throw new IllegalArgumentException("json must not be null");
    }

    JsonObject source = json;

    if (json.has("frame") && json.get("frame").isJsonObject()) {
      source = json.getAsJsonObject("frame");
    }

    int canIdentifier = requireInt(source, "canId");
    int dataLengthCode = requireInt(source, "dlc");
    boolean extendedFrame = requireBoolean(source, "extended");
    String dataBase64 = requireString(source, "data");

    if (dataLengthCode < 0 || dataLengthCode > 8) {
      throw new IllegalArgumentException("dlc must be 0..8 but was " + dataLengthCode);
    }

    byte[] decoded = decodeBase64(dataBase64);

    if (decoded.length < dataLengthCode) {
      throw new IllegalArgumentException(
          "Decoded data length (" + decoded.length + ") is less than dlc (" + dataLengthCode + ")"
      );
    }

    byte[] payload = new byte[dataLengthCode];
    if (dataLengthCode > 0) {
      System.arraycopy(decoded, 0, payload, 0, dataLengthCode);
    }

    return new CanFrame(canIdentifier, extendedFrame, dataLengthCode, payload);
  }

  private static int requireInt(JsonObject object, String field) {
    JsonElement element = object.get(field);
    if (element == null || element.isJsonNull()) {
      throw new IllegalArgumentException("Missing field '" + field + "'");
    }
    return element.getAsInt();
  }

  private static boolean requireBoolean(JsonObject object, String field) {
    JsonElement element = object.get(field);
    if (element == null || element.isJsonNull()) {
      throw new IllegalArgumentException("Missing field '" + field + "'");
    }
    return element.getAsBoolean();
  }

  private static String requireString(JsonObject object, String field) {
    JsonElement element = object.get(field);
    if (element == null || element.isJsonNull()) {
      throw new IllegalArgumentException("Missing field '" + field + "'");
    }
    return element.getAsString();
  }

  private static byte[] decodeBase64(String value) {
    try {
      return Base64.getDecoder().decode(value);
    }
    catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid Base64 payload", exception);
    }
  }
}
