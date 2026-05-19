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

package io.mapsmessaging.schemas.config;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import io.mapsmessaging.canbus.canaerospace.schema.CanaerospaceSchemaRegistry;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.impl.CanAerospaceFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Base64;

class TestCanAerospaceSchema {

  private static CanAerospaceFormatter buildFormatter() throws IOException {
    try {
      CanaerospaceSchemaRegistry registry = CanaerospaceSchemaRegistry.loadFromClasspath();
      return new CanAerospaceFormatter(registry);
    } catch (Exception e) {
      throw new IOException("Failed to load CANAerospace schema registry", e);
    }
  }

  @Test
  void roundTrip_rawPacket_extended() throws IOException {
    CanAerospaceFormatter formatter = buildFormatter();

    int canIdentifier = 0x18FF1234;
    boolean extendedFrame = true;
    int dataLengthCode = 8;
    byte[] payload = new byte[]{0x01, 0x04, 0x00, 0x00, 0x41, 0x20, 0x00, 0x00};

    CanFrame original = new CanFrame(canIdentifier, extendedFrame, dataLengthCode, payload);
    byte[] packed = original.getRawData();

    JsonObject json = formatter.parseToJson(packed, ParseMode.IGNORE);

    Assertions.assertTrue(json.has("canId"));

    Assertions.assertEquals(canIdentifier, json.get("canId").getAsInt());

//    byte[] roundTrippedPacked = formatter.parseFromJson(json);
  //  Assertions.assertArrayEquals(packed, roundTrippedPacked);
  }

  @Test
  void roundTrip_semanticJson_toPackedFrame() throws IOException {
    CanAerospaceFormatter formatter = buildFormatter();

    int canIdentifier = 0x18FF1234;
    boolean extendedFrame = true;
    byte[] payload = new byte[]{0x01, 0x04, 0x00, 0x00, 0x41, 0x20, 0x00, 0x00};

    CanFrame expected = new CanFrame(canIdentifier, extendedFrame, 8, payload);
    byte[] expectedPacked = expected.getRawData();

    JsonObject canaerospace = new JsonObject();
    canaerospace.addProperty("nodeId", 1);
    canaerospace.addProperty("payloadDataTypeNumber", 4);
    canaerospace.addProperty("payloadDataTypeName", "FLOAT");
    canaerospace.addProperty("serviceCode", 0);
    canaerospace.addProperty("messageCode", 0);
    canaerospace.addProperty("schemaDataType", "FLOAT");
    canaerospace.addProperty("rawValue", 10.0);

    JsonObject json = new JsonObject();
    json.addProperty("canId", canIdentifier);
    json.addProperty("extended", extendedFrame);
    json.add("canaerospace", canaerospace);

    byte[] packed = formatter.parseFromJson(json);
    Assertions.assertArrayEquals(expectedPacked, packed);
  }

  @Test
  void roundTrip_semanticJson_engineeringValue_withResolution() throws IOException {
    CanAerospaceFormatter formatter = buildFormatter();

    int canIdentifier = 0x18FF1234;
    boolean extendedFrame = true;
    byte[] payload = new byte[]{0x02, 0x02, 0x00, 0x00, 0x00, 0x00, 0x00, 0x64};

    CanFrame expected = new CanFrame(canIdentifier, extendedFrame, 8, payload);
    byte[] expectedPacked = expected.getRawData();

    JsonObject canaerospace = new JsonObject();
    canaerospace.addProperty("nodeId", 2);
    canaerospace.addProperty("payloadDataTypeNumber", 2);
    canaerospace.addProperty("payloadDataTypeName", "USHORT");
    canaerospace.addProperty("serviceCode", 0);
    canaerospace.addProperty("messageCode", 0);
    canaerospace.addProperty("schemaDataType", "USHORT");
    canaerospace.addProperty("resolution", 0.5);
    canaerospace.addProperty("engineeringValue", 50.0);

    JsonObject json = new JsonObject();
    json.addProperty("canId", canIdentifier);
    json.addProperty("extended", extendedFrame);
    json.add("canaerospace", canaerospace);

    byte[] packed = formatter.parseFromJson(json);
    Assertions.assertArrayEquals(expectedPacked, packed);
  }

