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

public class BitReader {
  private final byte[] backingArray;

  @Getter
  private int bitPosition;

  public BitReader(byte[] data) {
    this.backingArray = data;
    this.bitPosition = 0;
  }

  @Override
  public String toString() {
    int byteIndex = Math.min(bitPosition / 8, Math.max(0, backingArray.length - 1));
    int bitInByte = bitPosition % 8;
    int val = (byteIndex >= 0 && byteIndex < backingArray.length) ? (backingArray[byteIndex] & 0xFF) : -1;
    return "posBits=" + bitPosition + " posInByte=" + bitInByte + " len=" + backingArray.length + " curByte=0x" + Integer.toHexString(val);
  }

  public long readUnsigned(int bitCount) {
    if (bitCount < 1 || bitCount > 64) {
      throw new IllegalArgumentException("bitCount " + bitCount);
    }
    ensureAvailable(bitCount);

    long result = 0L;
    // LSB-first fields: accumulate into increasing bit positions
    for (int i = 0; i < bitCount; i++) {
      int bit = readNextBit();
      result |= ((long) bit) << i;
    }
    return result;
  }

  public long readSigned(int bits) {
    if (bits < 1 || bits > 64) throw new IllegalArgumentException("bits " + bits);
    long u = readUnsigned(bits);
    if (bits == 64) return u; // already full-width pattern
    long sign = 1L << (bits - 1);
    return (u & sign) != 0 ? u - (1L << bits) : u;
  }

  public byte[] readBytes(int byteCount) {
    if (byteCount < 0) throw new IllegalArgumentException("byteCount " + byteCount);
    alignToNextByte();
    int startByte = bitPosition / 8;
    int endByte = startByte + byteCount;
    if (endByte > backingArray.length) {
      throw new IndexOutOfBoundsException("readBytes beyond end: need " + byteCount + " bytes at pos " + startByte + " of " + backingArray.length);
    }
    byte[] slice = Arrays.copyOfRange(backingArray, startByte, endByte);
    bitPosition += byteCount * 8;
    return slice;
  }

  public void alignToNextByte() {
    int remainder = bitPosition % 8;
    if (remainder != 0) {
      bitPosition += (8 - remainder);
    }
  }

  // ---- helpers ----

  private int readNextBit() {
    int absoluteBitIndex = bitPosition++;
    int byteIndex = absoluteBitIndex / 8;
    if (byteIndex >= backingArray.length) {
      throw new IndexOutOfBoundsException("Read past end at bit " + absoluteBitIndex + " of " + (backingArray.length * 8));
    }
    int bitOffset = 7 - (absoluteBitIndex % 8);     // next serial bit in stream (MSB first within stored byte)
    return ((backingArray[byteIndex] & 0xFF) >> bitOffset) & 1;
  }

  private void ensureAvailable(int bitCount) {
    int remaining = backingArray.length * 8 - bitPosition;
    if (bitCount > remaining) {
      throw new IndexOutOfBoundsException("Need " + bitCount + " bits, only " + remaining + " remain");
    }
  }
}
