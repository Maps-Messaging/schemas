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

import java.io.ByteArrayOutputStream;

public class BitWriter {
  private final ByteArrayOutputStream byteArrayOutputStream;
  private int currentByte;
  private int bitsFilled;
  private final boolean littleEndian;


  public BitWriter(boolean littleEndian) {
    this.byteArrayOutputStream = new ByteArrayOutputStream();
    this.currentByte = 0;
    this.bitsFilled = 0;
    this.littleEndian = littleEndian;
  }


  public void writeUnsigned(long value, int bitCount) {
    for (int bitIndex = bitCount - 1; bitIndex >= 0; bitIndex--) {
      int bit = (int) ((value >> bitIndex) & 1L);
      currentByte = (currentByte << 1) | bit;
      bitsFilled++;
      if (bitsFilled == 8) {
        byteArrayOutputStream.write(currentByte & 0xFF);
        currentByte = 0;
        bitsFilled = 0;
      }
    }
  }


  public void writeSigned(long value, int bits) {
    if (bits < 1 || bits > 64) throw new IllegalArgumentException("bits " + bits);
    long mask = (bits == 64) ? -1L : ((1L << bits) - 1L);
    writeUnsigned(value & mask, bits);
  }

  public void writeRawBytes(byte[] data) {
    alignToNextByte();
    byteArrayOutputStream.writeBytes(data);
  }


  public void alignToNextByte() {
    if (bitsFilled > 0) {
      int shift = 8 - bitsFilled;
      currentByte = currentByte << shift;
      byteArrayOutputStream.write(currentByte & 0xFF);
      currentByte = 0;
      bitsFilled = 0;
    }
  }


  public byte[] toByteArray() {
    alignToNextByte();
    return byteArrayOutputStream.toByteArray();
  }
}