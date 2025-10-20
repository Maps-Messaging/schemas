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

public final class TestHeartbeatVectors {

  private TestHeartbeatVectors() {
  }

  public static byte[] buildExampleVector(CbcSchemaConfig schema) {
    BitWriter bitWriter = new BitWriter();

    // Header
    bitWriter.writeUnsigned(schema.getMessageKey(), 16);

    // secOfDay (mandatory)
    bitWriter.writeUnsigned(86399L, 17);

    // location (optional struct): presence + fields
    bitWriter.writeUnsigned(1L, 1);                // present
    bitWriter.writeSigned(51512L, 18);             // latitude (51.512 -> *1000)
    bitWriter.writeSigned(-118L, 19);              // longitude (-0.118 -> *1000)

    // signal (first three mandatory)
    bitWriter.writeSigned(-95L, 9);                // rsrp
    bitWriter.writeSigned(-7L, 6);                 // rsrq
    bitWriter.writeSigned(12L, 6);                 // sinr

    // signal.rssi (optional scalar)
    bitWriter.writeUnsigned(1L, 1);                // present
    bitWriter.writeSigned(-80L, 9);

    // signal.cellId (optional scalar)
    bitWriter.writeUnsigned(1L, 1);                // present
    bitWriter.writeUnsigned(0x01ABCDEF, 32);

    // psmConfig (optional struct)
    bitWriter.writeUnsigned(1L, 1);                // present
    bitWriter.writeUnsigned(0xAFL, 8);             // tauT3412
    bitWriter.writeUnsigned(0x1CL, 8);             // actT3324

    // edrxConfig (optional struct)
    bitWriter.writeUnsigned(1L, 1);                // present
    bitWriter.writeUnsigned(0xAL, 4);              // edrxCycle
    bitWriter.writeUnsigned(0x3L, 4);              // edrxPtw

    // counter (optional scalar)
    bitWriter.writeUnsigned(1L, 1);                // present
    bitWriter.writeUnsigned(65535L, 16);

    return bitWriter.toByteArray();
  }
}
