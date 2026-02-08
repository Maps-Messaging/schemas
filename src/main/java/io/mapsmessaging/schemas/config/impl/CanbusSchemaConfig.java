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
import java.util.Base64;

@Schema(description = "Canbus Schema Configuration")
public class CanbusSchemaConfig extends SchemaConfig {

  private static final String XML_PATH = "XmlPath";
  private static final String XML_BASE64 = "XmlBase64";

  public CanbusSchemaConfig() {
    super("canbus");
  }

  protected CanbusSchemaConfig(SchemaConfig config) {
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

  public String getXmlPath() {
    JsonObject json = getSchema();
    if (json != null && json.has(XML_PATH)) {
      return json.get(XML_PATH).getAsString();
    }
    return null;
  }

  public void setXmlPath(String xmlPath) {
    JsonObject json = ensureSchema();
    if (xmlPath == null || xmlPath.isBlank()) {
      json.remove(XML_PATH);
      return;
    }
    json.addProperty(XML_PATH, xmlPath);
    json.remove(XML_BASE64);
  }

  public void setXmlBase64(byte[] xmlBytes) {
    JsonObject json = ensureSchema();
    if (xmlBytes == null || xmlBytes.length == 0) {
      json.remove(XML_PATH);
      json.remove(XML_BASE64);
      return;
    }
    json.addProperty(XML_BASE64, Base64.getEncoder().encodeToString(xmlBytes));
    json.remove(XML_PATH);
  }

  public byte[] getXmlBase64() {
    JsonObject json = getSchema();
    if (json == null || !json.has(XML_BASE64)) {
      return null;
    }
    return Base64.getDecoder().decode(json.get(XML_BASE64).getAsString());
  }

  @Override
  public String pack() throws IOException {
    String path = getXmlPath();
    byte[] xml = getXmlBase64();

    if ((path == null || path.isBlank()) && (xml == null || xml.length > 0)) {
      throw new IOException("Canbus config requires either 'dialect' or 'dialectXml'");
    }

    if (path != null && !path.isBlank() && xml != null && xml.length > 0) {
      throw new IOException("Canbus config must specify only one of 'path' or 'xmlBase64'");
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