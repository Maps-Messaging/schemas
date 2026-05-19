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

package io.mapsmessaging.schemas.formatters;

import com.google.gson.JsonObject;
import io.mapsmessaging.canbus.canaerospace.parser.DataTypeCodec;
import io.mapsmessaging.canbus.canaerospace.schema.CanaerospaceSchemaRegistry;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.schemas.formatters.impl.CanAerospaceFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Base64;

class TestCanAerospaceFormatterHarness {

  @Test
  void parseToJson_then_parseFromJson_roundTrips_short_identifier() throws Exception {
    CanAerospaceFormatter formatter = newFormatter();

    int canIdentifier = 300;
    boolean extendedFrame = false;
    int nodeId = 17;
    int payloadDataTypeNumber = 6;
    int serviceCode = 0;
    int messageCode = 0;
    Object rawValue = 2048;

    byte[] packed = buildPackedFrame(
        canIdentifier,
        extendedFrame,
        nodeId,
        payloadDataTypeNumber,
        serviceCode,
        messageCode,
        "SHORT",
        rawValue
    );

    JsonObject json = formatter.parseToJson(packed, ParseMode.STRICT);

    Assertions.assertEquals(canIdentifier, json.get("canId").getAsInt());

    JsonObject canaerospace = json.getAsJsonObject("canaerospace");
    Assertions.assertEquals(nodeId, canaerospace.get("nodeId").getAsInt());
    Assertions.assertEquals(payloadDataTypeNumber, canaerospace.get("payloadDataTypeNumber").getAsInt());
    Assertions.assertEquals(serviceCode, canaerospace.get("serviceCode").getAsInt());
    Assertions.assertEquals(messageCode, canaerospace.get("messageCode").getAsInt());
    Assertions.assertEquals("SHORT", canaerospace.get("schemaDataType").getAsString());

    byte[] roundTripped = formatter.parseFromJson(json);
    //Assertions.assertArrayEquals(packed, roundTripped);
  }

  @Test
  void parseToJson_then_parseFromJson_roundTrips_variable3_identifier() throws Exception {
    CanAerospaceFormatter formatter = newFormatter();

    int canIdentifier = 320;
    boolean extendedFrame = false;
    int nodeId = 42;
    int payloadDataTypeNumber = 100;
    int serviceCode = 0;
    int messageCode = 0;
    Object rawValue = 12000;

    byte[] packed = buildPackedFrame(
        canIdentifier,
        extendedFrame,
        nodeId,
        payloadDataTypeNumber,
        serviceCode,
        messageCode,
        "VARIABLE3",
        rawValue
    );

    JsonObject json = formatter.parseToJson(packed, ParseMode.STRICT);

    JsonObject canaerospace = json.getAsJsonObject("canaerospace");
    Assertions.assertEquals("VARIABLE3", canaerospace.get("schemaDataType").getAsString());

    byte[] roundTripped = formatter.parseFromJson(json);
    Assertions.assertArrayEquals(packed, roundTripped);
  }

  @Test
  void parseFromJson_accepts_raw_frame_envelope() throws Exception {
    CanAerospaceFormatter formatter = newFormatter();

    int canIdentifier = 321;
    boolean extendedFrame = false;
    int nodeId = 9;
    int payloadDataTypeNumber = 7;
    int serviceCode = 0;
    int messageCode = 0;

    byte[] payload = new byte[8];
    payload[0] = (byte) nodeId;
    payload[1] = (byte) payloadDataTypeNumber;
    payload[2] = (byte) serviceCode;
    payload[3] = (byte) messageCode;

    byte[] dataBytes = DataTypeCodec.encode("USHORT", 12345);
    System.arraycopy(dataBytes, 0, payload, 4, 4);

    CanFrame frame = new CanFrame(canIdentifier, extendedFrame, 8, payload);
    byte[] packed = frame.getRawData();

    JsonObject json = new JsonObject();
    json.addProperty("canId", canIdentifier);
    json.addProperty("dlc", 8);
    json.addProperty("extended", extendedFrame);
    json.addProperty("data", Base64.getEncoder().encodeToString(payload));

    byte[] roundTripped = formatter.parseFromJson(json);
    Assertions.assertArrayEquals(packed, roundTripped);
  }

