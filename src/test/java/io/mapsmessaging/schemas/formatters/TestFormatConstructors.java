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

package io.mapsmessaging.schemas.formatters;

import io.mapsmessaging.schemas.config.impl.RawSchemaConfig;
import io.mapsmessaging.schemas.config.impl.XRegistrySchemaVersionImpl;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.UUID;

class TestFormatConstructors {

  @Test
  void validSchemaLoad() throws IOException {
    XRegistrySchemaVersion good = new RawSchemaConfig();
    good.setUniqueId(UUID.randomUUID());
    Assertions.assertNotNull(MessageFormatterFactory.getInstance().getFormatter(good));
  }


  @Test
  void invalidSchemaLoad() {
    XRegistrySchemaVersion bad = new BadSchema();
    bad.setUniqueId(UUID.randomUUID());
    Assertions.assertThrowsExactly(IOException.class, () -> MessageFormatterFactory.getInstance().getFormatter(bad));
  }

  static class BadSchema extends XRegistrySchemaVersionImpl {

    protected BadSchema() {
      super("BAD");
      setVersionId(UUID.randomUUID().toString());
    }

    protected BadSchema(XRegistrySchemaVersion config) {
      super(config);
    }

    @Override
    public XRegistrySchemaVersion getInstance(XRegistrySchemaVersion config) {
      return new BadSchema(config);
    }

    @Override
    public String getMimeType() {
      return "";
    }
  }

}
