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

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import java.io.IOException;
import java.util.Base64;
import java.util.Map.Entry;
import lombok.NonNull;
import org.apache.qpid.proton.amqp.Symbol;
import org.apache.qpid.proton.amqp.messaging.ApplicationProperties;
import org.apache.qpid.proton.amqp.messaging.Data;
import org.apache.qpid.proton.amqp.messaging.DeliveryAnnotations;
import org.apache.qpid.proton.amqp.messaging.Footer;
import org.apache.qpid.proton.amqp.messaging.MessageAnnotations;
import org.apache.qpid.proton.amqp.messaging.Section;
import org.apache.qpid.proton.codec.DroppingWritableBuffer;
import org.apache.qpid.proton.codec.WritableBuffer;
import org.apache.qpid.proton.message.Message;
import org.apache.qpid.proton.message.Message.Factory;
import org.json.JSONObject;

public class QpidJmsFormatter extends MessageFormatter {

  private static final String NAME = "QPID-JMS";

  public QpidJmsFormatter() {
    // Used by the service loader, there is nothing to do
  }

  public String getName() {
    return NAME;
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    return this;
  }


  @Override
  public ParsedObject parse(@NonNull byte[] payload) {
    Message protonMsg = Factory.create();
    protonMsg.decode(payload, 0, payload.length);
    return new StructuredResolver(new MapResolver(parse(protonMsg).toMap()), protonMsg);
  }

  @Override
  public JSONObject parseToJson(byte[] payload) {
    Message protonMsg = (Message) parse(payload);
    return parse(protonMsg);
  }

  private JSONObject parse(Message protonMsg) {
    JSONObject jsonObject = new JSONObject();
    pack(jsonObject, "address", protonMsg.getAddress());
    pack(jsonObject, "content-type", protonMsg.getContentType());
    pack(jsonObject, "creation-time", protonMsg.getCreationTime());
    pack(jsonObject, "content-encoding", protonMsg.getContentEncoding());
    pack(jsonObject, "delivery-count", protonMsg.getDeliveryCount());
    pack(jsonObject, "expiry-time", protonMsg.getExpiryTime());
    pack(jsonObject, "priority", protonMsg.getPriority());
    pack(jsonObject, "reply-to", protonMsg.getReplyTo());
    pack(jsonObject, "reply-to-group-id", protonMsg.getReplyToGroupId());
    pack(jsonObject, "subject", protonMsg.getSubject());
    pack(jsonObject, "TTL", protonMsg.getTtl());
    pack(jsonObject, "group-id", protonMsg.getGroupId());
    pack(jsonObject, "group-sequence", protonMsg.getGroupSequence());
    pack(jsonObject, "user-id", protonMsg.getUserId());
    pack(jsonObject, "message-id", protonMsg.getMessageId());
    pack(jsonObject, "correlation-id", protonMsg.getCorrelationId());

    pack(jsonObject, "error", protonMsg.getError());
    pack(jsonObject, "footer", protonMsg.getFooter());
    pack(jsonObject, "application-properties", protonMsg.getApplicationProperties());
    pack(jsonObject, "delivery-annotations", protonMsg.getDeliveryAnnotations());
    pack(jsonObject, "message-annotations", protonMsg.getMessageAnnotations());

    Section body = protonMsg.getBody();
    if (body instanceof Data) {
      Data data = (Data) body;
      jsonObject.put("data", new String(Base64.getEncoder().encode(data.getValue().getArray())));
    }
    return jsonObject;
  }

  private void pack(JSONObject jsonObject, String key, Object val) {
    if (val == null) {
      return;
    }
    if (val instanceof String) {
      jsonObject.put(key, val);
    } else if (val instanceof Number) {
      jsonObject.put(key, val);
    } else if (val instanceof Footer) {
      packFooter(key,  (Footer) val, jsonObject);
    } else if (val instanceof ApplicationProperties) {
      packApplicationProperties(key, (ApplicationProperties) val, jsonObject);
    } else if (val instanceof DeliveryAnnotations) {
      packDeliveryAnnotations(key, (DeliveryAnnotations) val, jsonObject);
    } else if (val instanceof MessageAnnotations) {
      packMessageAnnotations(key, (MessageAnnotations) val, jsonObject);
    } else {
      jsonObject.put(key, val.toString());
    }
  }

  private void packMessageAnnotations(String key, MessageAnnotations messageAnnotations, JSONObject jsonObject){
    JSONObject jsonMessageAnnotations = new JSONObject();
    for (Entry<Symbol, Object> entry : messageAnnotations.getValue().entrySet()) {
      pack(jsonMessageAnnotations, entry.getKey().toString(), entry.getValue());
    }
    jsonObject.put(key, jsonMessageAnnotations);
  }

  private void packFooter(String key, Footer footer, JSONObject jsonObject){
    JSONObject jsonFooter = new JSONObject();
    for (Object lookup : footer.getValue().entrySet()) {
      if (lookup instanceof Entry) {
        Entry<Object, Object> entry = (Entry<Object, Object>) lookup;
        pack(jsonFooter, entry.getKey().toString(), entry.getValue());
      }
    }
    jsonObject.put(key, jsonFooter);
  }

  private void packApplicationProperties(String key, ApplicationProperties applicationProperties, JSONObject jsonObject){
    JSONObject jsonApplication = new JSONObject();
    for (Entry<String, Object> entry : applicationProperties.getValue().entrySet()) {
      pack(jsonApplication, entry.getKey(), entry.getValue());
    }
    jsonObject.put(key, jsonApplication);
  }

  private void packDeliveryAnnotations(String key, DeliveryAnnotations deliveryAnnotations, JSONObject jsonObject){
    JSONObject jsonDeliveryAnnotations = new JSONObject();
    for (Entry<Symbol, Object> entry : deliveryAnnotations.getValue().entrySet()) {
      pack(jsonDeliveryAnnotations, entry.getKey().toString(), entry.getValue());
    }
    jsonObject.put(key, jsonDeliveryAnnotations);

  }

  @Override
  public byte[] pack(Object object) throws IOException {
    if (object instanceof Message) {
      Message protonMsg = (Message) object;
      WritableBuffer sizingBuffer = new DroppingWritableBuffer();
      protonMsg.encode(sizingBuffer);
      byte[] data = new byte[sizingBuffer.position() + 10];
      int size = protonMsg.encode(data, 0, data.length);
      if (size != data.length) {
        byte[] tmp = new byte[size];
        System.arraycopy(data, 0, tmp, 0, size);
        data = tmp;
      }
      return data;
    }
    logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), object.getClass().toString());
    throw new IOException("Unexpected object to be packed");
  }

}