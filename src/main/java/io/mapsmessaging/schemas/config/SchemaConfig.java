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
package io.mapsmessaging.schemas.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.mapsmessaging.schemas.config.impl.*;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;

@SuppressWarnings("javaarchitecture:S7091")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AvroSchemaConfig.class, name = "avro"),
    @JsonSubTypes.Type(value = CbcSchemaConfig.class, name = "cbc"),
    @JsonSubTypes.Type(value = CborSchemaConfig.class, name = "cbor"),
    @JsonSubTypes.Type(value = CsvSchemaConfig.class, name = "csv"),
    @JsonSubTypes.Type(value = JsonSchemaConfig.class, name = "json"),
    @JsonSubTypes.Type(value = MessagePackSchemaConfig.class, name = "messagePack"),
    @JsonSubTypes.Type(value = NativeSchemaConfig.class, name = "native"),
    @JsonSubTypes.Type(value = ProtoBufSchemaConfig.class, name = "protobuf"),
    @JsonSubTypes.Type(value = RawSchemaConfig.class, name = "raw"),
    @JsonSubTypes.Type(value = XmlSchemaConfig.class, name = "xml")
})

@Schema(description = "Abstract base class for all schema configurations",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "avro", schema = AvroSchemaConfig.class),
        @DiscriminatorMapping(value = "cbc", schema = CbcSchemaConfig.class),
        @DiscriminatorMapping(value = "cbor", schema = CborSchemaConfig.class),
        @DiscriminatorMapping(value = "csv", schema = CsvSchemaConfig.class),
        @DiscriminatorMapping(value = "json", schema = JsonSchemaConfig.class),
        @DiscriminatorMapping(value = "messagePack", schema = MessagePackSchemaConfig.class),
        @DiscriminatorMapping(value = "native", schema = NativeSchemaConfig.class),
        @DiscriminatorMapping(value = "protobuf", schema = ProtoBufSchemaConfig.class),
        @DiscriminatorMapping(value = "raw", schema = RawSchemaConfig.class),
        @DiscriminatorMapping(value = "xml", schema = XmlSchemaConfig.class),
    })

public abstract class SchemaConfig extends XRegistrySchemaVersion {
  protected SchemaConfig(String format) {
    super(format);
  }

  protected SchemaConfig(XRegistrySchemaVersion copyFrom) {
    super(copyFrom);
  }

  public void setVersion(int val) {
    super.setVersion("" + val);
  }

  public void setTitle(String val) {
    setName(val);
  }

  public String getTitle() {
    return getName();
  }

  public abstract SchemaConfig getInstance(XRegistrySchemaVersion config);

  public abstract String getMimeType();

}
