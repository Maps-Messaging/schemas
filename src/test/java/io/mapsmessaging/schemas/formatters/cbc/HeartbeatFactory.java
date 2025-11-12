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

import java.util.Random;

public final class HeartbeatFactory {
  private HeartbeatFactory() {
  }

  public static Heartbeat random(Random rng) {
    Heartbeat.HeartbeatBuilder b = Heartbeat.builder();

    b.secOfDay(rng.nextInt(1 << 17));

    if (rng.nextBoolean()) {
      int lat = rng.nextInt(1 << 17);
      if (rng.nextBoolean()) lat = -lat;
      int lon = rng.nextInt(1 << 18);
      if (rng.nextBoolean()) lon = -lon;
      b.latitudeMilli(lat);
      b.longitudeMilli(lon);
    }

    b.rsrp(rng.nextInt(1 << 8) - 128);
    b.rsrq(rng.nextInt(1 << 5) - 16);
    b.sinr(rng.nextInt(1 << 5) - 16);

    if (rng.nextBoolean()) b.rssi(rng.nextInt(1 << 8) - 128);
    if (rng.nextBoolean()) b.cellId((long) (Math.abs(rng.nextInt())));

    if (rng.nextBoolean()) {
      b.tauT3412(rng.nextInt(256));
      b.actT3324(rng.nextInt(256));
    }

    if (rng.nextBoolean()) {
      b.edrxCycle(rng.nextInt(16));
      b.edrxPtw(rng.nextInt(16));
    }

    if (rng.nextBoolean()) b.counter(rng.nextInt(1 << 16));

    return b.build();
  }
}
