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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.canbus.j1939.CanId;
import io.mapsmessaging.canbus.j1939.n2k.N2kParserFactory;
import io.mapsmessaging.canbus.j1939.n2k.codec.N2kMessageParser;
import io.mapsmessaging.canbus.j1939.n2k.canboat.CanboatField;
import io.mapsmessaging.canbus.j1939.n2k.canboat.CanboatMessage;
import io.mapsmessaging.canbus.j1939.n2k.compile.N2kCompiledMessage;
import io.mapsmessaging.canbus.j1939.n2k.compile.N2kCompiledRegistry;
import io.mapsmessaging.canbus.j1939.n2k.compile.N2kCompiler;
import io.mapsmessaging.canbus.j1939.n2k.model.N2kMessageDefinition;
import io.mapsmessaging.canbus.j1939.n2k.parser.N2kXmlDialectParser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CanbusSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParseException;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import io.mapsmessaging.schemas.repository.SchemaResolver;
import lombok.Getter;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashSet;
import java.util.Set;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class CanbusFormatter extends MessageFormatter {

  @Getter
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

  public CanbusFormatter(InputStream stream) throws IOException {
    N2kCompiledRegistry registry;
    try {
      List<N2kMessageDefinition> messageDefinitions = N2kXmlDialectParser.parse(stream);
      registry = N2kCompiler.compile(messageDefinitions);
    } catch (Exception e) {
      throw new IOException(e);
    }
    parser = new N2kMessageParser(registry);
  }


  @Override
  public ParsedObject parse(byte[] payload, ParseMode parseMode) throws ParseException {
    JsonObject json = parseToJson(payload, parseMode);
    Map<String, Object> map = gson.fromJson(json, Map.class);
    restoreCanboatUint64(json, map);
    return new StructuredResolver(new MapResolver(map), json);
  }

  private void restoreCanboatUint64(JsonObject envelope, Map<String, Object> values) {
    if (parser == null || parser.getRegistry().getDatabase() == null || !envelope.has("j1939")) {
      return;
    }
    JsonObject j1939 = envelope.getAsJsonObject("j1939");
    if (!j1939.has("n2k") || !j1939.get("n2k").isJsonObject() || !j1939.has("pgn")) {
      return;
    }
    JsonObject decoded = j1939.getAsJsonObject("n2k");
    if (!decoded.has("packet") || !decoded.get("packet").isJsonObject() || !decoded.has("name")) {
      return;
    }
    Object nested = values.get("j1939");
    if (!(nested instanceof Map<?, ?> outer) || !(outer.get("n2k") instanceof Map<?, ?> n2k)
        || !(n2k.get("packet") instanceof Map<?, ?> packet)) {
      return;
    }
    Set<String> names = new HashSet<>();
    String messageName = decoded.get("name").getAsString();
    for (CanboatMessage message : parser.getRegistry().getDatabase().candidates(j1939.get("pgn").getAsInt())) {
      if (!messageName.equals(message.id())) {
        continue;
      }
      for (CanboatField field : message.fields()) {
        if (field.number("BitLength", 0) == 64 && !Boolean.parseBoolean(field.text("Signed", "false"))
            && ("NUMBER".equals(field.type()) || "ISO_NAME".equals(field.type()))
            && BigDecimal.ONE.compareTo(new BigDecimal(field.text("Resolution", "1"))) == 0
            && BigDecimal.ZERO.compareTo(new BigDecimal(field.text("Offset", "0"))) == 0) {
          names.add(field.id());
        }
      }
      break;
    }
    @SuppressWarnings("unchecked")
    Map<String, Object> packetMap = (Map<String, Object>) packet;
    restoreCanboatFields(decoded.getAsJsonObject("packet"), packetMap, names);
  }

  private void restoreCanboatFields(JsonObject source, Map<String, Object> target, Set<String> names) {
    for (Map.Entry<String, JsonElement> entry : source.entrySet()) {
      String name = entry.getKey();
      JsonElement value = entry.getValue();
      if (names.contains(name) && value.isJsonPrimitive() && value.getAsJsonPrimitive().isNumber()) {
        try {
          BigInteger number = value.getAsBigDecimal().toBigIntegerExact();
          target.put(name, Uint64Values.requireRange(number));
        } catch (ArithmeticException | IOException ignored) {
          // Preserve the underlying parser's value if it is scaled, invalid or not an exact uint64.
        }
      } else if (value.isJsonObject() && target.get(name) instanceof Map<?, ?> nested) {
        @SuppressWarnings("unchecked")
        Map<String, Object> nestedMap = (Map<String, Object>) nested;
        restoreCanboatFields(value.getAsJsonObject(), nestedMap, names);
      } else if (value.isJsonArray() && target.get(name) instanceof List<?> list) {
        JsonArray array = value.getAsJsonArray();
        for (int i = 0; i < array.size() && i < list.size(); i++) {
          if (array.get(i).isJsonObject() && list.get(i) instanceof Map<?, ?> nested) {
            @SuppressWarnings("unchecked")
            Map<String, Object> nestedMap = (Map<String, Object>) nested;
            restoreCanboatFields(array.get(i).getAsJsonObject(), nestedMap, names);
          }
        }
      }
    }
  }

  @Override
  public Map<String, Object> getFormat() {
    return Map.of();
  }

  @Override
  public JsonObject parseToJson(byte[] payload, ParseMode parseMode) throws ParseException {
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
    if (parseMode == ParseMode.IGNORE) {
      return processRawPacket(frame);
    }
    throw new ParseException("No parser found for CAN ID " + id);
  }

  @Override
  public byte[] parseFromJson(JsonObject json) throws IOException {
    CanFrame frame = fromJson(json);
    return frame.getRawData();
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config, SchemaResolver schemaResolver) throws IOException {
    if (!(config instanceof CanbusSchemaConfig canbusSchemaConfig)) {
      throw new IOException("Invalid config type for Canbus formatter: " + config.getClass().getName());
    }
    if (canbusSchemaConfig.getXmlPath() != null && !canbusSchemaConfig.getXmlPath().isEmpty()) {
      return new CanbusFormatter(canbusSchemaConfig.getXmlPath());
    }
    if (canbusSchemaConfig.getXmlBase64() != null) {
      byte[] xml = canbusSchemaConfig.getXmlBase64();
      ByteArrayInputStream n2kStream = new ByteArrayInputStream(xml);
      return new CanbusFormatter(n2kStream);
    }
    return new CanbusFormatter(""); // load default database
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
    j1939.addProperty("pduFormat", canId.getPduFormat());
    j1939.addProperty("pdu1", canId.isPdu1());
    j1939.addProperty("pdu2", canId.isPdu2());
    j1939.addProperty("broadcast", canId.isBroadcast());

    if (canId.isPdu1()) {
      j1939.addProperty("destinationAddress", canId.getDestinationAddress());
    } else {
      j1939.addProperty("groupExtension", canId.getGroupExtension());
    }

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
