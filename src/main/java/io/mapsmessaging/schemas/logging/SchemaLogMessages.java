/*
 *
 *     Copyright [ 2020 - 2023 ] [Matthew Buckton]
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

package io.mapsmessaging.schemas.logging;

import io.mapsmessaging.logging.Category;
import io.mapsmessaging.logging.LEVEL;
import io.mapsmessaging.logging.LogMessage;
import lombok.Getter;

public enum SchemaLogMessages implements LogMessage {

  //<editor-fold desc="Config messages">
  SCHEMA_CONFIG_FACTORY_INVALID_CONFIG(LEVEL.WARN, SCHEMA_CATEGORY.CONFIG, "Unknown schema config found"),
  SCHEMA_CONFIG_FACTORY_SCHEMA_NOT_FOUND(LEVEL.WARN, SCHEMA_CATEGORY.CONFIG, "Unknown format in config, {}"),
  AVRO_SCHEMA_NOT_DEFINED(LEVEL.WARN, SCHEMA_CATEGORY.CONFIG, "No {} Schema found for Schema Id {}"),
  CSV_HEADER_NOT_DEFINED(LEVEL.WARN, SCHEMA_CATEGORY.CONFIG, "No {} header found for Schema Id {}"),
  NATIVE_TYPE_UNKNOWN(LEVEL.WARN, SCHEMA_CATEGORY.CONFIG, "Type for {} not defined for Schema Id {}"),
  PROTOBUF_DESCRIPTOR_NOT_DEFINED(LEVEL.WARN, SCHEMA_CATEGORY.CONFIG, "No {} descriptor defined for Schema Id {}"),
  PROTOBUF_MESSAGE_NAME_NOT_DEFINED(LEVEL.WARN, SCHEMA_CATEGORY.CONFIG, "No {} message name defined for Schema Id {}"),
  //</editor-fold>

  //<editor-fold desc="Formatter specific log messages">
  AVRO_PARSE_EXCEPTION(LEVEL.ERROR, SCHEMA_CATEGORY.FORMATTER, "Avro format raised exception during parsing, {}"),
  FORMATTER_UNEXPECTED_OBJECT(LEVEL.ERROR, SCHEMA_CATEGORY.FORMATTER, "{} formatter unable to parse object, unexpected data {}"),
  PROTOBUF_PARSE_EXCEPTION(LEVEL.ERROR, SCHEMA_CATEGORY.FORMATTER, "Protobuf format raised exception during parsing {}"),
  XML_CONFIGURATION_EXCEPTION(LEVEL.ERROR, SCHEMA_CATEGORY.FORMATTER, "XML formatter raised exception during construction {}"),
  XML_PARSE_EXCEPTION(LEVEL.ERROR, SCHEMA_CATEGORY.FORMATTER, "XML formatter raised exception during parsing {}"),
  //</editor-fold>
  ;


  private final @Getter String message;
  private final @Getter LEVEL level;
  private final @Getter Category category;
  private final @Getter int parameterCount;


  SchemaLogMessages(LEVEL level, SCHEMA_CATEGORY category, String message) {
    this.message = message;
    this.level = level;
    this.category = category;
    int location = message.indexOf("{}");
    int count = 0;
    while (location != -1) {
      count++;
      location = message.indexOf("{}", location + 2);
    }
    this.parameterCount = count;
  }

  public enum SCHEMA_CATEGORY implements Category {
    CONFIG("Config"),
    FORMATTER("Formatter"),
    REPOSITORY("Repository");

    private final @Getter String description;

    SCHEMA_CATEGORY(String description) {
      this.description = description;
    }

    public String getDivision() {
      return "Schema";
    }
  }
}
