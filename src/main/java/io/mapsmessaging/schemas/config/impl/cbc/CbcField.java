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


import java.util.Objects;

public final class CbcField {

  private CbcField() {
  }

  public static FieldSpecification of(
      String name, PrimitiveType type, int bitWidth, boolean signed) {
    return FieldSpecification.builder()
        .fieldName(name)
        .primitiveType(type)
        .bitWidth(bitWidth)
        .signed(signed)
        .build();
  }

  public static FieldSpecification fixed(
      String name, int bitWidth, boolean signed, double scale, double offset) {
    return FieldSpecification.builder()
        .fieldName(name)
        .primitiveType(PrimitiveType.FLOAT_FIXED)
        .bitWidth(bitWidth)
        .signed(signed)
        .scale(scale)
        .offset(offset)
        .build();
  }

  public static FieldSpecification bytes(String name, int byteCount) {
    Objects.requireNonNull(name);
    return FieldSpecification.builder()
        .fieldName(name)
        .primitiveType(PrimitiveType.BYTES)
        .bitWidth(byteCount * 8)
        .signed(false)
        .build();
  }
}
