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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.config.impl.XRegistrySchemaVersionImpl;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * The type Schema config factory.
 */

@SuppressWarnings("java:S6548") // yes it is a singleton
public class SchemaConfigFactory {
  public static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private final List<XRegistrySchemaVersion> schemaConfigs;
  private final Logger logger;

  private SchemaConfigFactory() {
    schemaConfigs = new ArrayList<>();
    ServiceLoader<XRegistrySchemaVersion> schemaConfigServiceLoader = ServiceLoader.load(XRegistrySchemaVersion.class);
    for (XRegistrySchemaVersion config : schemaConfigServiceLoader) {
      schemaConfigs.add(config);
    }
    logger = LoggerFactory.getLogger(SchemaConfigFactory.class);
  }

  public static SchemaConfigFactory getInstance() {
    return SchemaConfigFactory.Holder.INSTANCE;
  }

  @SuppressWarnings("unchecked")
  public static String detectFormat(Map<String, Object> root) {
    String uniqueId = root.get("uuid").toString();
    Object groupsRaw = root.get("schemagroups");
    if (!(groupsRaw instanceof Map<?, ?> groups)) return null;

    Object groupRaw = groups.get(uniqueId);
    if (!(groupRaw instanceof Map<?, ?> group)) return null;

    Object schemasRaw = group.get("schemas");
    if (!(schemasRaw instanceof Map<?, ?> schemas)) return null;

    Object schemaRaw = schemas.get(uniqueId);
    if (!(schemaRaw instanceof Map<?, ?> schema)) return null;

    Object f = schema.get("format");
    return f == null ? null : String.valueOf(f);
  }

  /**
   * Construct config schema config.
   *
   * @param rawPayload the raw payload
   * @return the schema config
   * @throws IOException the io exception
   */
  public XRegistrySchemaVersion constructConfig(byte[] rawPayload) throws IOException {
    if (rawPayload == null || rawPayload.length == 0) {
      throw new IllegalStateException("Raw payload is null or empty");
    }
    try {
      return constructConfig(new String(rawPayload));
    } catch (Error e) {
      throw new IOException(e);
    }
  }

  /**
   * Construct config schema config.
   *
   * @param payload the payload
   * @return the schema config
   * @throws IOException the io exception
   */
  public XRegistrySchemaVersion constructConfig(String payload) throws IOException {
    try {
      XRegistrySchemaVersion version = gson.fromJson(payload, XRegistrySchemaVersion.class);
      if (version == null || version.getFormat() == null) {
        throw new IOException("Schema config is not valid");
      }
      return constructConfig(version);
    } catch (JsonSyntaxException e) {
      throw new IOException(e);
    }
  }

  public XRegistrySchemaVersion constructConfig(JsonObject payload) throws IOException {
    XRegistrySchemaVersion version = gson.fromJson(payload, XRegistrySchemaVersion.class);
    if (version == null || version.getFormat() == null) {
      throw new IOException("Schema config is not valid");
    }
    return constructConfig(version);
  }

  private XRegistrySchemaVersion constructConfig(XRegistrySchemaVersion config) throws IOException {
    XRegistrySchemaVersion base = findSchemaConfig(config.getFormat());
    if (base instanceof XRegistrySchemaVersionImpl impl) {
      return impl.getInstance(config);
    }
    return null;
  }

  private XRegistrySchemaVersion findSchemaConfig(String name) {
    return schemaConfigs.stream()
        .filter(c -> c.getFormat().equalsIgnoreCase(name))
        .findFirst()
        .orElse(null);
  }

  private static class Holder {
    static final SchemaConfigFactory INSTANCE = new SchemaConfigFactory();
  }

}
