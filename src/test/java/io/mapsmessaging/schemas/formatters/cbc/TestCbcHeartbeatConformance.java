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

package io.mapsmessaging.schemas.formatters.cbc;


import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.impl.CbcSchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.impl.CbcFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;

public class TestCbcHeartbeatConformance {

  public static final String INMARSAT_HEARTBEAT_JSON = """
      {
        "application": "viasatNtnProofOfConcept",
        "version": "1.1",
        "description": "Viasat NB-NTN proof of concept for efficient binary data transport",
        "messages": [{
          "description": "A basic health/config status message for NB-NTN devices.",
          "direction": "UPLINK",
          "name": "heartbeat",
          "messageKey": 65280,
          "fields": [
            { "name": "secOfDay", "type": "uint", "size": 17 },
            { "name": "location", "type": "struct", "optional": true, "fields": [
              { "name": "latitude",  "type": "int", "size": 18, "decalc": "v/1000", "encalc": "v*1000" },
              { "name": "longitude", "type": "int", "size": 19, "decalc": "v/1000", "encalc": "v*1000" }
            ]},
            { "name": "signal", "type": "struct", "fields": [
              { "name": "rsrp",  "type": "int", "size": 9  },
              { "name": "rsrq",  "type": "int", "size": 6  },
              { "name": "sinr",  "type": "int", "size": 6  },
              { "name": "rssi",  "type": "int", "size": 9,  "optional": true },
              { "name": "cellId","type": "uint","size": 32, "optional": true }
            ]},
            { "name": "psmConfig", "type": "struct", "optional": true, "fields": [
              { "name": "tauT3412", "type": "bitmask", "size": 8 },
              { "name": "actT3324", "type": "bitmask", "size": 8 }
            ]},
            { "name": "edrxConfig", "type": "struct", "optional": true, "fields": [
              { "name": "edrxCycle", "type": "bitmask", "size": 4 },
              { "name": "edrxPtw",   "type": "bitmask", "size": 4 }
            ]},
            { "name": "counter", "type": "uint", "size": 16, "optional": true }
          ]
        }]
      }
      """;

  // Golden payload (hex) produced by our BitWriter (or by the reference encoder once you have it)
  // secOfDay=86399, lat=51.512, lon=-0.118, rsrp=-95, rsrq=-7, sinr=12, rssi=-80, cellId=0x01ABCDEF,
  // tau=0xAF, act=0x1C, edrxCycle=0xA, edrxPtw=0x3, counter=65535
  private static final String GOLDEN_HEX =
      "FF00" +    // messageKey 65280
          "1FFFF" +   // secOfDay 17 bits -> packed with following bits; this hex is illustrative placeholder
          "...";     // Replace with real hex once you generate from your vector builder or reference codec
  private static final String HEARTBEAT_JSON = "";

  @Test
  void parse_and_roundtrip_heartbeat() throws Exception {

    CbcSchemaConfig schema = new CbcSchemaConfig();
    JsonObject obj = JsonParser.parseString(INMARSAT_HEARTBEAT_JSON).getAsJsonObject();

    JsonArray arr = obj.getAsJsonArray("messages");
    JsonObject msg1 = arr.get(0).getAsJsonObject();
    schema.setSchema(msg1);
    CbcFormatter formatter = (CbcFormatter) MessageFormatterFactory.getInstance().getFormatter(schema);

    byte[] payload = TestHeartbeatVectors.buildExampleVector(schema); // our local vector builder
    ParsedObject parsed = formatter.parse(payload, ParseMode.IGNORE);

    // core fields
    Assertions.assertEquals(65280L, ((Number) parsed.get("messageKey")).longValue());
    Assertions.assertEquals(86399L, ((Number) parsed.get("secOfDay")).longValue());
    Assertions.assertEquals(51.512, ((Number) parsed.get("location.latitude")).doubleValue(), 1e-3);
    Assertions.assertEquals(-0.118, ((Number) parsed.get("location.longitude")).doubleValue(), 1e-3);

    Assertions.assertEquals(-95L, ((Number) parsed.get("signal.rsrp")).longValue());
    Assertions.assertEquals(-7L, ((Number) parsed.get("signal.rsrq")).longValue());
    Assertions.assertEquals(12L, ((Number) parsed.get("signal.sinr")).longValue());
    Assertions.assertEquals(-80L, ((Number) parsed.get("signal.rssi")).longValue());
    Assertions.assertEquals(0x01ABCDEF, ((Number) parsed.get("signal.cellId")).longValue());
    Assertions.assertEquals(0xAF, ((Number) parsed.get("psmConfig.tauT3412")).longValue());
    Assertions.assertEquals(0x1C, ((Number) parsed.get("psmConfig.actT3324")).longValue());
    Assertions.assertEquals(0xA, ((Number) parsed.get("edrxConfig.edrxCycle")).longValue());
    Assertions.assertEquals(0x3, ((Number) parsed.get("edrxConfig.edrxPtw")).longValue());
    Assertions.assertEquals(65535L, ((Number) parsed.get("counter")).longValue());

    // JSON view checks
    JsonObject json = formatter.parseToJson(payload, ParseMode.IGNORE);
    Assertions.assertEquals(86399, json.get("secOfDay").getAsInt());

    // Round-trip
    Map<String, Object> valueMap = jsonToMap(json);
    byte[] reencoded = ((CbcFormatter) new CbcFormatter().getInstance(schema, null)).toBytes(valueMap);
    Assertions.assertArrayEquals(payload, reencoded);
  }


  void decode_golden_from_reference() throws Exception {
    CbcSchemaConfig schema = new CbcSchemaConfig();
    schema.setSchema(HEARTBEAT_JSON);
    CbcFormatter formatter = (CbcFormatter) MessageFormatterFactory.getInstance().getFormatter(schema);


    byte[] golden = HexUtil.parseHex(GOLDEN_HEX.replace(" ", "").replace("\n", ""));
    ParsedObject parsed = formatter.parse(golden, ParseMode.IGNORE);

    // Spot check a few fields
    Assertions.assertEquals(65280L, ((Number) parsed.get("messageTypeId")).longValue());
    Assertions.assertTrue(((Number) parsed.get("secOfDay")).longValue() > 0);
  }

  private static Map<String, Object> jsonToMap(JsonObject json) {
    return new com.google.gson.Gson().fromJson(json, Map.class);
  }
}
