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

package io.mapsmessaging.schemas.config.impl.cbc;

import lombok.Getter;

import java.util.Arrays;

public class BitCursor {
  private final byte[] backingArray;
  private final boolean littleEndian;
  @Getter
  private int bitPosition;


  public BitCursor(byte[] data, boolean littleEndian) {
    this.backingArray = data;
    this.littleEndian = littleEndian;
    this.bitPosition = 0;
  }


  public long readUnsigned(int bitCount) {
    long result = 0L;
    for (int bitIndex = 0; bitIndex < bitCount; bitIndex++) {
      int absoluteBitIndex = bitPosition + bitIndex;
      int byteIndex = absoluteBitIndex / 8;
      int bitOffset = 7 - (absoluteBitIndex % 8);
      int bit = (backingArray[byteIndex] >> bitOffset) & 1;
      result = (result << 1) | bit;
    }
    bitPosition += bitCount;
    return result;
  }


  public long readSigned(int bits) {
    if (bits < 1 || bits > 64) throw new IllegalArgumentException("bits " + bits);
    long u = readUnsigned(bits);
    if (bits == 64) return u;                 // already correct bit pattern as long
    long sign = 1L << (bits - 1);             // OK for bits==64 -> shift by 63
    return (u & sign) != 0 ? u - (1L << bits) : u;  // note: (1L<<bits) never hits 64 here
  }


  public byte[] readBytes(int byteCount) {
    alignToNextByte();
    int startByte = bitPosition / 8;
    byte[] slice = Arrays.copyOfRange(backingArray, startByte, startByte + byteCount);
    bitPosition += byteCount * 8;
    return slice;
  }


  public void skip(int bitCount) {
    bitPosition += bitCount;
  }


  public void alignToNextByte() {
    int remainder = bitPosition % 8;
    if (remainder != 0) {
      bitPosition += (8 - remainder);
    }
  }
}