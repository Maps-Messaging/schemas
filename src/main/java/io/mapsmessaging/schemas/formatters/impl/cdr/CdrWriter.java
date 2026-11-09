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

import java.io.ByteArrayOutputStream;

public final class CdrWriter {
    private final ByteArrayOutputStream output = new ByteArrayOutputStream();
    private final boolean littleEndian;

    public CdrWriter(boolean littleEndian) {
      this.littleEndian = littleEndian;
      output.write(0);
      output.write(littleEndian ? 1 : 0);
      output.write(0);
      output.write(0);
    }

    private void align(int width) {
      while ((output.size() - 4) % width != 0) {
        output.write(0);
      }
    }

    public void writeInteger(long value, int width) {
      align(width);
      for (int i = 0; i < width; i++) {
        int shift = (littleEndian ? i : width - i - 1) * 8;
        output.write((int) (value >>> shift) & 0xff);
      }
    }

    public void writeBytes(byte[] data) {
      output.writeBytes(data);
    }

    public byte[] toByteArray() {
      return output.toByteArray();
    }
  }


