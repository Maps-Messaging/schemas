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

package io.mapsmessaging.schemas.formatters;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.impl.RawSchemaConfig;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class TestFormatConstructors {

  @Test
  void validSchemaLoad() throws IOException {
    SchemaConfig good = new RawSchemaConfig();
    good.setUniqueId(UUID.randomUUID());
    Assertions.assertNotNull(MessageFormatterFactory.getInstance().getFormatter(good));
  }


  @Test
  void invalidSchemaLoad() {
    SchemaConfig bad = new BadSchema();
    bad.setUniqueId(UUID.randomUUID());
    Assertions.assertThrowsExactly(IOException.class, () -> MessageFormatterFactory.getInstance().getFormatter(bad));
  }

  static class BadSchema extends SchemaConfig {

    protected BadSchema() {
      super("BAD");
      uniqueId = UUID.randomUUID().toString();
    }

    protected BadSchema(String format, Map<String, Object> config) {
      super(format, config);
    }

    @Override
    protected JSONObject packData() {
      JSONObject data = new JSONObject();
      packData(data);
      return data;
    }

    @Override
    protected SchemaConfig getInstance(Map<String, Object> config) {
      return this;
    }
  }

}
