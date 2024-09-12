/*
 * Copyright [ 2020 - 2024 ] [Matthew Buckton]
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package io.mapsmessaging.schemas.repository.impl;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import io.mapsmessaging.schemas.config.impl.JsonSchemaConfig;
import lombok.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.UUID;

public class RestSchemaRepository extends SimpleSchemaRepository {

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
        .uri(new URI(url+"/api/v1/schema/map"))
        .header(CONTENT_TYPE_HEADER, CONTENT_TYPE)
        .GET()
        .build();

    HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
    String body = response.body();
    JSONObject jsonObject = new JSONObject(body);
    if(jsonObject.has("data")){
      JSONObject schemaMap = jsonObject.getJSONObject("data");
      for(String key: schemaMap.keySet()){
        JSONArray array = schemaMap.getJSONArray(key);
        for(int x =0;x<array.length();x++){
          JSONObject schema = new JSONObject(array.getString(x));
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
        if(response.statusCode() >= 200 && response.statusCode() < 300) {
          JSONObject jsonObject = new JSONObject(body);
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
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("context", context);
      jsonObject.put("schema", config.pack());
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(url+"/api/v1/schema"))
          .header(CONTENT_TYPE_HEADER, CONTENT_TYPE)
          .POST(BodyPublishers.ofString(jsonObject.toString(2)))
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