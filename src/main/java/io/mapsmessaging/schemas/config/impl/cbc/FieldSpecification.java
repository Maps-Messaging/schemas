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

import com.google.gson.JsonObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FieldSpecification {
  private String fieldName;
  private PrimitiveType primitiveType;
  private int bitWidth;
  private boolean signed;
  @Builder.Default
  private double scale = 1.0d;
  @Builder.Default
  private double offset = 0.0d;
  @Builder.Default
  private String unit = null;
  @Builder.Default
  private Map<Integer, String> enumerationTable = null;
  @Builder.Default
  private String presenceCondition = null;
  @Builder.Default
  private String group = null;
  @Builder.Default
  private boolean byteAlignAfter = false;


  public JsonObject toJson() {
    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("fieldName", fieldName);
    jsonObject.addProperty("primitiveType", primitiveType.name());
    jsonObject.addProperty("bitWidth", bitWidth);
    jsonObject.addProperty("signed", signed);
    if (scale != 1.0d) jsonObject.addProperty("scale", scale);
    if (offset != 0.0d) jsonObject.addProperty("offset", offset);
    if (unit != null) jsonObject.addProperty("unit", unit);
    if (presenceCondition != null) jsonObject.addProperty("presenceCondition", presenceCondition);
    if (group != null) jsonObject.addProperty("group", group);
    if (byteAlignAfter) jsonObject.addProperty("byteAlignAfter", true);
    if (enumerationTable != null && !enumerationTable.isEmpty()) {
      JsonObject enumObject = new JsonObject();
      for (Map.Entry<Integer, String> entry : enumerationTable.entrySet()) {
        enumObject.addProperty(String.valueOf(entry.getKey()), entry.getValue());
      }
      jsonObject.add("enumerationTable", enumObject);
    }
    return jsonObject;
  }


  public static FieldSpecification from(Map<String, Object> map) {
    FieldSpecification.FieldSpecificationBuilder builder = FieldSpecification.builder();
    Object nameObject = map.get("fieldName");
    if (nameObject != null) builder.fieldName(String.valueOf(nameObject));
    Object typeObject = map.get("primitiveType");
    if (typeObject != null) builder.primitiveType(PrimitiveType.valueOf(String.valueOf(typeObject)));
    Object bitWidthObject = map.get("bitWidth");
    if (bitWidthObject != null) builder.bitWidth(parseInt(String.valueOf(bitWidthObject)));
    Object signedObject = map.get("signed");
    if (signedObject != null) builder.signed(Boolean.parseBoolean(String.valueOf(signedObject)));
    Object scaleObject = map.get("scale");
    if (scaleObject != null) builder.scale(Double.parseDouble(String.valueOf(scaleObject)));
    Object offsetObject = map.get("offset");
    if (offsetObject != null) builder.offset(Double.parseDouble(String.valueOf(offsetObject)));
    Object unitObject = map.get("unit");
    if (unitObject != null) builder.unit(String.valueOf(unitObject));
    Object presenceObject = map.get("presenceCondition");
    if (presenceObject != null) builder.presenceCondition(String.valueOf(presenceObject));
    Object groupObject = map.get("group");
    if (groupObject != null) builder.group(String.valueOf(groupObject));
    Object alignObject = map.get("byteAlignAfter");
    if (alignObject != null) builder.byteAlignAfter(Boolean.parseBoolean(String.valueOf(alignObject)));
// enumerationTable optional: expects Map<Integer,String>; callers can set after construction if needed
    return builder.build();
  }

  private static int parseInt(String string) {
    if (string.contains(".")) {
      string = string.substring(0, string.indexOf("."));
    }
    return Integer.parseInt(string);
  }

}
