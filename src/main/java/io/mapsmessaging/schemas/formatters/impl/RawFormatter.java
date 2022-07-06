/*
 *
 *     Copyright [ 2020 - 2022 ] [Matthew Buckton]
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

package io.mapsmessaging.schemas.formatters.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import java.io.IOException;
import java.util.Base64;
import org.json.JSONObject;

public class RawFormatter extends MessageFormatter {

  public RawFormatter() {
    // Used by the service loader, there is nothing to do
  }

  @Override
  public String getName() {
    return "RAW";
  }

  @Override
  public JSONObject parseToJson(byte[] payload) {
    JSONObject obj = new JSONObject();
    obj.put("payload", new String(Base64.getEncoder().encode(payload)));
    return obj;
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    return new ParsedObject() {
      @Override
      public Object getReferenced() {
        return payload;
      }

      @Override
      public Object get(String s) {
        return null;
      }
    };
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    return this;
  }

}
