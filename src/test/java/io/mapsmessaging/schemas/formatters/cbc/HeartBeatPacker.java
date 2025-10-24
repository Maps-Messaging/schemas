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


import io.mapsmessaging.schemas.config.impl.CbcSchemaConfig;
import io.mapsmessaging.schemas.config.impl.cbc.BitWriter;

public final class HeartBeatPacker {
  private HeartBeatPacker() {
  }

  public static byte[] pack(Heartbeat hb, CbcSchemaConfig schema) {
    BitWriter w = new BitWriter();

    if (schema.getCbcFormat().getMessageKey() > 0) {
      w.writeUnsigned(schema.getCbcFormat().getMessageKey(), 16);
    }

    // secOfDay (mandatory)
    w.writeUnsigned(hb.getSecOfDay(), 17);

    // location (optional struct): presence + fields
    boolean hasLocation = hb.getLatitudeMilli() != null && hb.getLongitudeMilli() != null;
    w.writeUnsigned(hasLocation ? 1L : 0L, 1);
    if (hasLocation) {
      w.writeSigned(hb.getLatitudeMilli(), 18);
      w.writeSigned(hb.getLongitudeMilli(), 19);
    }

    // signal mandatory
    w.writeSigned(hb.getRsrp(), 9);
    w.writeSigned(hb.getRsrq(), 6);
    w.writeSigned(hb.getSinr(), 6);

    // rssi (optional)
    boolean hasRssi = hb.getRssi() != null;
    w.writeUnsigned(hasRssi ? 1L : 0L, 1);
    if (hasRssi) w.writeSigned(hb.getRssi(), 9);

    // cellId (optional)
    boolean hasCell = hb.getCellId() != null;
    w.writeUnsigned(hasCell ? 1L : 0L, 1);
    if (hasCell) w.writeUnsigned(hb.getCellId(), 32);

    // psmConfig (optional struct)
    boolean hasPsm = hb.getTauT3412() != null && hb.getActT3324() != null;
    w.writeUnsigned(hasPsm ? 1L : 0L, 1);
    if (hasPsm) {
      w.writeUnsigned(hb.getTauT3412(), 8);
      w.writeUnsigned(hb.getActT3324(), 8);
    }

    // edrxConfig (optional struct)
    boolean hasEdrx = hb.getEdrxCycle() != null && hb.getEdrxPtw() != null;
    w.writeUnsigned(hasEdrx ? 1L : 0L, 1);
    if (hasEdrx) {
      w.writeUnsigned(hb.getEdrxCycle(), 4);
      w.writeUnsigned(hb.getEdrxPtw(), 4);
    }

    // counter (optional)
    boolean hasCounter = hb.getCounter() != null;
    w.writeUnsigned(hasCounter ? 1L : 0L, 1);
    if (hasCounter) w.writeUnsigned(hb.getCounter(), 16);

    return w.toByteArray();
  }
}
