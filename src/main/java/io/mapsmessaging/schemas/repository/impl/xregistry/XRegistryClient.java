/*
 *  Copyright [ 2020 - 2025 ] Matthew Buckton
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
 */

package io.mapsmessaging.schemas.repository.impl.xregistry;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.mapsmessaging.schemas.model.XRegistrySchema;
import io.mapsmessaging.schemas.model.XRegistrySchemaVersion;
import io.mapsmessaging.schemas.repository.impl.xregistry.model.XRegistryResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

public class XRegistryClient implements AutoCloseable {

  private static final Logger log = LoggerFactory.getLogger(XRegistryClient.class);

  private final XRegistryConfig config;
  private final HttpClient httpClient;
  private final ObjectMapper objectMapper;

  public XRegistryClient(XRegistryConfig config) {
    this.config = config;
    this.httpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(10))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .version(HttpClient.Version.HTTP_1_1)
        .build();
    this.objectMapper = new ObjectMapper();
  }

  private HttpRequest.Builder baseBuilder(String url) {
    HttpRequest.Builder builder = HttpRequest.newBuilder()
        .uri(URI.create(url))
        .timeout(Duration.ofSeconds(30));
    if (config.getApiKey() != null) {
      builder.header("Authorization", "Bearer " + config.getApiKey());
    }
    return builder;
  }

  public XRegistrySchemaVersion registerSchema(String schemaId, String schemaFormat, String schemaContent, String description)
      throws IOException {

    String url = String.format("%s/groups/%s/schemas", config.getBaseUrl(), config.getGroupName());

    XRegistrySchema body = new XRegistrySchema();
    body.setSchemaId(schemaId);
    body.setFormat(schemaFormat);
    body.setSchema(schemaContent);
    body.setDescription(description);

    String jsonBody = objectMapper.writeValueAsString(body);

    HttpRequest request = baseBuilder(url)
        .header("Content-Type", "application/json")
        .header("xRegistry-id", schemaId)
        .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      int statusCode = response.statusCode();
      String responseBody = response.body();
      if (statusCode >= 200 && statusCode < 300) {
        log.info("Schema registered: {}", schemaId);
        return objectMapper.readValue(responseBody, XRegistrySchemaVersion.class);
      }
      throw new IOException("Failed to register schema: " + statusCode + " - " + responseBody);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted during registerSchema", e);
    }
  }

  public XRegistrySchemaVersion getSchema(String schemaId, String version) throws IOException {
    String url = (version != null)
        ? String.format("%s/groups/%s/schemas/%s/versions/%s", config.getBaseUrl(), config.getGroupName(), schemaId, version)
        : String.format("%s/groups/%s/schemas/%s", config.getBaseUrl(), config.getGroupName(), schemaId);

    HttpRequest request = baseBuilder(url).GET().build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      int statusCode = response.statusCode();
      if (statusCode == 404) {
        return null;
      }
      if (statusCode >= 200 && statusCode < 300) {
        String responseBody = response.body();
        return objectMapper.readValue(responseBody, XRegistrySchemaVersion.class);
      }
      throw new IOException("Failed to get schema: " + statusCode);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted during getSchema", e);
    }
  }

  public List<XRegistrySchemaVersion> listSchemas() throws IOException {
    String url = String.format("%s/groups/%s/schemas", config.getBaseUrl(), config.getGroupName());

    HttpRequest request = baseBuilder(url).GET().build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      int statusCode = response.statusCode();
      if (statusCode >= 200 && statusCode < 300) {
        String responseBody = response.body();
        XRegistryResponse wrapper = objectMapper.readValue(responseBody, XRegistryResponse.class);
        return wrapper.getSchemas();
      }
      throw new IOException("Failed to list schemas: " + statusCode);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted during listSchemas", e);
    }
  }

  public void deleteSchema(String schemaId) throws IOException {
    String url = String.format("%s/groups/%s/schemas/%s", config.getBaseUrl(), config.getGroupName(), schemaId);

    HttpRequest request = baseBuilder(url).DELETE().build();

    try {
      HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());
      int statusCode = response.statusCode();
      if (statusCode >= 200 && statusCode < 300) {
        log.info("Schema deleted: {}", schemaId);
        return;
      }
      throw new IOException("Failed to delete schema: " + statusCode);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted during deleteSchema", e);
    }
  }

  public XRegistrySchema updateSchema(String schemaId, String schemaFormat, String schemaContent) throws IOException {
    String url = String.format("%s/groups/%s/schemas/%s", config.getBaseUrl(), config.getGroupName(), schemaId);

    XRegistrySchema body = new XRegistrySchema();
    body.setSchemaId(schemaId);
    body.setFormat(schemaFormat);
    body.setSchema(schemaContent);

    String jsonBody = objectMapper.writeValueAsString(body);

    HttpRequest request = baseBuilder(url)
        .header("Content-Type", "application/json")
        .PUT(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
        .build();

    try {
      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
      int statusCode = response.statusCode();
      String responseBody = response.body();
      if (statusCode >= 200 && statusCode < 300) {
        log.info("Schema updated: {}", schemaId);
        return objectMapper.readValue(responseBody, XRegistrySchema.class);
      }
      throw new IOException("Failed to update schema: " + statusCode + " - " + responseBody);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted during updateSchema", e);
    }
  }

  @Override
  public void close() {
    // JDK HttpClient has nothing to close. Leaving this for compatibility.
  }
}
