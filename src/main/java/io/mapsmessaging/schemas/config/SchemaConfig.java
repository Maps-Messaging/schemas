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
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static io.mapsmessaging.schemas.config.Constants.*;
import static io.mapsmessaging.schemas.config.SchemaConfigFactory.gson;


@SuppressWarnings("javaarchitecture:S7091")
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = AvroSchemaConfig.class, name = "avro"),
    @JsonSubTypes.Type(value = CsvSchemaConfig.class, name = "csv"),
    @JsonSubTypes.Type(value = CbcSchemaConfig.class, name = "cbc"),
    @JsonSubTypes.Type(value = JsonSchemaConfig.class, name = "json"),
    @JsonSubTypes.Type(value = CborSchemaConfig.class, name = "cbor"),
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
        @DiscriminatorMapping(value = "csv", schema = CsvSchemaConfig.class),
        @DiscriminatorMapping(value = "json", schema = JsonSchemaConfig.class),
        @DiscriminatorMapping(value = "cbor", schema = CborSchemaConfig.class),
        @DiscriminatorMapping(value = "messagePack", schema = MessagePackSchemaConfig.class),
        @DiscriminatorMapping(value = "native", schema = NativeSchemaConfig.class),
        @DiscriminatorMapping(value = "protobuf", schema = ProtoBufSchemaConfig.class),
        @DiscriminatorMapping(value = "raw", schema = RawSchemaConfig.class),
        @DiscriminatorMapping(value = "xml", schema = XmlSchemaConfig.class),
        @DiscriminatorMapping(value = "cbc", schema = CbcSchemaConfig.class)
    })

/**
 * The type Schema config.
 */
public abstract class SchemaConfig implements Serializable {
  private static final DateTimeFormatter SPACE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
  /**
   * The name of the Formatter that the configuration represents. Typically, set by the class that extends this
   */
  @Getter
  protected final String format;
  @Getter
  private final Map<String, String> labels;
  /**
   * The Logger to use for all messages
   */
  protected transient Logger logger;
  @Getter
  @Setter
  protected String title;
  @Getter
  @Setter
  protected String name;
  @Getter
  @Setter
  protected String matchExpression;
  /**
   * The Unique id.
   */
  @Getter
  protected String uniqueId;
  @Getter
  private OffsetDateTime creation;
  @Getter
  @Setter
  private OffsetDateTime updated;
  @Getter
  @Setter
  private OffsetDateTime expiresAfter;
  @Getter
  @Setter
  private OffsetDateTime notBefore;
  @Setter
  @Getter
  private String comments;
  @Setter
  @Getter
  private String description;
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
  @Getter
  @Setter
  private String versionId;

  @Getter
  @Setter
  private String ancestor;

  @Getter
  @Setter
  private String selfUrl;

  @Getter
  @Setter
  private String schemaUrl;

  @Getter
  @Setter
  private String documentationUrl;

  @Getter
  @Setter
  private Boolean validated;

  /**
   * Instantiates a new Schema config.
   *
   * @param format the format
   */
  protected SchemaConfig(String format) {
    this.format = format;
    labels = new HashMap<>();
    logger = LoggerFactory.getLogger(SchemaConfig.class);
  }

  /**
   * Instantiates a new Schema config.
   *
   * @param format the formatter name
   * @param config the config map with the defined fields
   */
  @SuppressWarnings("java:S3776") // too many ifs - required for explicit field mapping
  protected SchemaConfig(String format, Map<String, Object> config) {
    this(format);
    if (config != null && !config.isEmpty()) {
      unpackData(config);
    }
  }

