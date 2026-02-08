/*
 *
 *     Copyright [ 2020 - 2026 ] [Matthew Buckton]
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

package io.mapsmessaging.schemas.config;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.mapsmessaging.schemas.config.impl.*;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class GsonFactory {

  private GsonFactory() {
  }

  public static Gson buildGson() {
    return new GsonBuilder()
        .setPrettyPrinting()
        .disableHtmlEscaping()
        .registerTypeAdapterFactory(
            new SchemaConfigPolymorphicFactory("format")
                .registerSubtype("json", JsonSchemaConfig.class)
                .registerSubtype("avro", AvroSchemaConfig.class)
                .registerSubtype("protobuf", ProtoBufSchemaConfig.class)
                .registerSubtype("csv", CsvSchemaConfig.class)
                .registerSubtype("xml", XmlSchemaConfig.class)
                .registerSubtype("cbor", CborSchemaConfig.class)
                .registerSubtype("messagepack", MessagePackSchemaConfig.class)
                .registerSubtype("native", NativeSchemaConfig.class)
                .registerSubtype("cbc", CbcSchemaConfig.class)
                .registerSubtype("raw", RawSchemaConfig.class)
                .registerSubtype("mavlink", MavlinkSchemaConfig.class)
                .registerSubtype("n2k", N2kSchemaConfig.class)
        )
        .create();
  }

  /**
   * Minimal, case-insensitive polymorphic adapter for SchemaConfig keyed by a discriminator field.
   * Reads the JSON object, looks up the subtype by lowercase label, and delegates.
   */
  private static final class SchemaConfigPolymorphicFactory implements TypeAdapterFactory {
    private final String discriminatorFieldName;
    private final Map<String, Class<? extends SchemaConfig>> labelToSubtype = new LinkedHashMap<>();

    SchemaConfigPolymorphicFactory(String discriminatorFieldName) {
      this.discriminatorFieldName = discriminatorFieldName;
    }

    SchemaConfigPolymorphicFactory registerSubtype(String label, Class<? extends SchemaConfig> subtype) {
      labelToSubtype.put(label.toLowerCase(Locale.ROOT), subtype);
      return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> typeToken) {
      if (!SchemaConfig.class.isAssignableFrom(typeToken.getRawType())) {
        return null;
      }

      return (TypeAdapter<T>) new TypeAdapter<SchemaConfig>() {
        @Override
        public void write(JsonWriter out, SchemaConfig value) throws IOException {
          // Delegate to the concrete type adapter so normal @SerializedName etc. are respected.
          if (value == null) {
            out.nullValue();
          } else {
            Class<? extends SchemaConfig> runtimeType = value.getClass();
            TypeAdapter<SchemaConfig> delegate =
                (TypeAdapter<SchemaConfig>) gson.getDelegateAdapter(SchemaConfigPolymorphicFactory.this, TypeToken.get(runtimeType));
            delegate.write(out, value);
          }
        }

        @Override
        public SchemaConfig read(JsonReader in) throws IOException {
          JsonElement element = JsonParser.parseReader(in);
          if (!element.isJsonObject()) {
            throw new JsonParseException("Expected JSON object for SchemaConfig");
          }
          JsonObject object = element.getAsJsonObject();

          JsonElement discriminator = object.get(discriminatorFieldName);
          if (discriminator == null || discriminator.isJsonNull()) {
            throw new JsonParseException("Missing discriminator field '" + discriminatorFieldName + "'");
          }

          String label = discriminator.getAsString().toLowerCase(Locale.ROOT);
          Class<? extends SchemaConfig> targetClass = labelToSubtype.get(label);
          if (targetClass == null) {
            throw new JsonParseException("Unknown format '" + label + "'. Supported: " + labelToSubtype.keySet());
          }

          TypeAdapter<? extends SchemaConfig> delegate =
              gson.getDelegateAdapter(SchemaConfigPolymorphicFactory.this, TypeToken.get(targetClass));
          return delegate.fromJsonTree(object);
        }
      };
    }
  }
}
