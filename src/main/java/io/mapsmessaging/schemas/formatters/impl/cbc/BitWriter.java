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

import io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class BitWriter {
  private final ByteArrayOutputStream byteArrayOutputStream;
  private int currentByte;
  private int bitsFilled;

  public BitWriter() {
    this.byteArrayOutputStream = new ByteArrayOutputStream();
    this.currentByte = 0;
    this.bitsFilled = 0;
  }

  public void writeUnsigned(long value, int bitCount) {
    if (bitCount < 1 || bitCount > 64) {
      throw new IllegalArgumentException("Invalid bitCount: " + bitCount);
    }

    for (int bitIndex = 0; bitIndex < bitCount; bitIndex++) {
      int bit = (int) ((value >> bitIndex) & 1L);
      writeBit(bit);
    }
  }

  private void writeBit(int bit) {
    currentByte = (currentByte << 1) | (bit & 1);
    bitsFilled++;
    if (bitsFilled == 8) {
      byteArrayOutputStream.write(currentByte & 0xFF);
      currentByte = 0;
      bitsFilled = 0;
    }
  }

  // WRITE: variable-length string
  public void writeString(FieldSpecification f, BitWriter w, Object value) {
    boolean fixed = Boolean.TRUE.equals(f.getFixed());
    int size = f.getSize();
    String s = Objects.toString(value, "");
    byte[] ascii = s.getBytes(StandardCharsets.US_ASCII);

    if (fixed) {
      byte[] out = new byte[size];
      System.arraycopy(ascii, 0, out, 0, Math.min(ascii.length, size));
      w.writeRawBytes(out);                               // raw bytes, writer aligns internally
    } else {
      int len = Math.min(ascii.length, size);
      w.alignToNextByte();                                // force byte boundary
      w.writeUnsigned(len, 8);                            // length byte
      byte[] out = new byte[len];
      System.arraycopy(ascii, 0, out, 0, len);
      w.writeRawBytes(out);                               // raw bytes
    }
  }

  public void writeSigned(long value, int bitCount) {
    if (bitCount < 1 || bitCount > 64) throw new IllegalArgumentException("bits " + bitCount);
    long mask = (bitCount == 64) ? -1L : ((1L << bitCount) - 1L);
    writeUnsigned(value & mask, bitCount);
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
