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

import io.mapsmessaging.schemas.config.SchemaConfig;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

/**
 * The type Message formatter factory.
 */
@SuppressWarnings("java:S6548") // yes it is a singleton
public class MessageFormatterFactory {

  private static class Holder {
    static final MessageFormatterFactory INSTANCE = new MessageFormatterFactory();
  }

  public static MessageFormatterFactory getInstance() {
    return MessageFormatterFactory.Holder.INSTANCE;
  }

  private final List<MessageFormatter> messageFormatters;

  private MessageFormatterFactory() {
    messageFormatters = new ArrayList<>();
    ServiceLoader<MessageFormatter> messageFormatterServiceLoader = ServiceLoader.load(MessageFormatter.class);
    for (MessageFormatter messageFormatter : messageFormatterServiceLoader) {
      messageFormatters.add(messageFormatter);
    }
  }

  public List<String> getFormatters() {
    List<String> formatList = new ArrayList<>();
    for (MessageFormatter messageFormatter : messageFormatters) {
      formatList.add(messageFormatter.getName());
    }
    return formatList;
  }


  /**
   * Gets formatter.
   *
   * @param config the config
   * @return the formatter
   * @throws IOException the io exception
   */
  public MessageFormatter getFormatter(SchemaConfig config) throws IOException {
    for (MessageFormatter formatter : messageFormatters) {
      if (formatter.getName().equalsIgnoreCase(config.getFormat())) {
        return formatter.getInstance(config);
      }
    }
    throw new IOException("Unknown format config received");
  }
}

