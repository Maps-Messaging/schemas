/*
 * Copyright [ 2024 - 2026 ] MapsMessaging B.V.
 *
 * Licensed under the Apache License, Version 2.0 with the Commons Clause
 * (the "License"); you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *     https://commonsclause.com/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package io.mapsmessaging.schemas.formatters.impl;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParseException;
import io.mapsmessaging.schemas.formatters.ParseMode;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import io.mapsmessaging.schemas.repository.SchemaResolver;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import io.mapsmessaging.schemas.formatters.impl.cdr.CdrField;
import io.mapsmessaging.schemas.formatters.impl.cdr.CdrCodec;
import io.mapsmessaging.schemas.formatters.impl.cdr.CdrSchemaCompiler;
import io.mapsmessaging.schemas.formatters.impl.cdr.CdrSchemaResolver;
import io.mapsmessaging.schemas.formatters.impl.cdr.CdrUint64ParsedObject;
import io.mapsmessaging.schemas.formatters.impl.cdr.CdrReader;
import io.mapsmessaging.schemas.formatters.impl.cdr.CdrWriter;
import java.util.Map;

/**
 * Plain XCDR1 (CDR_BE / CDR_LE) DDS sample formatter.
 *
 * <p>The schema has an ordered "fields" array. Each field has a "name" and
 * "type". Struct fields contain another "fields" array; array and sequence
 * fields contain an "element" descriptor (a type string or a descriptor
 * object). Fixed arrays additionally contain a "length". This formatter
 * supports IDL and ordered JSON Schema inputs. XCDR2, parameter-list CDR and unions remain unsupported.
 */
public class CdrFormatter extends MessageFormatter {

  private final JsonObject definition;
  private final boolean littleEndian;
  private final List<CdrField> fields;

  public CdrFormatter() {
    definition = null;
    littleEndian = true;
    fields = List.of();
  }

  private CdrFormatter(JsonObject schema) throws IOException {
    definition = CdrSchemaResolver.resolve(schema);
    littleEndian = !definition.has("littleEndian") || definition.get("littleEndian").getAsBoolean();
    fields = CdrSchemaCompiler.compileFields(definition.getAsJsonArray("fields"));
  }

  @Override
  public String getName() {
    return "CDR";
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config, SchemaResolver schemaResolver) throws IOException {
    return new CdrFormatter(config.getSchema());
  }

  @Override
  public byte[] parseFromJson(JsonObject jsonObject) throws IOException {
    requireDefinition();
    if (jsonObject == null) {
      throw new IOException("CDR sample cannot be null");
    }
    try {
      CdrWriter writer = new CdrWriter(littleEndian);
      CdrCodec.writeFields(writer, fields, jsonObject);
      return writer.toByteArray();
    } catch (IllegalArgumentException | IllegalStateException e) {
      throw new IOException("Unable to encode CDR sample: " + e.getMessage(), e);
    }
  }

  @Override
  public JsonObject parseToJson(byte[] payload, ParseMode parseMode) throws ParseException {
    try {
      requireDefinition();
      if (payload == null || payload.length < 4 || payload[0] != 0 ||
          (payload[1] != 0 && payload[1] != 1) || payload[2] != 0 || payload[3] != 0) {
        throw new IOException("Unsupported or missing plain CDR encapsulation header");
      }
      boolean inputLittleEndian = payload[1] == 1;
      CdrReader reader = new CdrReader(payload, inputLittleEndian);
      JsonObject result = CdrCodec.readFields(reader, fields);
      if (reader.remaining() != 0) {
        throw new IOException("Trailing bytes in CDR sample");
      }
      return result;
    } catch (IOException | IllegalArgumentException | IllegalStateException e) {
      throw new ParseException("Unable to decode CDR sample: " + e.getMessage(), e);
    }
  }

  @Override
  public ParsedObject parse(byte[] payload, ParseMode parseMode) throws ParseException {
    try {
      JsonObject object = parseToJson(payload, parseMode);
      @SuppressWarnings("unchecked")
      Map<String, Object> map = gson.fromJson(object, Map.class);
      return new CdrUint64ParsedObject(new StructuredResolver(new MapResolver(map), object), object, fields);
    } catch (ParseException e) {
      if (parseMode == ParseMode.IGNORE) {
        return new DefaultParser(payload);
      }
      throw e;
    }
  }

  @Override
  public Map<String, Object> getFormat() {
    if (definition == null) {
      return Map.of();
    }
    Map<String, Object> result = new LinkedHashMap<>();
    for (JsonElement field : definition.getAsJsonArray("fields")) {
      JsonObject descriptor = field.getAsJsonObject();
      result.put(descriptor.get("name").getAsString(),
          gson.fromJson(descriptor, Map.class));
    }
    return result;
  }

  private void requireDefinition() throws IOException {
    if (definition == null) {
      throw new IOException("CDR formatter has no schema");
    }
  }
}
