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

package io.mapsmessaging.schemas.formatters;

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import org.json.JSONObject;

public abstract class MessageFormatter {

  protected Logger logger;

  protected MessageFormatter() {
    logger = LoggerFactory.getLogger(MessageFormatter.class);
  }

  public abstract String getName();

  public abstract MessageFormatter getInstance(SchemaConfig config) throws IOException;

  public abstract JSONObject parseToJson(byte[] payload) throws IOException;

  public abstract ParsedObject parse(byte[] payload);

}
