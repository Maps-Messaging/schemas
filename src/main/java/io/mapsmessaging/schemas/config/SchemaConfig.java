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

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.config.impl.*;
import io.swagger.v3.oas.annotations.media.DiscriminatorMapping;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import static io.mapsmessaging.schemas.config.Constants.*;
import static io.mapsmessaging.schemas.config.SchemaConfigFactory.gson;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AvroSchemaConfig.class, name = "avro"),
    @JsonSubTypes.Type(value = CsvSchemaConfig.class, name = "csv"),
    @JsonSubTypes.Type(value = JsonSchemaConfig.class, name = "json"),
    @JsonSubTypes.Type(value = NativeSchemaConfig.class, name = "native"),
    @JsonSubTypes.Type(value = ProtoBufSchemaConfig.class, name = "protobuf"),
    @JsonSubTypes.Type(value = RawSchemaConfig.class, name = "raw"),
    @JsonSubTypes.Type(value = XmlSchemaConfig.class, name = "xml")
})
@Schema(description = "Abstract base class for all schema configurations",
    discriminatorProperty = "type",
    discriminatorMapping = {
        @DiscriminatorMapping(value = "avro", schema = AvroSchemaConfig.class),
        @DiscriminatorMapping(value = "csv", schema = CsvSchemaConfig.class),
        @DiscriminatorMapping(value = "json", schema = JsonSchemaConfig.class),
        @DiscriminatorMapping(value = "native", schema = NativeSchemaConfig.class),
        @DiscriminatorMapping(value = "protobuf", schema = ProtoBufSchemaConfig.class),
        @DiscriminatorMapping(value = "raw", schema = RawSchemaConfig.class),
        @DiscriminatorMapping(value = "xml", schema = XmlSchemaConfig.class)
    })

/**
 * The type Schema config.
 */
public abstract class SchemaConfig implements Serializable {

  /**
   * The Logger to use for all messages
   */
  protected transient Logger logger;

  /**
   * The name of the Formatter that the configuration represents. Typically, set by the class that extends this
   */
  @Getter
  protected final String format;

  @Getter
  @Setter
  protected String title;

  /**
   * The Unique id.
   */
  @Getter
  protected String uniqueId;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  @Getter
  private LocalDateTime creation;

  @Getter
  @Setter
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime expiresAfter;

  @Getter
  @Setter
  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  @JsonSerialize(using = LocalDateTimeSerializer.class)
  @JsonDeserialize(using = LocalDateTimeDeserializer.class)
  private LocalDateTime notBefore;

  @Setter
  @Getter
  private String comments;


  @Setter
  @Getter
  private String version;

  @Setter
  @Getter
  private String source;

  @Setter
  @Getter
  private String mimeType;

  /**
   * This represents the "rt" in the Link-Format
   */
  @Setter
  @Getter
  private String resourceType;


  /**
   * This represents the "if" in the Link-Format
   */
  @Setter
  @Getter
  private String interfaceDescription;

  /**
   * Instantiates a new Schema config.
   *
   * @param format the format
   */
  protected SchemaConfig(String format) {
    this.format = format;
    logger = LoggerFactory.getLogger(SchemaConfig.class);
  }

  /**
   * Instantiates a new Schema config.
   *
   * @param format the formatter name
   * @param config the config map with the defined fields
   */
  protected SchemaConfig(String format, Map<String, Object> config) {
    this(format);
    uniqueId = config.get(io.mapsmessaging.schemas.config.Constants.UUID).toString();
    if (config.containsKey(EXPIRES_AFTER)) {
      expiresAfter = loadDateTime(config, EXPIRES_AFTER);
    }
    if (config.containsKey(NOT_BEFORE)) {
      notBefore = loadDateTime(config, NOT_BEFORE);
    }
    if (config.containsKey(CREATION)) {
      creation = loadDateTime(config, CREATION);
    }
    if (config.containsKey(COMMENTS)) {
      comments = (String) config.get(COMMENTS);
    }
    if (config.containsKey(VERSION)) {
      version = (String) config.get(VERSION);
    }
    if (config.containsKey(SOURCE)) {
      source = (String)  config.get(SOURCE);
    }
    if (config.containsKey(TITLE)) {
      title = (String) config.get(TITLE);
    }
    if (config.containsKey(MIME_TYPE)) {
      mimeType = (String)config.get(MIME_TYPE);
    }
    if (config.containsKey(RESOURCE_TYPE)) {
      resourceType = (String) config.get(RESOURCE_TYPE);
    }
    if (config.containsKey(INTERFACE_DESCRIPTION)) {
      interfaceDescription = (String)config.get(INTERFACE_DESCRIPTION);
    }
  }

  private LocalDateTime loadDateTime(Map<String, Object> config, String key){
    Object val = config.get(key);
    if(val instanceof  String) {
      return LocalDateTime.parse((String)val);
    }
    if(val instanceof LocalDateTime){
      return (LocalDateTime)val;
    }
    return null;
  }

  /**
   * Pack the schema into a JSON representation string.
   *
   * @return the string
   * @throws IOException the io exception
   */
  public String pack() throws IOException {
    return gson.toJson(packtoJSON());
  }

  /**
   * To builds a map of the configuration.
   *
   * @return the map
   * @throws IOException the io exception
   */
  public Map<String, Object> toMap() throws IOException {
    Type type = new TypeToken<Map<String, Object>>() {
    }.getType();
    return gson.fromJson(packtoJSON(), type);
  }

  /**
   * Sets unique id.
   *
   * @param uniqueId the unique id
   */
  public void setUniqueId(UUID uniqueId) {
    this.uniqueId = uniqueId.toString();
    creation = LocalDateTime.now();
  }

  /**
   * Sets unique id.
   *
   * @param uniqueId the unique id
   */
  public void setUniqueId(String uniqueId) {
    this.uniqueId = uniqueId;
    creation = LocalDateTime.now();
  }

  /**
   * Pack data to a json object.
   *
   * @return the json object
   * @throws IOException the io exception
   */
  protected abstract JsonObject packData() throws IOException;

  /**
   * Gets instance from the supplied config map
   *
   * @param config the config
   * @return the instance
   */
  protected abstract SchemaConfig getInstance(Map<String, Object> config);

  /**
   * Pack data.
   *
   * @param jsonObject the json object
   */
  protected void packData(JsonObject jsonObject) {
    jsonObject.addProperty(FORMAT, format);
    if (expiresAfter != null) {
      jsonObject.addProperty(EXPIRES_AFTER, expiresAfter.toString());
    }
    if (notBefore != null) {
      jsonObject.addProperty(NOT_BEFORE, notBefore.toString());
    }
    jsonObject.addProperty(io.mapsmessaging.schemas.config.Constants.UUID, uniqueId);
    if (creation == null) {
      creation = LocalDateTime.now();
    }
    jsonObject.addProperty(CREATION, creation.toString());
    pack(jsonObject, comments, COMMENTS);
    pack(jsonObject, version, VERSION);
    pack(jsonObject, source, SOURCE);
    pack(jsonObject, title, TITLE);
    pack(jsonObject, mimeType, MIME_TYPE);
    pack(jsonObject, resourceType, RESOURCE_TYPE);
    pack(jsonObject, interfaceDescription, INTERFACE_DESCRIPTION);
  }

  private void pack(JsonObject jsonObject, String val, String key) {
    if (val != null && !val.isEmpty()) {
      jsonObject.addProperty(key, val);
    }
  }

  private JsonObject packtoJSON() throws IOException {
    JsonObject jsonObject = new JsonObject();
    jsonObject.add(SCHEMA, packData());
    return jsonObject;
  }

}