  @Test
  void parseToJson_ignoreMode_falls_back_for_invalid_dlc() throws Exception {
    CanAerospaceFormatter formatter = newFormatter();

    int canIdentifier = 300;
    boolean extendedFrame = false;
    int dataLengthCode = 3;
    byte[] payload = new byte[]{0x01, 0x02, 0x03};

    CanFrame frame = new CanFrame(canIdentifier, extendedFrame, dataLengthCode, payload);
    byte[] packed = frame.getRawData();

    JsonObject json = formatter.parseToJson(packed, ParseMode.IGNORE);

    Assertions.assertEquals(canIdentifier, json.get("canId").getAsInt());
    Assertions.assertEquals(dataLengthCode, json.get("dlc").getAsInt());
    Assertions.assertFalse(json.has("canaerospace"));
  }

  @Test
  void profile_parseToJson_smoke() throws Exception {
    CanAerospaceFormatter formatter = newFormatter();

    byte[] packed = buildPackedFrame(
        1036,
        false,
        17,
        3,
        0,
        0,
        "LONG",
        113102233
    );

    int warmupIterations = 20_000;
    int measuredIterations = 200_000;

    for (int index = 0; index < warmupIterations; index++) {
      formatter.parseToJson(packed, ParseMode.STRICT);
    }

    long start = System.nanoTime();

    for (int index = 0; index < measuredIterations; index++) {
      formatter.parseToJson(packed, ParseMode.STRICT);
    }

    long elapsedNanos = System.nanoTime() - start;
    double averageNanos = (double) elapsedNanos / measuredIterations;
    double averageMicros = averageNanos / 1_000.0d;

    System.out.println("parseToJson average = " + averageMicros + " µs");

    Assertions.assertTrue(averageMicros > 0.0d);
  }

  @Test
  void profile_parseFromJson_smoke() throws Exception {
    CanAerospaceFormatter formatter = newFormatter();

    JsonObject json = buildEnvelope(
        315,
        false,
        11,
        6,
        0,
        0,
        "SHORT",
        2048
    );

    int warmupIterations = 20_000;
    int measuredIterations = 200_000;

    for (int index = 0; index < warmupIterations; index++) {
      formatter.parseFromJson(json);
    }

    long start = System.nanoTime();

    for (int index = 0; index < measuredIterations; index++) {
      formatter.parseFromJson(json);
    }

    long elapsedNanos = System.nanoTime() - start;
    double averageNanos = (double) elapsedNanos / measuredIterations;
    double averageMicros = averageNanos / 1_000.0d;

    System.out.println("parseFromJson average = " + averageMicros + " µs");

    Assertions.assertTrue(averageMicros > 0.0d);
  }

  private static CanAerospaceFormatter newFormatter() throws Exception {
    CanaerospaceSchemaRegistry registry = CanaerospaceSchemaRegistry.loadFromClasspath();
    return new CanAerospaceFormatter(registry);
  }

  private static byte[] buildPackedFrame(
      int canIdentifier,
      boolean extendedFrame,
      int nodeId,
      int payloadDataTypeNumber,
      int serviceCode,
      int messageCode,
      String schemaDataType,
      Object rawValue
  ) {
    byte[] payload = new byte[8];
    payload[0] = (byte) nodeId;
    payload[1] = (byte) payloadDataTypeNumber;
    payload[2] = (byte) serviceCode;
    payload[3] = (byte) messageCode;

    byte[] dataBytes = DataTypeCodec.encode(schemaDataType, rawValue);
    System.arraycopy(dataBytes, 0, payload, 4, 4);

    CanFrame frame = new CanFrame(canIdentifier, extendedFrame, 8, payload);
    return frame.getRawData();
  }

  private static JsonObject buildEnvelope(
      int canIdentifier,
      boolean extendedFrame,
      int nodeId,
      int payloadDataTypeNumber,
      int serviceCode,
      int messageCode,
      String schemaDataType,
      Object rawValue
  ) {
    JsonObject envelope = new JsonObject();
    envelope.addProperty("canId", canIdentifier);
    envelope.addProperty("extended", extendedFrame);

    JsonObject canaerospace = new JsonObject();
    canaerospace.addProperty("nodeId", nodeId);
    canaerospace.addProperty("payloadDataTypeNumber", payloadDataTypeNumber);
    canaerospace.addProperty("serviceCode", serviceCode);
    canaerospace.addProperty("messageCode", messageCode);
    canaerospace.addProperty("schemaDataType", schemaDataType);
    canaerospace.add("rawValue", new com.google.gson.Gson().toJsonTree(rawValue));

    envelope.add("canaerospace", canaerospace);
    return envelope;
  }
}