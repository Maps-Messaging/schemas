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
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.MessageFormatterFactory;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Random;

import static io.mapsmessaging.schemas.formatters.cbc.TestCbcHeartbeatConformance.INMARSAT_HEARTBEAT_JSON;


class TestCbcFuzzHeartbeat {

  private static CbcSchemaConfig loadSchema() {
    CbcSchemaConfig schema = new CbcSchemaConfig();
    JsonObject obj = JsonParser.parseString(INMARSAT_HEARTBEAT_JSON).getAsJsonObject();
    ;
    JsonArray arr = obj.getAsJsonArray("messages");
    JsonObject msg1 = arr.get(0).getAsJsonObject();
    schema.setSchema(msg1.toString());
    return schema;
  }

  @Test
  void roundTrip_random_examples() throws IOException {
    CbcSchemaConfig schema = loadSchema();
    MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(schema);
    Random rng = new Random(42);

    for (int i = 0; i < 500; i++) {
      Heartbeat hb = HeartbeatFactory.random(rng);
      byte[] packed = HeartBeatPacker.pack(hb, schema);

      // parse to JSON and to ParsedObject
      JsonObject json = formatter.parseToJson(packed, ParseMode.IGNORE);
      ParsedObject po = formatter.parse(packed, ParseMode.IGNORE);

      // header
      Assertions.assertEquals(65280L, json.get("messageKey").getAsLong());

      // mandatory
      Assertions.assertEquals(hb.getSecOfDay(), json.get("secOfDay").getAsInt());

      // location optional
      if (hb.getLatitudeMilli() != null && hb.getLongitudeMilli() != null) {
        JsonObject location = json.get("location").getAsJsonObject();
        double lat = location.get("latitude").getAsDouble();
        double lon = location.get("longitude").getAsDouble();
        Assertions.assertEquals(hb.getLatitudeMilli() / 1000.0, lat, 1e-6);
        Assertions.assertEquals(hb.getLongitudeMilli() / 1000.0, lon, 1e-6);
      } else {
        Assertions.assertFalse(json.has("location"));
      }

      // signal mandatory
      JsonObject signal = json.get("signal").getAsJsonObject();
      Assertions.assertNotNull(signal);
      Assertions.assertEquals(hb.getRsrp(), signal.get("rsrp").getAsInt());
      Assertions.assertEquals(hb.getRsrq(), signal.get("rsrq").getAsInt());
      Assertions.assertEquals(hb.getSinr(), signal.get("sinr").getAsInt());

      // rssi optional
      if (hb.getRssi() != null) {
        Assertions.assertEquals(hb.getRssi().intValue(), signal.get("rssi").getAsInt());
      } else {
        Assertions.assertNull(signal.get("rssi"));
      }

      // cellId optional
      if (hb.getCellId() != null) {
        Assertions.assertEquals(hb.getCellId().longValue(), signal.get("cellId").getAsLong());
      } else {
        Assertions.assertNull(signal.get("cellId"));
      }

      // psmConfig optional
      if (json.has("psmConfig")) {
        JsonObject psmConfig = json.get("psmConfig").getAsJsonObject();
        if (hb.getTauT3412() != null && hb.getActT3324() != null) {
          Assertions.assertNotNull(psmConfig);
          Assertions.assertEquals(hb.getTauT3412().intValue(), psmConfig.get("tauT3412").getAsInt());
          Assertions.assertEquals(hb.getActT3324().intValue(), psmConfig.get("actT3324").getAsInt());
        }
      }

      // edrxConfig optional
      if (json.has("edrxConfig")) {

        JsonObject edrxConfig = json.get("edrxConfig").getAsJsonObject();
        if (hb.getEdrxCycle() != null && hb.getEdrxPtw() != null) {
          Assertions.assertNotNull(edrxConfig);
          Assertions.assertEquals(hb.getEdrxCycle().intValue(), edrxConfig.get("edrxCycle").getAsInt());
          Assertions.assertEquals(hb.getEdrxPtw().intValue(), edrxConfig.get("edrxPtw").getAsInt());
        }
      }

      // counter optional
      if (hb.getCounter() != null) {
        Assertions.assertEquals(hb.getCounter().intValue(), json.get("counter").getAsInt());
      } else {
        Assertions.assertNull(json.get("counter"));
      }

      // also ensure map resolver resolves flat keys
      Assertions.assertEquals(hb.getRsrp(), ((Number) po.get("signal.rsrp")).intValue());
    }
  }

}
