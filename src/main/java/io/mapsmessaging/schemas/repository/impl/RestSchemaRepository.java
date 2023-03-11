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

package io.mapsmessaging.schemas.repository.impl;


import io.mapsmessaging.logging.Logger;
import io.mapsmessaging.logging.LoggerFactory;
import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.config.SchemaConfigFactory;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.NonNull;
import org.json.JSONArray;
import org.json.JSONObject;

public class RestSchemaRepository extends SimpleSchemaRepository {

  private final Logger logger = LoggerFactory.getLogger(RestSchemaRepository.class);

  private final String url;
  private final HttpClient client;

  public RestSchemaRepository(@NonNull String hostUrl) throws IOException, URISyntaxException, InterruptedException {
    url = hostUrl;
    client = HttpClient.newHttpClient();
    loadData();
  }

  private void loadData() throws IOException, URISyntaxException, InterruptedException {
    HttpRequest request = HttpRequest.newBuilder()
        .uri(new URI(url+"/api/v1/server/schema/map"))
        .header("Content-Type", "application/json")
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

  public SchemaConfig getSchema(@NonNull String uuid) {
    SchemaConfig config =  mapByUUID.get(uuid);
    if(config == null){
      // lookup on server
    }
    return config;
  }


  @Override
  public SchemaConfig addSchema(@NonNull String context, @NonNull SchemaConfig config) {
    return super.addSchema(context, config);
  }

  @Override
  public void removeSchema(@NonNull String uuid) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(url+"/api/v1/server/schema/"+uuid))
          .header("Content-Type", "application/json")
          .DELETE().build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      super.removeSchema(uuid);
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void removeAllSchemas() {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(url+"/api/v1/server/schema"))
          .header("Content-Type", "application/json")
          .DELETE().build();
      HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
      super.removeAllSchemas();
    } catch (URISyntaxException | IOException | InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  public static void main(String[] args) throws IOException, URISyntaxException, InterruptedException {
    SimpleSchemaRepository repository = new RestSchemaRepository("http://localhost:8080");

  }

}