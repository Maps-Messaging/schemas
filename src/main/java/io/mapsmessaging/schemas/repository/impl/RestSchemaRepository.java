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

package io.mapsmessaging.schemas.repository.impl;

import com.google.gson.*;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import lombok.NonNull;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

public class RestSchemaRepository extends SimpleSchemaRepository {
  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
  private static final String CONTENT_TYPE_HEADER = "Content-Type";
  private static final String CONTENT_TYPE = "application/json";

  private final String url;
  private final HttpClient client;

  public RestSchemaRepository(@NonNull String hostUrl) throws IOException, URISyntaxException, InterruptedException {
    url = hostUrl;
    client = HttpClient.newHttpClient();
    loadData();
  }

  private void loadData() throws IOException, URISyntaxException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(url + "/api/v1/schema/map"))
        .header(CONTENT_TYPE_HEADER, CONTENT_TYPE)
        .GET()
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    String body = response.body();

    JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();
    if (jsonObject.has("data") && jsonObject.get("data").isJsonObject()) {
      JsonObject schemaMap = jsonObject.getAsJsonObject("data");

      for (Map.Entry<String, JsonElement> entry : schemaMap.entrySet()) {
        String key = entry.getKey();
        JsonArray array = entry.getValue().getAsJsonArray();

        for (JsonElement element : array) {
          JsonObject schema = JsonParser.parseString(element.getAsString()).getAsJsonObject();
          super.addSchema(key, SchemaConfigFactory.getInstance().constructConfig(schema));
        }
      }
    }
  }


  @Override
  public SchemaConfig getSchema(@NonNull String uuid) {
    SchemaConfig config =  mapByUUID.get(uuid);
    if(config == null){
      try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(new URI(url+"/api/v1/schema/"+uuid))
            .header(CONTENT_TYPE_HEADER, CONTENT_TYPE)
            .GET()
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String body = response.body();
        if (response.statusCode() >= 200 && response.statusCode() < 300) {
          JsonObject jsonObject = JsonParser.parseString(body).getAsJsonObject();
          config = SchemaConfigFactory.getInstance().constructConfig(jsonObject);
          super.addSchema("/", config);
        }

      } catch (IOException | URISyntaxException | InterruptedException e) {
        // to do
        Thread.currentThread().interrupt();
      }
    }
    return config;
  }

  @Override
  public void removeSchema(@NonNull String uuid) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(url+"/api/v1/schema/"+uuid))
          .header(CONTENT_TYPE_HEADER, CONTENT_TYPE)
          .DELETE().build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if(response.statusCode() >= 200 && response.statusCode() < 300) {
        super.removeSchema(uuid);
      }
    } catch (URISyntaxException | IOException | InterruptedException e) {
      // log it
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public void removeAllSchemas() {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(url+"/api/v1/schema"))
          .header(CONTENT_TYPE_HEADER, CONTENT_TYPE)
          .DELETE().build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if(response.statusCode() >= 200 && response.statusCode() < 300) {
        super.removeAllSchemas();
      }
    } catch (URISyntaxException | IOException | InterruptedException e) {
      // log it
      Thread.currentThread().interrupt();
    }
  }

  @Override
  public SchemaConfig addSchema(@NonNull String context, @NonNull SchemaConfig config) {
    try {
      JsonObject jsonObject = new JsonObject();
      jsonObject.addProperty("context", context);
      jsonObject.add("schema", JsonParser.parseString(config.pack()));

      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(url + "/api/v1/schema"))
          .header(CONTENT_TYPE_HEADER, CONTENT_TYPE)
          .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(jsonObject)))
          .build();

      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      if(response.statusCode() >= 200 && response.statusCode() < 300) {
        return super.addSchema(context, config);
      }
    } catch (URISyntaxException | IOException | InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return null;
  }

  public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
    SimpleSchemaRepository repository = new RestSchemaRepository("http://localhost:8080");
    JsonSchemaConfig json = new JsonSchemaConfig();
    json.setUniqueId(UUID.randomUUID());
    repository.addSchema("/root", json);
    for(SchemaConfig config:repository.getAll()){
      if(config.getFormat().equalsIgnoreCase("json")){
        repository.removeSchema(config.getUniqueId());
      }
    }
  }
}