  private static OffsetDateTime parseTimestamp(String text) {
    if (text == null || text.isEmpty()) return null;
    try {
      return OffsetDateTime.parse(text, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    } catch (Exception ignored) {
    }
    try {
      return ZonedDateTime.parse(text, DateTimeFormatter.ISO_ZONED_DATE_TIME).toOffsetDateTime();
    } catch (Exception ignored) {
    }
    try {
      return LocalDateTime.parse(text, DateTimeFormatter.ISO_LOCAL_DATE_TIME).atOffset(ZoneOffset.UTC);
    } catch (Exception ignored) {
    }
    try {
      return LocalDateTime.parse(text, SPACE_FMT).atOffset(ZoneOffset.UTC);
    } catch (Exception ignored) {
    }
    try {
      long epoch = Long.parseLong(text.trim());
      if (Math.abs(epoch) < 1_000_000_000_000L) { // < ~2001-09-09 in ms => treat as seconds
        return Instant.ofEpochSecond(epoch).atOffset(ZoneOffset.UTC);
      } else {
        return Instant.ofEpochMilli(epoch).atOffset(ZoneOffset.UTC);
      }
    } catch (Exception ignored) {
      // Ignore this
    }
    return null;
  }

  public JsonObject packData() throws IOException {
    JsonObject json = new JsonObject();
    return packData(json);
  }

  public abstract byte[] getSchemaDefinition();

  protected abstract SchemaConfig getInstance(Map<String, Object> config);

  protected abstract void unpackSpecific(Map<String, Object> data);

  protected abstract void packSpecific(JsonObject jsonObject) throws IOException;

  /**
   * Pack the schema into a JSON representation string.
   *
   * @return the string
   * @throws IOException the io exception
   */
  public String pack() throws IOException {
    return gson.toJson(packToJson());
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
    return gson.fromJson(packToJson(), type);
  }

  public void setUniqueId(String uniqueId) {
    if (this.uniqueId != null && !this.uniqueId.isEmpty()) {
      return; // already set; do not overwrite
    }
    this.uniqueId = uniqueId;
    if (this.creation == null) {
      this.creation = OffsetDateTime.now(ZoneOffset.UTC);
    }
  }

  public void setUniqueId(UUID uniqueId) {
    setUniqueId(uniqueId.toString());
  }

  // Add these new generic entry points
  @SuppressWarnings("unchecked")
  public final void unpackData(Map<String, Object> data) {
    loadBase(data);

    Object groupsRaw = data.get("schemagroups");
    if (!(groupsRaw instanceof Map<?, ?> groups)) return;

    Object groupRaw = groups.get(uniqueId); // deterministic key
    if (!(groupRaw instanceof Map<?, ?> group)) return;

    Object schemasRaw = group.get("schemas");
    if (!(schemasRaw instanceof Map<?, ?> schemas)) return;

    Object schemaRaw = schemas.get(uniqueId); // deterministic key
    if (schemaRaw instanceof Map<?, ?> schemaMap) {
      unpackSpecific((Map<String, Object>) schemaMap); // subtype loads payload
    }
  }

  public final JsonObject packData(JsonObject root) throws IOException {
    packBase(root);
    JsonObject schemaResource = new JsonObject();
    packSpecific(schemaResource);
    schemaResource.addProperty("format", format);
    JsonObject schemas = new JsonObject();
    schemas.add(uniqueId, schemaResource);
    JsonObject group = new JsonObject();
    group.addProperty("schemagroupid", uniqueId);
    group.add("schemas", schemas);
    group.addProperty("schemascount", 1);
    JsonObject groups = new JsonObject();
    groups.add(uniqueId, group);
    root.add("schemagroups", groups);
    root.addProperty("schemagroupscount", 1);
    return root;
  }

  // Base loaders/packers extracted from your ctor and packData
  @SuppressWarnings("java:S3776")
  protected final void loadBase(Map<String, Object> config) {
    if (config.containsKey(io.mapsmessaging.schemas.config.Constants.UUID)) {
      Object id = config.get(io.mapsmessaging.schemas.config.Constants.UUID);
      if (id != null) setUniqueId(String.valueOf(id));
    }
    if (config.containsKey(NAME)) {
      name = (String) config.get(NAME);
    }
    if (config.containsKey(MATCH)) {
      matchExpression = (String) config.get(MATCH);
    }
    if (config.containsKey(EXPIRES_AFTER)) {
      expiresAfter = loadDateTime(config, EXPIRES_AFTER);
    }
    if (config.containsKey(NOT_BEFORE)) {
      notBefore = loadDateTime(config, NOT_BEFORE);
    }
    if (config.containsKey(CREATION)) {
      creation = loadDateTime(config, CREATION);
    }
    if (config.containsKey(UPDATED)) {
      updated = loadDateTime(config, UPDATED);
    }
    if (config.containsKey(COMMENTS)) {
      comments = (String) config.get(COMMENTS);
    }
    if (config.containsKey(DESCRIPTION)) {
      description = (String) config.get(DESCRIPTION);
    }
    if (config.containsKey(VERSION)) {
      Object v = config.get(VERSION);
      if (v instanceof Number n) {
        version = String.valueOf(n.intValue());
      } else if (v != null) {
        version = String.valueOf(v);
      }
    }
    if (config.containsKey(SOURCE)) {
      source = (String) config.get(SOURCE);
    }
    if (config.containsKey(TITLE)) {
      title = (String) config.get(TITLE);
    }
    if (config.containsKey(MIME_TYPE)) {
      mimeType = (String) config.get(MIME_TYPE);
    }
    if (config.containsKey(RESOURCE_TYPE)) {
      resourceType = (String) config.get(RESOURCE_TYPE);
    }
    if (config.containsKey(INTERFACE_DESCRIPTION)) {
      interfaceDescription = (String) config.get(INTERFACE_DESCRIPTION);
    }
    if (config.containsKey(VERSION_ID)) {
      versionId = (String) config.get(VERSION_ID);
    }
    if (config.containsKey(ANCESTOR)) {
      ancestor = (String) config.get(ANCESTOR);
    }
    if (config.containsKey(SELF_URL)) {
      selfUrl = (String) config.get(SELF_URL);
    }
    if (config.containsKey(SCHEMA_URL)) {
      schemaUrl = (String) config.get(SCHEMA_URL);
    }
    if (config.containsKey(DOCUMENTATION_URL)) {
      documentationUrl = (String) config.get(DOCUMENTATION_URL);
    }
    if (config.containsKey(VALIDATED)) {
      Object v = config.get(VALIDATED);
      validated = (v instanceof Boolean b) ? b : Boolean.valueOf(String.valueOf(v));
    }
    if (config.containsKey(LABELS)) {
      loadLabels(config);
    }
  }

  protected final void packBase(JsonObject jsonObject) {
    OffsetDateTime creationUtc = creation == null
        ? OffsetDateTime.now(ZoneOffset.UTC)
        : creation.withOffsetSameInstant(ZoneOffset.UTC);
    jsonObject.addProperty(io.mapsmessaging.schemas.config.Constants.UUID, uniqueId);
    jsonObject.addProperty(CREATION, creationUtc.toString());

    if (updated != null) {
      jsonObject.addProperty(UPDATED, updated.withOffsetSameInstant(ZoneOffset.UTC).toString());
    }
    if (expiresAfter != null) {
      jsonObject.addProperty(EXPIRES_AFTER, expiresAfter.withOffsetSameInstant(ZoneOffset.UTC).toString());
    }
    if (notBefore != null) {
      jsonObject.addProperty(NOT_BEFORE, notBefore.withOffsetSameInstant(ZoneOffset.UTC).toString());
    }

    pack(jsonObject, comments, COMMENTS);
    pack(jsonObject, description, DESCRIPTION);
    pack(jsonObject, version, VERSION);
    pack(jsonObject, source, SOURCE);
    pack(jsonObject, title, TITLE);
    pack(jsonObject, name, NAME);
    pack(jsonObject, matchExpression, MATCH);
    pack(jsonObject, mimeType, MIME_TYPE);
    pack(jsonObject, resourceType, RESOURCE_TYPE);
    pack(jsonObject, interfaceDescription, INTERFACE_DESCRIPTION);
    pack(jsonObject, versionId, VERSION_ID);
    pack(jsonObject, ancestor, ANCESTOR);
    pack(jsonObject, selfUrl, SELF_URL);
    pack(jsonObject, schemaUrl, SCHEMA_URL);
    pack(jsonObject, documentationUrl, DOCUMENTATION_URL);
    if (validated != null) {
      jsonObject.addProperty(VALIDATED, validated);
    }

    if (labels != null && !labels.isEmpty()) {
      JsonObject labelObject = new JsonObject();
      for (Map.Entry<String, String> entry : labels.entrySet()) {
        if (entry.getKey() != null && entry.getValue() != null) {
          labelObject.addProperty(entry.getKey(), entry.getValue());
        }
      }
      if (!labelObject.isEmpty()) {
        jsonObject.add(LABELS, labelObject);
      }
    }
  }

  private void pack(JsonObject jsonObject, String val, String key) {
    if (val != null && !val.isEmpty()) {
      jsonObject.addProperty(key, val);
    }
  }

  private JsonObject packToJson() throws IOException {
    return packData();
  }

  private OffsetDateTime loadDateTime(Map<String, Object> config, String key) {
    Object value = config.get(key);
    if (value == null) return null;
    if (value instanceof OffsetDateTime offsetDateTime) return offsetDateTime;
    if (value instanceof ZonedDateTime zonedDateTime) return zonedDateTime.toOffsetDateTime();
    if (value instanceof LocalDateTime localDateTime) return localDateTime.atOffset(ZoneOffset.UTC);
    if (value instanceof Instant instant) return instant.atOffset(ZoneOffset.UTC);
    if (value instanceof Number number) {
      long epoch = number.longValue();
      if (Math.abs(epoch) < 1_000_000_000_000L) {
        return Instant.ofEpochSecond(epoch).atOffset(ZoneOffset.UTC);
      } else {
        return Instant.ofEpochMilli(epoch).atOffset(ZoneOffset.UTC);
      }
    }
    return parseTimestamp(String.valueOf(value));
  }

  @SuppressWarnings("unchecked")
  private void loadLabels(Map<String, Object> config) {
    Object raw = config.get(LABELS);
    if (raw == null) {
      return;
    }
    if (raw instanceof Map<?, ?> anyMap) {
      for (Map.Entry<?, ?> entry : anyMap.entrySet()) {
        String key = entry.getKey() == null ? null : String.valueOf(entry.getKey());
        String value = entry.getValue() == null ? null : String.valueOf(entry.getValue());
        if (key != null && !key.isEmpty() && value != null && value.chars().noneMatch(Character::isISOControl)) {
          labels.put(key, value);
        }
      }
    }
  }
}
