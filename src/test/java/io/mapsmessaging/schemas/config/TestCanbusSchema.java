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

import com.google.gson.JsonObject;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.impl.CanbusFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;

class TestCanbusSchema {

  private static void assertN2kDatabasePresentIfPackaged() throws IOException {
    try (InputStream stream = TestCanbusSchema.class.getClassLoader().getResourceAsStream("n2k/n2k.xml")) {
      if (stream == null) {
        return; // optional, depends on packaging; formatter can run raw-only without it
      }
      byte[] tmp = new byte[16];
      int read = stream.read(tmp);
      Assertions.assertTrue(read > 0, "n2k/n2k.xml is empty/unreadable");
    }
  }

  @Test
  void roundTrip_rawPacket_extended() throws IOException {
    assertN2kDatabasePresentIfPackaged();

    CanbusFormatter formatter = new CanbusFormatter();

    int canIdentifier = 0x18F11234;
    boolean extendedFrame = true;
    int dataLengthCode = 8;
    byte[] payload = new byte[]{0x00, 0x11, 0x22, 0x33, 0x44, 0x55, 0x66, 0x77};

    CanFrame original = new CanFrame(canIdentifier, extendedFrame, dataLengthCode, payload);
    byte[] packed = original.getRawData();

    JsonObject json = formatter.parseToJson(packed, ParseMode.IGNORE);

    Assertions.assertTrue(json.has("canId"));
    Assertions.assertTrue(json.has("dlc"));
    Assertions.assertTrue(json.has("extended"));
    Assertions.assertTrue(json.has("data"));

    Assertions.assertEquals(canIdentifier, json.get("canId").getAsInt());
    Assertions.assertEquals(dataLengthCode, json.get("dlc").getAsInt());
    Assertions.assertTrue(json.get("extended").getAsBoolean());
    Assertions.assertEquals(Base64.getEncoder().encodeToString(payload), json.get("data").getAsString());

    byte[] roundTrippedPacked = formatter.parseFromJson(json);
    Assertions.assertArrayEquals(packed, roundTrippedPacked);
  }

  @Test
  void roundTrip_rawPacket_standardId_canOpenFriendly() throws IOException {
    CanbusFormatter formatter = new CanbusFormatter();

    int canIdentifier = 0x000007E8;
    boolean extendedFrame = false;
    int dataLengthCode = 3;
    byte[] payload = new byte[]{0x02, 0x01, 0x0C};

    CanFrame original = new CanFrame(canIdentifier, extendedFrame, dataLengthCode, payload);
    byte[] packed = original.getRawData();

    JsonObject json = formatter.parseToJson(packed, ParseMode.IGNORE);

    Assertions.assertEquals(canIdentifier, json.get("canId").getAsInt());
    Assertions.assertEquals(dataLengthCode, json.get("dlc").getAsInt());
    Assertions.assertFalse(json.get("extended").getAsBoolean());
    Assertions.assertEquals(Base64.getEncoder().encodeToString(payload), json.get("data").getAsString());

    byte[] roundTrippedPacked = formatter.parseFromJson(json);
    Assertions.assertArrayEquals(packed, roundTrippedPacked);
  }

  @Test
  void parseFromJson_acceptsEnvelopeWithJ1939Decoration() throws IOException {
    CanbusFormatter formatter = new CanbusFormatter();

    int canIdentifier = 0x18F11234;
    boolean extendedFrame = true;
    int dataLengthCode = 8;
    byte[] payload = new byte[]{0x10, 0x20, 0x30, 0x40, 0x50, 0x60, 0x70, 0x7F};

    CanFrame original = new CanFrame(canIdentifier, extendedFrame, dataLengthCode, payload);
    byte[] packed = original.getRawData();

    JsonObject envelope = new JsonObject();
    envelope.addProperty("canId", canIdentifier);
    envelope.addProperty("dlc", dataLengthCode);
    envelope.addProperty("extended", extendedFrame);
    envelope.addProperty("data", Base64.getEncoder().encodeToString(payload));

    JsonObject j1939 = new JsonObject();
    j1939.addProperty("source", 17);
    j1939.addProperty("destination", 255);
    j1939.addProperty("priority", 6);
    j1939.addProperty("pgn", 0x1F112);
    j1939.addProperty("pdu1", false);

    JsonObject n2k = new JsonObject();
    n2k.addProperty("messageName", "Whatever");

    j1939.add("n2k", n2k);
    envelope.add("j1939", j1939);

    byte[] fromJsonPacked = formatter.parseFromJson(envelope);
    Assertions.assertArrayEquals(packed, fromJsonPacked);
  }

  @Test
  void parseFromJson_acceptsNestedFrameObject() throws IOException {
    CanbusFormatter formatter = new CanbusFormatter();

    int canIdentifier = 0x18F11234;
    boolean extendedFrame = true;
    int dataLengthCode = 2;
    byte[] payload = new byte[]{(byte) 0xAA, (byte) 0x55};

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
  void parseFromJson_rejectsDlcOutOfRange() {
    JsonObject json = new JsonObject();
    json.addProperty("canId", 123);
    json.addProperty("dlc", 9);
    json.addProperty("extended", true);
    json.addProperty("data", Base64.getEncoder().encodeToString(new byte[]{0x01}));

    Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
      new CanbusFormatter().parseFromJson(json);
    });
  }

  @Test
  void parseFromJson_rejectsMissingFields() {
    JsonObject json = new JsonObject();
    json.addProperty("canId", 123);

    Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
      new CanbusFormatter().parseFromJson(json);
    });
  }

  @Test
  void parseFromJson_rejectsBadBase64() {
    JsonObject json = new JsonObject();
    json.addProperty("canId", 123);
    json.addProperty("dlc", 1);
    json.addProperty("extended", true);
    json.addProperty("data", "this is not base64");

    Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
      new CanbusFormatter().parseFromJson(json);
    });
  }

  @Test
  void parseFromJson_rejectsDecodedShorterThanDlc() {
    JsonObject json = new JsonObject();
    json.addProperty("canId", 123);
    json.addProperty("dlc", 4);
    json.addProperty("extended", true);
    json.addProperty("data", Base64.getEncoder().encodeToString(new byte[]{0x01, 0x02}));

    Assertions.assertThrowsExactly(IllegalArgumentException.class, () -> {
      new CanbusFormatter().parseFromJson(json);
    });
  }
}
