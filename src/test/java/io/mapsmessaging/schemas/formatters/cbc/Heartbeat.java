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

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Heartbeat {
  private int secOfDay;

  // optional struct: location (values in milli-degrees)
  private Integer latitudeMilli;
  private Integer longitudeMilli;

  // signal (first three mandatory)
  private int rsrp;
  private int rsrq;
  private int sinr;

  // optional scalars
  private Integer rssi;      // dB
  private Long cellId;       // unsigned 32

  // optional struct: psmConfig
  private Integer tauT3412;  // 8-bit bitmask
  private Integer actT3324;  // 8-bit bitmask

  // optional struct: edrxConfig
  private Integer edrxCycle; // 4-bit bitmask
  private Integer edrxPtw;   // 4-bit bitmask

  // optional scalar
  private Integer counter;   // 16-bit
}
