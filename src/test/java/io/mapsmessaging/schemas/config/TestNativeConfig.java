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

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig;
import io.mapsmessaging.schemas.config.impl.NativeSchemaConfig.TYPE;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.UUID;

class TestNativeConfig extends GeneralBaseTest {

  XRegistrySchemaVersion getProperties() {
    NativeSchemaConfig props = new NativeSchemaConfig();
    props.setType(TYPE.DOUBLE);
    return props;
  }

  @Override
  XRegistrySchemaVersion buildConfig() {
    NativeSchemaConfig props = new NativeSchemaConfig();
    props.setType(TYPE.DOUBLE);
    setBaseConfig(props);
    return props;
  }

  @Override
  void validate(XRegistrySchemaVersion schemaConfig) {
    Assertions.assertInstanceOf(NativeSchemaConfig.class, schemaConfig);
    NativeSchemaConfig config = (NativeSchemaConfig) schemaConfig;
    Assertions.assertEquals(TYPE.DOUBLE, config.getType());
  }


  @Test
  void invalidConfig() {
    NativeSchemaConfig config = new NativeSchemaConfig();
    config.setUniqueId(UUID.randomUUID());
    config.setExpiresAfter(OffsetDateTime.now().plusDays(10));
    config.setNotBefore(OffsetDateTime.now().minusDays(10));
    Assertions.assertThrowsExactly(IOException.class, config::pack);
  }

  @Test
  void checkAllTypes() throws IOException {
    for (TYPE type : NativeSchemaConfig.TYPE.values()) {
      NativeSchemaConfig config = new NativeSchemaConfig();
      config.setType(type);
      config.setUniqueId(UUID.randomUUID());
      config.setExpiresAfter(OffsetDateTime.now().plusDays(10));
      config.setNotBefore(OffsetDateTime.now().minusDays(10));
      JsonObject jsonObject = JsonParser.parseString(new String(config.pack())).getAsJsonObject();
      Assertions.assertEquals(type.toString(), jsonObject.getAsJsonObject("schema").get("type").getAsString());

    }

    for (TYPE type : NativeSchemaConfig.TYPE.values()) {
      NativeSchemaConfig props = new NativeSchemaConfig();
      props.setType(type);
      JsonObject jsonObject = JsonParser.parseString(new String(props.pack())).getAsJsonObject();
      Assertions.assertEquals(type.toString(), jsonObject.getAsJsonObject("schema").get("type").getAsString());
    }
  }
}