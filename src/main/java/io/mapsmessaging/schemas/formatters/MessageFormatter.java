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

import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import org.json.JSONObject;

/**
 * The type Message formatter.
 */
public abstract class MessageFormatter {

  /**
   * The Logger.
   */
  protected Logger logger;

  /**
   * Instantiates a new Message formatter.
   */
  protected MessageFormatter() {
    logger = LoggerFactory.getLogger(MessageFormatter.class);
  }

  /**
   * Gets name.
   *
   * @return the name
   */
  public abstract String getName();

  /**
   * Gets instance.
   *
   * @param config the config
   * @return the instance
   * @throws IOException the io exception
   */
  public abstract MessageFormatter getInstance(SchemaConfig config) throws IOException;

  /**
   * Parse to json json object.
   *
   * @param payload the payload
   * @return the json object
   * @throws IOException the io exception
   */
  public abstract JSONObject parseToJson(byte[] payload) throws IOException;

  /**
   * Parse parsed object.
   *
   * @param payload the payload
   * @return the parsed object
   */
  public abstract ParsedObject parse(byte[] payload);

  /**
   * The type Default parser.
   */
  protected static class DefaultParser implements ParsedObject {

    private final byte[] payload;

    /**
     * Instantiates a new Default parser.
     *
     * @param payload the payload
     */
    public DefaultParser(byte[] payload) {
      this.payload = payload;
    }

    @Override
    public Object getReferenced() {
      return payload;
    }

    @Override
    public Object get(String s) {
      return null;
    }
  }

}
