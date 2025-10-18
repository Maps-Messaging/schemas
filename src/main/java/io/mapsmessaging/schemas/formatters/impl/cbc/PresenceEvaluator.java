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

import java.util.Map;


public class PresenceEvaluator {

  public static boolean isPresent(FieldSpecification fieldSpecification, Map<String, Object> knownValues) {
    String expression = fieldSpecification.getPresenceCondition();
    if (expression == null || expression.isEmpty()) {
      return true;
    }
// Extremely small expression support: "fieldName == 1" or "fieldName != 0"
// Replace with your real expression engine if needed
    String[] parts = expression.split("\\s+");
    if (parts.length != 3) {
      return true;
    }
    String leftName = parts[0];
    String operator = parts[1];
    String rightLiteral = parts[2];
    Object leftValue = knownValues.get(leftName);
    if (!(leftValue instanceof Number)) {
      return true;
    }
    long leftLong = ((Number) leftValue).longValue();
    long rightLong = Long.parseLong(rightLiteral);
    switch (operator) {
      case "==":
        return leftLong == rightLong;
      case "!=":
        return leftLong != rightLong;
      case ">":
        return leftLong > rightLong;
      case ">=":
        return leftLong >= rightLong;
      case "<":
        return leftLong < rightLong;
      case "<=":
        return leftLong <= rightLong;
      default:
        return true;
    }
  }
}