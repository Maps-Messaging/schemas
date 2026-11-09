/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package io.mapsmessaging.schemas.formatters.impl.cdr;

import java.io.IOException;
import java.nio.ByteBuffer;

public final class CdrReader {
  private final ByteBuffer buffer;
  private final boolean littleEndian;

  public CdrReader(byte[] payload, boolean littleEndian) {
    this.buffer = ByteBuffer.wrap(payload);
    this.buffer.position(4);
    this.littleEndian = littleEndian;
  }

  public int remaining() {
    return buffer.remaining();
  }

  private void align(int width) throws IOException {
    int padding = (width - ((buffer.position() - 4) % width)) % width;
    if (padding > buffer.remaining()) {
      throw new IOException("Truncated CDR padding");
    }
    buffer.position(buffer.position() + padding);
  }

  public long readInteger(int width, boolean signed) throws IOException {
    align(width);
    if (buffer.remaining() < width) {
      throw new IOException("Truncated CDR value");
    }
    long result = 0;
    for (int i = 0; i < width; i++) {
      long octet = buffer.get() & 0xffL;
      result |= octet << ((littleEndian ? i : width - i - 1) * 8);
    }
    if (signed && width < 8) {
      int shift = 64 - width * 8;
      return (result << shift) >> shift;
    }
    return result;
  }

  public byte[] readBytes(int count) throws IOException {
    if (count < 0 || count > buffer.remaining()) {
      throw new IOException("Truncated CDR data");
    }
    byte[] result = new byte[count];
    buffer.get(result);
    return result;
  }
}
