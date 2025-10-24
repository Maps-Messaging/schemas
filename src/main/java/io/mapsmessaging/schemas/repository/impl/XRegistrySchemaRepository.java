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

package io.mapsmessaging.schemas.repository.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import io.mapsmessaging.schemas.repository.impl.xregistry.XRegistryClient;
import io.mapsmessaging.schemas.repository.impl.xregistry.XRegistryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class XRegistrySchemaRepository extends SimpleSchemaRepository {

  private static final Logger log = LoggerFactory.getLogger(XRegistrySchemaRepository.class);

  private static final String CONFIG_FILE = "xregistry.yaml";
  private static final String REPOSITORY_NAME = "xregistry";
  private XRegistryClient client;
  private XRegistryConfig config;

  public void XRegistrySchemaRepository(Map<String, String> properties) throws IOException {
    log.info("Initializing xRegistry Schema Repository");
    config = loadConfiguration(properties);
    client = new XRegistryClient(config);
    log.info("xRegistry initialized: {}", config.getBaseUrl());
  }


  @Override
  public List<XRegistrySchemaVersion> getAll() {
    List<XRegistrySchemaVersion> out = new ArrayList<>();
    try {
      for (var x : client.listSchemas()) {
        out.add(x);
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
    return out;
  }

  public void removeSchema(String schemaId) {
    try {
      client.deleteSchema(schemaId);
      super.removeSchema(schemaId);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public String getName() {
    return REPOSITORY_NAME;
  }

  @Override
  public void removeAllSchemas() {
    List<XRegistrySchemaVersion> schemas = getAll();
    for (XRegistrySchemaVersion schema : schemas) {
      removeSchema(schema.getUniqueId());
    }
    super.removeAllSchemas();
  }

  @Override
  public XRegistrySchemaVersion addSchema(String context, XRegistrySchemaVersion schema) {
//    try {
    //return client.registerSchema(schema.getUniqueId(), schema.getFormat(), schema.pack(), "");
    //  } catch (IOException e) {
    // e.printStackTrace();
    //}
    return schema;
  }

  @Override
  public XRegistrySchemaVersion getSchema(String uuid) {
    String version = "";
    XRegistrySchemaVersion schema = super.getSchema(uuid);
    if (schema != null) {
      return schema;
    }
    try {
      var x = client.getSchema(uuid, version);
      if (x == null) return null;
      mapByUUID.put(uuid, x);
      return x;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return null;
  }

  @Override
  public List<XRegistrySchemaVersion> getSchemaByContext(String context) {
    return List.of();
  }

  @Override
  public List<XRegistrySchemaVersion> getSchemas(String type) {
    return List.of();
  }

  @Override
  public Map<String, List<XRegistrySchemaVersion>> getMappedSchemas() {
    return Map.of();
  }

  public void close() throws Exception {
    if (client != null) client.close();
  }

  private XRegistryConfig loadConfiguration(Map<String, String> properties) throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

    // classpath
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE)) {
      if (is != null) {
        XRegistryConfig cfg = mapper.readValue(is, XRegistryConfig.class);
        if (properties != null) overrideConfig(cfg, properties);
        return cfg;
      }
    }
    // filesystem
    File f = new File(CONFIG_FILE);
    if (f.exists()) {
      XRegistryConfig cfg = mapper.readValue(f, XRegistryConfig.class);
      if (properties != null) overrideConfig(cfg, properties);
      return cfg;
    }
    // properties only
    XRegistryConfig cfg = new XRegistryConfig();
    if (properties != null) overrideConfig(cfg, properties);
    if (cfg.getBaseUrl() == null) throw new IOException("xRegistry baseUrl not configured");
    return cfg;
  }

  private void overrideConfig(XRegistryConfig cfg, Map<String, String> p) {
    if (p.containsKey("baseUrl")) cfg.setBaseUrl(p.get("baseUrl"));
    if (p.containsKey("apiKey")) cfg.setApiKey(p.get("apiKey"));
    if (p.containsKey("groupName")) cfg.setGroupName(p.get("groupName"));
    if (p.containsKey("enableCache")) cfg.setEnableCache(Boolean.parseBoolean(p.get("enableCache")));
  }

}