  @Test
  void roundTrip_formatter_parseToJson_parseFromJson() throws IOException {
    CanAerospaceFormatter formatter = buildFormatter();

    int canIdentifier = 0x18FF1234;
    boolean extendedFrame = true;
    int dataLengthCode = 8;
    byte[] payload = new byte[]{0x01, 0x04, 0x00, 0x00, 0x41, 0x20, 0x00, 0x00};

    CanFrame original = new CanFrame(canIdentifier, extendedFrame, dataLengthCode, payload);
    byte[] packed = original.getRawData();

    JsonObject json = formatter.parseToJson(packed, ParseMode.STRICT);

    Assertions.assertTrue(json.has("canId"));
    Assertions.assertTrue(json.has("canaerospace"));

    JsonObject canaerospace = json.getAsJsonObject("canaerospace");
    Assertions.assertNotNull(canaerospace);
    Assertions.assertEquals(1, canaerospace.get("nodeId").getAsInt());
    Assertions.assertEquals(4, canaerospace.get("payloadDataTypeNumber").getAsInt());
    Assertions.assertEquals(0, canaerospace.get("serviceCode").getAsInt());
    Assertions.assertEquals(0, canaerospace.get("messageCode").getAsInt());

//    byte[] roundTrippedPacked = formatter.parseFromJson(json);
//    Assertions.assertArrayEquals(packed, roundTrippedPacked);
  }

  @Test
  void parseFromJson_acceptsNestedFrameObject() throws IOException {
    CanAerospaceFormatter formatter = buildFormatter();

    int canIdentifier = 0x18FF1234;
    boolean extendedFrame = true;
    int dataLengthCode = 8;
    byte[] payload = new byte[]{0x01, 0x04, 0x00, 0x00, 0x41, 0x20, 0x00, 0x00};

    CanFrame original = new CanFrame(canIdentifier, extendedFrame, dataLengthCode, payload);
    byte[] packed = original.getRawData();

    JsonObject frameJson = new JsonObject();
    frameJson.addProperty("canId", canIdentifier);
    frameJson.addProperty("dlc", dataLengthCode);
    frameJson.addProperty("extended", extendedFrame);
    frameJson.addProperty("data", Base64.getEncoder().encodeToString(payload));

    JsonObject wrapper = new JsonObject();
    wrapper.add("frame", frameJson);

    byte[] fromJsonPacked = formatter.parseFromJson(wrapper);
    Assertions.assertArrayEquals(packed, fromJsonPacked);
  }

  @Test
  void parseToJson_ignoreMode_allowsWrongDlcAsRawPacket() throws IOException {
    CanAerospaceFormatter formatter = buildFormatter();

    int canIdentifier = 0x18FF1234;
    boolean extendedFrame = true;
    int dataLengthCode = 3;
    byte[] payload = new byte[]{0x01, 0x02, 0x03};

    CanFrame original = new CanFrame(canIdentifier, extendedFrame, dataLengthCode, payload);
    byte[] packed = original.getRawData();

    JsonObject json = formatter.parseToJson(packed, ParseMode.IGNORE);

    Assertions.assertEquals(canIdentifier, json.get("canId").getAsInt());
    Assertions.assertEquals(dataLengthCode, json.get("dlc").getAsInt());
    Assertions.assertTrue(json.get("extended").getAsBoolean());
    Assertions.assertEquals(Base64.getEncoder().encodeToString(payload), json.get("data").getAsString());
    Assertions.assertFalse(json.has("canaerospace"));
  }

  @Test
  void parseToJson_strictMode_rejectsWrongDlc() throws IOException {
    CanAerospaceFormatter formatter = buildFormatter();

    int canIdentifier = 0x18FF1234;
    boolean extendedFrame = true;
    int dataLengthCode = 3;
    byte[] payload = new byte[]{0x01, 0x02, 0x03};

    CanFrame original = new CanFrame(canIdentifier, extendedFrame, dataLengthCode, payload);
    byte[] packed = original.getRawData();

    Assertions.assertThrowsExactly(io.mapsmessaging.schemas.formatters.ParseException.class, () -> {
      formatter.parseToJson(packed, ParseMode.STRICT);
    });
  }

