/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.schemas.formatters.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.canbus.canaerospace.parser.CanaerospaceFrameParser;
import io.mapsmessaging.canbus.canaerospace.parser.DataTypeCodec;
import io.mapsmessaging.canbus.canaerospace.parser.ParsedCanaerospaceMessage;
import io.mapsmessaging.canbus.canaerospace.schema.CanaerospaceSchemaRegistry;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.CanAerospaceSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParseException;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import io.mapsmessaging.schemas.repository.SchemaResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

public class CanAerospaceFormatter extends MessageFormatter {

  private static final int CAN_AEROSPACE_DLC = 8;

  private final CanaerospaceFrameParser parser;

  public CanAerospaceFormatter() {
    this.parser = null;
  }

  public CanAerospaceFormatter(String yamlPath) throws IOException {
    this(loadRegistry(yamlPath));
  }

  public CanAerospaceFormatter(CanaerospaceSchemaRegistry registry) {
    if (registry == null) {
      throw new IllegalArgumentException("registry must not be null");
    }
    this.parser = new CanaerospaceFrameParser(registry);
  }

  @Override
  public ParsedObject parse(byte[] payload, ParseMode parseMode) throws ParseException {
    JsonObject json = parseToJson(payload, parseMode);
    Map<String, Object> map = gson.fromJson(json, Map.class);
    return new StructuredResolver(new MapResolver(map), json);
  }

  @Override
  public Map<String, Object> getFormat() {
    return Map.of();
  }

  @Override
  public JsonObject parseToJson(byte[] payload, ParseMode parseMode) throws ParseException {
    CanFrame frame = CanFrame.fromBytes(payload);

    if (frame.dataLengthCode() != CAN_AEROSPACE_DLC) {
      if (parseMode == ParseMode.IGNORE) {
        return processRawPacket(frame);
      }
      throw new ParseException("CANAerospace frame requires dlc=8 but was " + frame.dataLengthCode());
    }

    if (parser == null) {
      if (parseMode == ParseMode.IGNORE) {
        return processRawPacket(frame);
      }
      throw new ParseException("No CANAerospace parser configured");
    }

    try {
      ParsedCanaerospaceMessage parsedMessage = parser.parse(frame.canIdentifier(), frame.data());
      return buildEnvelope(frame, parsedMessage);
    } catch (IllegalArgumentException exception) {
      if (parseMode == ParseMode.IGNORE) {
        return processRawPacket(frame);
      }
      throw new ParseException("Failed to parse CANAerospace frame", exception);
    }
  }

  @Override
  public byte[] parseFromJson(JsonObject json) throws IOException {
    CanFrame frame = fromJson(json);
    return frame.getRawData();
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config, SchemaResolver schemaResolver) throws IOException {
    if (!(config instanceof CanAerospaceSchemaConfig canAerospaceSchemaConfig)) {
      throw new IOException("Invalid config type for CanAerospace formatter: " + config.getClass().getName());
    }

    String yamlPath = canAerospaceSchemaConfig.getYamlPath();
    if (yamlPath != null && !yamlPath.isBlank()) {
      return new CanAerospaceFormatter(yamlPath);
    }

    try {
      return new CanAerospaceFormatter(CanaerospaceSchemaRegistry.loadFromClasspath());
    } catch (Exception e) {
      throw new IOException(e);
    }
  }

  @Override
  public String getName() {
    return "canaerospace";
  }

  private static CanaerospaceSchemaRegistry loadRegistry(String yamlPath) throws IOException {
    if (yamlPath == null || yamlPath.isBlank()) {
      throw new IOException("yamlPath must not be null or blank");
    }
    try {
      return CanaerospaceSchemaRegistry.load(Path.of(yamlPath));
    } catch (Exception e) {
      throw new IOException("Failed to load CANAerospace schema registry", e);
    }
  }

