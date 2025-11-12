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

public class CbcStream {

  protected int reqSizeBits(io.mapsmessaging.schemas.config.impl.cbc.FieldSpecification f) {
    Integer s = f.getSize();
    if (s == null) throw new IllegalStateException("Field '" + f.getName() + "' requires size");
    return s;
  }

  protected void requireFitsBits(long code, int bits, FieldSpecification field) {
    long max = (bits == 64) ? -1L : ((1L << bits) - 1L);
    if (bits < 64 && (code < 0 || code > max)) {
      throw new IllegalArgumentException("Enum code " + code + " exceeds " + bits + " bits for '" + field.getName() + "'");
    }
  }

  // --- enum helpers ---
  protected boolean hasEnum(FieldSpecification field) {
    return field.getEnumTable() != null && !field.getEnumTable().isEmpty();
  }


  protected double evalEncalc(String expr, long v) {
    String e = expr.trim();
    if (e.startsWith("v/")) {
      double denom = Double.parseDouble(e.substring(2).trim());
      return v / denom;
    } else if (e.startsWith("v*")) {
      double mult = Double.parseDouble(e.substring(2).trim());
      return v * mult;
    } else if (e.startsWith("v+")) {
      double add = Double.parseDouble(e.substring(2).trim());
      return v + add;
    } else if (e.startsWith("v-")) {
      double sub = Double.parseDouble(e.substring(2).trim());
      return v - sub;
    }
    throw new IllegalArgumentException("Unsupported encalc expression: " + expr);
  }

  protected double evalEncalc(String expr, double v) {
    String e = expr.trim();
    if (e.startsWith("v/")) {
      double denom = Double.parseDouble(e.substring(2).trim());
      return v / denom;
    } else if (e.startsWith("v*")) {
      double mult = Double.parseDouble(e.substring(2).trim());
      return v * mult;
    } else if (e.startsWith("v+")) {
      double add = Double.parseDouble(e.substring(2).trim());
      return v + add;
    } else if (e.startsWith("v-")) {
      double sub = Double.parseDouble(e.substring(2).trim());
      return v - sub;
    }
    throw new IllegalArgumentException("Unsupported encalc expression: " + expr);
  }

}
