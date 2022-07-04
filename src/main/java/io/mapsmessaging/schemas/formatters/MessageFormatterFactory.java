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

import io.mapsmessaging.schemas.config.SchemaConfig;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;

public class MessageFormatterFactory {

  private static final MessageFormatterFactory instance;

  static{
    instance = new MessageFormatterFactory();
  }

  public static MessageFormatterFactory getInstance() {
    return instance;
  }

  private final List<MessageFormatter> messageFormatters;

  public MessageFormatter getFormatter(SchemaConfig config) throws IOException {
    for (MessageFormatter formatter : messageFormatters) {
      if (formatter.getName().equalsIgnoreCase(config.getFormat())) {
        return formatter.getInstance(config);
      }
    }
    throw new IOException("Unknown format config received");
  }

  private MessageFormatterFactory() {
    messageFormatters = new ArrayList<>();
    ServiceLoader<MessageFormatter> messageFormatterServiceLoader = ServiceLoader.load(MessageFormatter.class);
    for(MessageFormatter messageFormatter:messageFormatterServiceLoader){
      messageFormatters.add(messageFormatter);
    }
  }
}

