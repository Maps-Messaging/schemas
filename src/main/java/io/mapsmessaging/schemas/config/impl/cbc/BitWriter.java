package io.mapsmessaging.schemas.config.impl.cbc;

import java.io.ByteArrayOutputStream;

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
