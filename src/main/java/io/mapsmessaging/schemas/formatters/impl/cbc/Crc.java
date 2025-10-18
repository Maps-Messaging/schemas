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

package io.mapsmessaging.schemas.formatters.impl.cbc;


import io.mapsmessaging.schemas.config.impl.cbc.CrcType;

public class Crc {

  static byte[] computeChecksum(byte[] data, CrcType crcType) {
    return switch (crcType) {
      case CRC16_X25 -> crc16X25(data);
      case CRC32_IEEE -> crc32(data);
      default -> new byte[0];
    };
  }


  private static byte[] crc16X25(byte[] data) {
    int crc = 0xFFFF;
    for (byte b : data) {
      crc ^= (b & 0xFF);
      for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
        if ((crc & 1) != 0) {
          crc = (crc >>> 1) ^ 0x8408;
        } else {
          crc = crc >>> 1;
        }
      }
    }
    crc = ~crc & 0xFFFF;
    byte low = (byte) (crc & 0xFF);
    byte high = (byte) ((crc >>> 8) & 0xFF);
    return new byte[]{low, high};
  }


  private static byte[] crc32(byte[] data) {
    int polynomial = 0xEDB88320;
    int crc = 0xFFFFFFFF;
    for (byte b : data) {
      crc ^= (b & 0xFF);
      for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
        if ((crc & 1) != 0) {
          crc = (crc >>> 1) ^ polynomial;
        } else {
          crc = crc >>> 1;
        }
      }
    }
    crc = ~crc;
    byte b0 = (byte) (crc & 0xFF);
    byte b1 = (byte) ((crc >>> 8) & 0xFF);
    byte b2 = (byte) ((crc >>> 16) & 0xFF);
    byte b3 = (byte) ((crc >>> 24) & 0xFF);
    return new byte[]{b3, b2, b1, b0};
  }
}