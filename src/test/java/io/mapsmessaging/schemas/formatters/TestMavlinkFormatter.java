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
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.MavlinkSchemaConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class TestMavlinkFormatter {

  private static final String DIALECT = "common";

  private SchemaConfig getSchema() {
    MavlinkSchemaConfig config = new MavlinkSchemaConfig();
    config.setDialect(DIALECT);
    return config;
  }

  private static JsonObject header(JsonObject json) {
    Assertions.assertTrue(json.has("header"));
    return json.getAsJsonObject("header");
  }

  private static JsonObject payload(JsonObject json) {
    Assertions.assertTrue(json.has("payload"));
    return json.getAsJsonObject("payload");
  }

  @Test
  void commandIntEnvelopeStringRoundTripJsonToFrameToJson() throws IOException {
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    Assertions.assertNotNull(formatter);

    String json = """
        {
          "header": {
            "version": "V2",
            "systemId": 2,
            "componentId": 1,
            "sequence": 1,
            "messageId": 75,
            "signed": false,
            "incompatibilityFlags": 0,
            "compatibilityFlags": 0
          },
          "payload": {
            "target_system": 2,
            "target_component": 1,
            "frame": 6,
            "command": 192,
            "current": 0,
            "autocontinue": 0,
            "param1": -1.0,
            "param2": 1.0,
            "param3": 0.0,
            "param4": 0.0,
            "x": 594657200,
            "y": 248236800,
            "z": 100.0
          }
        }
        """;

    JsonObject input = JsonParser.parseString(json).getAsJsonObject();

    byte[] frame = formatter.parseFromJson(input);
    Assertions.assertNotNull(frame);
    Assertions.assertTrue(frame.length > 0);

    JsonObject output = formatter.parseToJson(frame, ParseMode.IGNORE);
    Assertions.assertNotNull(output);

    JsonObject outHeader = header(output);
    JsonObject outPayload = payload(output);

    Assertions.assertEquals(75, outHeader.get("messageId").getAsInt());
    Assertions.assertEquals(2, outHeader.get("systemId").getAsInt());
    Assertions.assertEquals(1, outHeader.get("componentId").getAsInt());

    Assertions.assertEquals(2, outPayload.get("target_system").getAsInt());
    Assertions.assertEquals(1, outPayload.get("target_component").getAsInt());
    Assertions.assertEquals(6, outPayload.get("frame").getAsInt());
    Assertions.assertEquals(192, outPayload.get("command").getAsInt());
    Assertions.assertEquals(0, outPayload.get("current").getAsInt());
    Assertions.assertEquals(0, outPayload.get("autocontinue").getAsInt());

    Assertions.assertEquals(-1.0f, outPayload.get("param1").getAsFloat());
    Assertions.assertEquals(1.0f, outPayload.get("param2").getAsFloat());
    Assertions.assertEquals(0.0f, outPayload.get("param3").getAsFloat());
    Assertions.assertEquals(0.0f, outPayload.get("param4").getAsFloat());

    Assertions.assertEquals(594657200, outPayload.get("x").getAsInt());
    Assertions.assertEquals(248236800, outPayload.get("y").getAsInt());
    Assertions.assertEquals(100.0f, outPayload.get("z").getAsFloat());
  }

  private static JsonObject flattenMavlinkEnvelope(JsonObject envelope) {
    JsonObject mavlink = envelope.getAsJsonObject("mavlink");
    JsonObject decoded = mavlink
        .getAsJsonObject("payload")
        .getAsJsonObject("decoded");

    JsonObject flattened = new JsonObject();

    flattened.add("messageId", mavlink.get("messageId"));
    flattened.add("systemId", mavlink.get("systemId"));
    flattened.add("componentId", mavlink.get("componentId"));

    if (mavlink.has("sequence")) {
      flattened.add("sequence", mavlink.get("sequence"));
    } else {
      flattened.addProperty("sequence", 0);
    }

    for (String key : decoded.keySet()) {
      if (!decoded.get(key).isJsonNull()) {
        flattened.add(key, decoded.get(key));
      }
    }

    return flattened;
  }

  @Test
  void sysStatusRoundTripJsonToFrameToJson() throws IOException {
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    Assertions.assertNotNull(formatter);

    JsonObject input = new JsonObject();
    input.addProperty("messageId", 1);     // SYS_STATUS exists in common.xml
    input.addProperty("systemId", 1);
    input.addProperty("componentId", 1);
    input.addProperty("sequence", 7);

    input.addProperty("onboard_control_sensors_present", 1);
    input.addProperty("onboard_control_sensors_enabled", 1);
    input.addProperty("onboard_control_sensors_health", 1);
    input.addProperty("load", 250);
    input.addProperty("voltage_battery", 12000);
    input.addProperty("current_battery", 100);
    input.addProperty("battery_remaining", 90);
    input.addProperty("drop_rate_comm", 0);
    input.addProperty("errors_comm", 0);
    input.addProperty("errors_count1", 0);
    input.addProperty("errors_count2", 0);
    input.addProperty("errors_count3", 0);
    input.addProperty("errors_count4", 0);

    byte[] frame = formatter.parseFromJson(input);
    Assertions.assertNotNull(frame);
    Assertions.assertTrue(frame.length > 0);

    JsonObject output = formatter.parseToJson(frame, ParseMode.IGNORE);
    Assertions.assertNotNull(output);

    JsonObject outHeader = header(output);
    JsonObject outPayload = payload(output);

    Assertions.assertEquals(1, outHeader.get("messageId").getAsInt());
    Assertions.assertEquals(1, outHeader.get("systemId").getAsInt());
    Assertions.assertEquals(1, outHeader.get("componentId").getAsInt());
    Assertions.assertEquals(7, outHeader.get("sequence").getAsInt());

    Assertions.assertEquals(1, outPayload.get("onboard_control_sensors_present").getAsInt());
    Assertions.assertEquals(1, outPayload.get("onboard_control_sensors_enabled").getAsInt());
    Assertions.assertEquals(1, outPayload.get("onboard_control_sensors_health").getAsInt());
    Assertions.assertEquals(250, outPayload.get("load").getAsInt());
    Assertions.assertEquals(12000, outPayload.get("voltage_battery").getAsInt());
    Assertions.assertEquals(100, outPayload.get("current_battery").getAsInt());
    Assertions.assertEquals(90, outPayload.get("battery_remaining").getAsInt());
  }

  @Test
  void invalidJsonMissingMessageIdFails() throws IOException {
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    Assertions.assertNotNull(formatter);

    JsonObject bad = new JsonObject();
    bad.addProperty("systemId", 1);

    Assertions.assertThrowsExactly(IOException.class, () -> formatter.parseFromJson(bad));
  }

  @Test
  void heartbeatAcceptsEnumNames() throws Exception {
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    Assertions.assertNotNull(formatter);

    JsonObject input = new JsonObject();
    input.addProperty("messageId", 0); // HEARTBEAT
    input.addProperty("systemId", 1);
    input.addProperty("componentId", 1);
    input.addProperty("sequence", 1);

    input.addProperty("custom_mode", 0);
    input.addProperty("type", "MAV_TYPE_QUADROTOR");
    input.addProperty("autopilot", "MAV_AUTOPILOT_ARDUPILOTMEGA");
    input.addProperty("base_mode", 0);
    input.addProperty("system_status", "MAV_STATE_ACTIVE");
    input.addProperty("mavlink_version", 3);

    byte[] frame = formatter.parseFromJson(input);
    Assertions.assertNotNull(frame);

    JsonObject output = formatter.parseToJson(frame, ParseMode.IGNORE);
    Assertions.assertNotNull(output);

    JsonObject outPayload = payload(output);

    // These are stable for common.xml, so keeping them is fine.
    Assertions.assertEquals(2, outPayload.get("type").getAsInt());
    Assertions.assertEquals(3, outPayload.get("autopilot").getAsInt());
    Assertions.assertEquals(4, outPayload.get("system_status").getAsInt());
  }

  @Test
  void invalidBinaryDoesNotThrowRuntime() throws IOException {
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    Assertions.assertNotNull(formatter);

    byte[] garbage = "This should not be parsable".getBytes();
    ParsedObject parsed = formatter.parse(garbage, ParseMode.IGNORE);

    Assertions.assertNotNull(parsed);
    Assertions.assertNull(parsed.get("value"));
  }

  @Test
  void envelopeRoundTrip_parseToJson_outputIsAcceptedBy_parseFromJson() throws Exception {
    SchemaConfig schemaConfig = getSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schemaConfig);
    Assertions.assertNotNull(formatter);

    JsonObject legacyInput = new JsonObject();
    legacyInput.addProperty("messageId", 0);
    legacyInput.addProperty("systemId", 1);
    legacyInput.addProperty("componentId", 1);
    legacyInput.addProperty("sequence", 1);
    legacyInput.addProperty("custom_mode", 0);
    legacyInput.addProperty("type", "MAV_TYPE_QUADROTOR");
    legacyInput.addProperty("autopilot", "MAV_AUTOPILOT_ARDUPILOTMEGA");
    legacyInput.addProperty("base_mode", 0);
    legacyInput.addProperty("system_status", "MAV_STATE_ACTIVE");
    legacyInput.addProperty("mavlink_version", 3);

    byte[] frame1 = formatter.parseFromJson(legacyInput);
    JsonObject envelope = formatter.parseToJson(frame1, ParseMode.IGNORE);

    byte[] frame2 = formatter.parseFromJson(envelope);
    JsonObject envelope2 = formatter.parseToJson(frame2, ParseMode.IGNORE);

    Assertions.assertEquals(
        header(envelope).get("messageId").getAsInt(),
        header(envelope2).get("messageId").getAsInt()
    );
  }
}