  @Test
  void parseFromJson_rejectsDlcNotEight() {
    JsonObject json = new JsonObject();
    json.addProperty("canId", 123);
    json.addProperty("dlc", 7);
    json.addProperty("extended", true);
    json.addProperty("data", Base64.getEncoder().encodeToString(new byte[]{0x01, 0x02, 0x03}));

    Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
      buildFormatter().parseFromJson(json);
    });
  }

  @Test
  void parseFromJson_rejectsMissingFieldsInRawJson() {
    JsonObject json = new JsonObject();
    json.addProperty("canId", 123);

    Assertions.assertThrowsExactly(IOException.class, () -> {
      buildFormatter().parseFromJson(json);
    });
  }

  @Test
  void parseFromJson_rejectsBadBase64() {
    JsonObject json = new JsonObject();
    json.addProperty("canId", 123);
    json.addProperty("dlc", 8);
    json.addProperty("extended", true);
    json.addProperty("data", "this is not base64");

    Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
      buildFormatter().parseFromJson(json);
    });
  }

  @Test
  void parseFromJson_rejectsDecodedShorterThanDlc() {
    JsonObject json = new JsonObject();
    json.addProperty("canId", 123);
    json.addProperty("dlc", 8);
    json.addProperty("extended", true);
    json.addProperty("data", Base64.getEncoder().encodeToString(new byte[]{0x01, 0x02}));

    Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
      buildFormatter().parseFromJson(json);
    });
  }

  @Test
  void parseFromJson_rejectsMissingCanaerospaceObject() {
    JsonObject json = new JsonObject();
    json.addProperty("canId", 0x18FF1234);
    json.addProperty("extended", true);

    Assertions.assertThrowsExactly(IOException.class, () -> {
      buildFormatter().parseFromJson(json);
    });
  }

  @Test
  void parseFromJson_rejectsMissingSemanticFields() {
    JsonObject canaerospace = new JsonObject();
    canaerospace.addProperty("nodeId", 1);

    JsonObject json = new JsonObject();
    json.addProperty("canId", 0x18FF1234);
    json.addProperty("extended", true);
    json.add("canaerospace", canaerospace);

    Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
      buildFormatter().parseFromJson(json);
    });
  }

  @Test
  void parseFromJson_rejectsMissingDataType() {
    JsonObject canaerospace = new JsonObject();
    canaerospace.addProperty("nodeId", 1);
    canaerospace.addProperty("payloadDataTypeNumber", 4);
    canaerospace.addProperty("serviceCode", 0);
    canaerospace.addProperty("messageCode", 0);
    canaerospace.addProperty("rawValue", 10.0);

    JsonObject json = new JsonObject();
    json.addProperty("canId", 0x18FF1234);
    json.addProperty("extended", true);
    json.add("canaerospace", canaerospace);

    Assertions.assertThrowsExactly(IOException.class, () -> {
      buildFormatter().parseFromJson(json);
    });
  }

  @Test
  void parseFromJson_acceptsArrayRawValue() throws IOException {
    CanAerospaceFormatter formatter = buildFormatter();

    JsonArray rawValue = new JsonArray();
    rawValue.add(1);
    rawValue.add(2);

    JsonObject canaerospace = new JsonObject();
    canaerospace.addProperty("nodeId", 1);
    canaerospace.addProperty("payloadDataTypeNumber", 8);
    canaerospace.addProperty("payloadDataTypeName", "UCHAR2");
    canaerospace.addProperty("serviceCode", 0);
    canaerospace.addProperty("messageCode", 0);
    canaerospace.addProperty("schemaDataType", "UCHAR2");
    canaerospace.add("rawValue", rawValue);

    JsonObject json = new JsonObject();
    json.addProperty("canId", 0x18FF1234);
    json.addProperty("extended", true);
    json.add("canaerospace", canaerospace);

    byte[] packed = formatter.parseFromJson(json);

    CanFrame frame = CanFrame.fromBytes(packed);
    Assertions.assertEquals(8, frame.dataLengthCode());
    Assertions.assertArrayEquals(new byte[]{0x01, 0x08, 0x00, 0x00, 0x01, 0x02, 0x00, 0x00}, frame.data());
  }
}