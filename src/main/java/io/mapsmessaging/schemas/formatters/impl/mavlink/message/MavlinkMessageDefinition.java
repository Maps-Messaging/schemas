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

package io.mapsmessaging.schemas.formatters.impl.mavlink.message;

import io.mapsmessaging.schemas.formatters.impl.mavlink.message.fields.MavlinkFieldDefinition;
import io.mapsmessaging.schemas.formatters.impl.mavlink.message.fields.MavlinkWireType;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class MavlinkMessageDefinition {
  private int messageId;
  private String name;
  private String description;
  private List<MavlinkFieldDefinition> fields = new ArrayList<>();
  private int extraCrc;

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("MavlinkMessageDefinition [messageId=")
        .append(messageId)
        .append(", name=")
        .append(name)
        .append(", description=")
        .append(description)
        .append("\n");
    for (MavlinkFieldDefinition field : fields) {
      builder.append(field.toString()).append("\n");
    }

    return builder.toString();
  }


  private static List<MavlinkFieldDefinition> getSortedFields(List<MavlinkFieldDefinition> xmlOrdered) {
    List<MavlinkFieldDefinition> sorted = new ArrayList<>(xmlOrdered);
    sorted.sort((a, b) -> {
      int sa = a.getWireType().getSizeInBytes();
      int sb = b.getWireType().getSizeInBytes();
      // bigger first
      return Integer.compare(sb, sa);
    });

    return sorted;
  }

  public void setXmlOrderedFields(List<MavlinkFieldDefinition> xmlOrdered) {
    fields = getSortedFields(xmlOrdered);
    computeExtraCrc();
  }

  private void computeExtraCrc() {
    X25Crc crc = new X25Crc();
    crc.reset();
    crcCharArray(crc, getName().toCharArray());
    for (MavlinkFieldDefinition field : fields) {
      if (!field.isExtension()) {
        MavlinkWireType fieldType = MavlinkWireType.fromXmlType(field.getType());
        crcCharArray(crc, fieldType.getWireName().toCharArray());
        crcCharArray(crc, field.getName().toCharArray());
        if (field.isArray()) crc.update(field.getArrayLength() & 0xFF);
      }
    }
    int val = crc.getRawCrc();
    // MAVLink extra CRC uses the *raw low byte* before xor-out
    extraCrc = ((val & 0xFF) ^ ((val >> 8) & 0xff)) & 0xff;
  }

  private void crcCharArray(X25Crc crc, char[] charArray) {
    for (char c : charArray) {
      crc.update((byte) c);
    }
    crc.update(' ');
  }

}