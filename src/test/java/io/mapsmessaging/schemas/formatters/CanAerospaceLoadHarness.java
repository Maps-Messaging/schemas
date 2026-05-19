/*
 *
 *     Copyright [ 2020 - 2026 ] [Matthew Buckton]
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package io.mapsmessaging.schemas.formatters;

import com.google.gson.JsonObject;
import io.mapsmessaging.canbus.canaerospace.parser.DataTypeCodec;
import io.mapsmessaging.canbus.canaerospace.schema.CanaerospaceSchemaRegistry;
import io.mapsmessaging.canbus.device.frames.CanFrame;
import io.mapsmessaging.schemas.formatters.impl.CanAerospaceFormatter;

import java.util.List;
import java.util.Random;

public class CanAerospaceLoadHarness {

  private static final long TEST_DURATION_MS = 120_000;

  private static final List<Integer> IDENTIFIERS = List.of(
      300, 301, 302, 303, 304,
      315, 316, 318,
      320, 321,
      1036, 1037, 1040
  );

  private static final List<String> DATA_TYPES = List.of(
      "SHORT",
      "USHORT",
      "LONG",
      "FLOAT",
      "VARIABLE3"
  );

  public static void main(String[] args) throws Exception {

    CanaerospaceSchemaRegistry registry = CanaerospaceSchemaRegistry.loadFromClasspath();
    CanAerospaceFormatter formatter = new CanAerospaceFormatter(registry);

    Random random = new Random();

    long startTime = System.currentTimeMillis();
    long endTime = startTime + TEST_DURATION_MS;

    long count = 0;
    long parseToJsonTime = 0;
    long parseFromJsonTime = 0;

    while (System.currentTimeMillis() < endTime) {

      int canIdentifier = IDENTIFIERS.get(random.nextInt(IDENTIFIERS.size()));
      String dataType = DATA_TYPES.get(random.nextInt(DATA_TYPES.size()));

      Object value = generateValue(random, dataType);

      byte[] packed = buildPackedFrame(
          canIdentifier,
          false,
          random.nextInt(255),
          dataTypeToNumber(dataType),
          0,
          0,
          dataType,
          value
      );

      long t1 = System.nanoTime();
      JsonObject json = formatter.parseToJson(packed, ParseMode.STRICT);
      long t2 = System.nanoTime();


      parseToJsonTime += (t2 - t1);

      count++;
    }

    long durationMs = System.currentTimeMillis() - startTime;

    double opsPerSecond = count / (durationMs / 1000.0);
    double avgToJsonUs = (parseToJsonTime / (double) count) / 1000.0;

    System.out.println("----- RESULTS -----");
    System.out.println("Duration: " + durationMs + " ms");
    System.out.println("Frames processed: " + count);
    System.out.println("Throughput: " + opsPerSecond + " ops/sec");
    System.out.println("parseToJson avg: " + avgToJsonUs + " µs");
  }

  private static byte[] buildPackedFrame(
      int canIdentifier,
      boolean extendedFrame,
      int nodeId,
      int payloadDataTypeNumber,
      int serviceCode,
      int messageCode,
      String schemaDataType,
      Object rawValue
  ) {

    byte[] payload = new byte[8];

    payload[0] = (byte) nodeId;
    payload[1] = (byte) payloadDataTypeNumber;
    payload[2] = (byte) serviceCode;
    payload[3] = (byte) messageCode;

    byte[] dataBytes = DataTypeCodec.encode(schemaDataType, rawValue);
    System.arraycopy(dataBytes, 0, payload, 4, 4);

    CanFrame frame = new CanFrame(canIdentifier, extendedFrame, 8, payload);
    return frame.getRawData();
  }

  private static Object generateValue(Random random, String type) {
    return switch (type) {
      case "SHORT" -> random.nextInt(65536) - 32768;
      case "USHORT" -> random.nextInt(65536);
      case "LONG" -> random.nextInt();
      case "FLOAT" -> random.nextFloat() * 1000.0f;
      case "VARIABLE3" -> random.nextInt(1 << 18) - (1 << 17);
      default -> 0;
    };
  }

  private static int dataTypeToNumber(String type) {
    return switch (type) {
      case "SHORT" -> 6;
      case "USHORT" -> 7;
      case "LONG" -> 3;
      case "FLOAT" -> 2;
      case "VARIABLE3" -> 100;
      default -> 0;
    };
  }
}