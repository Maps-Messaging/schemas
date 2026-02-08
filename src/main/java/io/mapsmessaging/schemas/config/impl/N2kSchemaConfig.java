/*
 *
 *     Copyright [ 2020 - 2026 ] [Matthew Buckton]
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

package io.mapsmessaging.schemas.config.impl;

import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.swagger.v3.oas.annotations.media.Schema;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Schema(description = "NMEA2000 Schema Configuration")
public class N2kSchemaConfig extends SchemaConfig {

  private static final String DIALECT = "dialect";
  private static final String DIALECT_XML = "dialectXml";
  private static final String DIALECT_XML_BASE64 = "dialectXmlBase64";

  public N2kSchemaConfig() {
    super("n2k");
  }

  protected N2kSchemaConfig(SchemaConfig config) {
    super(config);
  }

  @Override
  public String getMimeType() {
    return "application/octet-stream";
  }

  @Override
  public SchemaConfig getInstance(SchemaConfig config) {
    return new MavlinkSchemaConfig(config);
  }


  public String getDialect() {
    JsonObject json = getSchema();
    if (json == null || !json.has(DIALECT)) {
      return null;
    }
    return json.get(DIALECT).getAsString();
  }

  public void setDialect(String dialect) {
    JsonObject json = ensureSchema();
    if (dialect == null || dialect.isBlank()) {
      json.remove(DIALECT);
      return;
    }
    json.addProperty(DIALECT, dialect.toLowerCase());
  }

  public String getDialectXml() {
    JsonObject json = getSchema();
    if (json == null) {
      return null;
    }

    if (json.has(DIALECT_XML)) {
      return json.get(DIALECT_XML).getAsString();
    }

    if (json.has(DIALECT_XML_BASE64)) {
      byte[] bytes = Base64.getDecoder().decode(json.get(DIALECT_XML_BASE64).getAsString());
      return new String(bytes, StandardCharsets.UTF_8);
    }

    return null;
  }

  public void setDialectXml(String xml) {
    JsonObject json = ensureSchema();
    if (xml == null || xml.isBlank()) {
      json.remove(DIALECT_XML);
      json.remove(DIALECT_XML_BASE64);
      return;
    }
    json.addProperty(DIALECT_XML, xml);
    json.remove(DIALECT_XML_BASE64);
  }

  public void setDialectXmlBase64(byte[] xmlBytes) {
    JsonObject json = ensureSchema();
    if (xmlBytes == null || xmlBytes.length == 0) {
      json.remove(DIALECT_XML);
      json.remove(DIALECT_XML_BASE64);
      return;
    }
    json.addProperty(DIALECT_XML_BASE64, Base64.getEncoder().encodeToString(xmlBytes));
    json.remove(DIALECT_XML);
  }

  @Override
  public String pack() throws IOException {
    String dialect = getDialect();
    String xml = getDialectXml();

    if ((dialect == null || dialect.isBlank()) && (xml == null || xml.isBlank())) {
      throw new IOException("N2K config requires either 'dialect' or 'dialectXml'");
    }

    if (dialect != null && !dialect.isBlank() && xml != null && !xml.isBlank()) {
      throw new IOException("N2K config must specify only one of 'dialect' or 'dialectXml'");
    }

    return super.pack();
  }

  private JsonObject ensureSchema() {
    JsonObject json = getSchema();
    if (json == null) {
      json = new JsonObject();
      setSchema(json);
    }
    return json;
  }
}