  private JsonObject buildEnvelope(CanFrame frame, ParsedCanaerospaceMessage parsedMessage) {
    JsonObject envelope = new JsonObject();
    envelope.addProperty("canId", frame.canIdentifier());
    envelope.addProperty("dlc", frame.dataLengthCode());
    envelope.addProperty("extended", frame.extendedFrame());
    envelope.addProperty("data", Base64.getEncoder().encodeToString(frame.data()));

    JsonObject canaerospace = new JsonObject();
    addString(canaerospace, "messageType", parsedMessage.getMessageType());
    canaerospace.addProperty("nodeId", parsedMessage.getNodeId());
    canaerospace.addProperty("payloadDataTypeNumber", parsedMessage.getPayloadDataTypeNumber());
    addString(canaerospace, "payloadDataTypeName", parsedMessage.getPayloadDataTypeName());
    canaerospace.addProperty("serviceCode", parsedMessage.getServiceCode());
    canaerospace.addProperty("messageCode", parsedMessage.getMessageCode());
    canaerospace.addProperty("dataBytes", Base64.getEncoder().encodeToString(parsedMessage.getDataBytes()));

    addString(canaerospace, "group", parsedMessage.getGroup());
    addString(canaerospace, "title", parsedMessage.getTitle());
    addString(canaerospace, "name", parsedMessage.getName());
    addString(canaerospace, "schemaDataType", parsedMessage.getSchemaDataType());
    addString(canaerospace, "units", parsedMessage.getUnits());
    addString(canaerospace, "notes", parsedMessage.getNotes());

    addNumber(canaerospace, "resolution", parsedMessage.getResolution());
    addNumber(canaerospace, "rangeMin", parsedMessage.getRangeMin());
    addNumber(canaerospace, "rangeMax", parsedMessage.getRangeMax());

    Object rawValue = parsedMessage.getRawValue();
    if (rawValue != null) {
      canaerospace.add("rawValue", gson.toJsonTree(rawValue));
    }

    addNumber(canaerospace, "engineeringValue", parsedMessage.getEngineeringValue());
    canaerospace.addProperty("dataTypeMismatch", parsedMessage.isDataTypeMismatch());

    envelope.add("canaerospace", canaerospace);
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

  private CanFrame fromJson(JsonObject json) throws IOException {
    if (json == null) {
      throw new IllegalArgumentException("json must not be null");
    }

    JsonObject rawFrameSource = getRawFrameSource(json);
    if (rawFrameSource != null) {
      return buildFrameFromRawJson(rawFrameSource);
    }

    if (!json.has("canaerospace") || !json.get("canaerospace").isJsonObject()) {
      throw new IOException("Missing 'canaerospace' object");
    }

    JsonObject canaerospace = json.getAsJsonObject("canaerospace");

    int canIdentifier = requireInt(json, "canId");
    boolean extendedFrame = getBoolean(json, "extended", true);

    int nodeId = requireInt(canaerospace, "nodeId");
    int payloadDataTypeNumber = requireInt(canaerospace, "payloadDataTypeNumber");
    int serviceCode = requireInt(canaerospace, "serviceCode");
    int messageCode = requireInt(canaerospace, "messageCode");

    String schemaDataType = getString(canaerospace, "schemaDataType");
    if (schemaDataType == null || schemaDataType.isBlank()) {
      schemaDataType = getString(canaerospace, "payloadDataTypeName");
    }
    if (schemaDataType == null || schemaDataType.isBlank()) {
      throw new IOException("Missing CANAerospace data type. Expected 'schemaDataType' or 'payloadDataTypeName'");
    }

    Object rawValue = extractValue(canaerospace, "rawValue");
    if (rawValue == null && canaerospace.has("engineeringValue")) {
      Double engineeringValue = canaerospace.get("engineeringValue").getAsDouble();
      Double resolution = getDouble(canaerospace, "resolution");

      if (resolution != null && resolution != 0.0d) {
        rawValue = engineeringValue / resolution;
      } else {
        rawValue = engineeringValue;
      }
    }

    byte[] dataBytes = DataTypeCodec.encode(schemaDataType, rawValue);

    byte[] payload = new byte[CAN_AEROSPACE_DLC];
    payload[0] = (byte) nodeId;
    payload[1] = (byte) payloadDataTypeNumber;
    payload[2] = (byte) serviceCode;
    payload[3] = (byte) messageCode;
    System.arraycopy(dataBytes, 0, payload, 4, 4);

    return new CanFrame(canIdentifier, extendedFrame, CAN_AEROSPACE_DLC, payload);
  }

  private static JsonObject getRawFrameSource(JsonObject json) {
    if (json.has("data")) {
      return json;
    }

    if (json.has("frame") && json.get("frame").isJsonObject()) {
      JsonObject frame = json.getAsJsonObject("frame");
      if (frame.has("data")) {
        return frame;
      }
    }

    return null;
  }

  private static CanFrame buildFrameFromRawJson(JsonObject source) {
    int canIdentifier = requireInt(source, "canId");
    int dataLengthCode = requireInt(source, "dlc");
    boolean extendedFrame = requireBoolean(source, "extended");
    String dataBase64 = requireString(source, "data");

    if (dataLengthCode != CAN_AEROSPACE_DLC) {
      throw new IllegalArgumentException("dlc must be 8 for CANAerospace but was " + dataLengthCode);
    }

    byte[] decoded = decodeBase64(dataBase64);
    if (decoded.length < dataLengthCode) {
      throw new IllegalArgumentException(
          "Decoded data length (" + decoded.length + ") is less than dlc (" + dataLengthCode + ")"
      );
    }

    byte[] payload = new byte[dataLengthCode];
    System.arraycopy(decoded, 0, payload, 0, dataLengthCode);

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

  private static boolean getBoolean(JsonObject object, String field, boolean defaultValue) {
    JsonElement element = object.get(field);
    if (element == null || element.isJsonNull()) {
      return defaultValue;
    }
    return element.getAsBoolean();
  }

  private static String getString(JsonObject object, String field) {
    JsonElement element = object.get(field);
    if (element == null || element.isJsonNull()) {
      return null;
    }
    return element.getAsString();
  }

  private static Double getDouble(JsonObject object, String field) {
    JsonElement element = object.get(field);
    if (element == null || element.isJsonNull()) {
      return null;
    }
    return element.getAsDouble();
  }

  private Object extractValue(JsonObject object, String field) {
    JsonElement element = object.get(field);
    if (element == null || element.isJsonNull()) {
      return null;
    }
    return gson.fromJson(element, Object.class);
  }

  private static byte[] decodeBase64(String value) {
    try {
      return Base64.getDecoder().decode(value);
    } catch (IllegalArgumentException exception) {
      throw new IllegalArgumentException("Invalid Base64 payload", exception);
    }
  }

  private static void addString(JsonObject jsonObject, String name, String value) {
    if (value != null) {
      jsonObject.addProperty(name, value);
    }
  }

  private static void addNumber(JsonObject jsonObject, String name, Number value) {
    if (value != null) {
      jsonObject.addProperty(name, value);
    }
  }
}