# Schemas

## Introduction

When handling different data from different sources the problem arises trying to understand the different event structures and definitions.
Currently, there is Avro from Apache, Protobuf from google, JSON, XML, CSV that can be used to define data in events.

* So which one to use when ingesting data from many sources?
* How to maintain a generic approach to message formats?

This is where this package comes in to play. It provides an abstract ability to pack and unpack data as well as the ability to query fields within the data. Making it simpler to
add new emerging technolgies without rewriting your code.

The MapsMessaging server uses this package to be able to parse and filter in flight data from different sources. It uses the Repository to maintain a list of known schemas and will
replicate the definitions amongst other servers.

It is released here as a standalone package so others can use it as well, either in client side or server side code, or when you simply are not sure on what data you may ingest but
need to be able to query it.

## Supported

This package allows for different schema configuration and message formatters.

Current supported protocols are

| Name     | Description                | Link                                                            |
|----------|----------------------------|-----------------------------------------------------------------|
| JSON     | Javascript Notation        | https://www.json.org/json-en.html                               |
| XML      | extensible markup language | https://www.w3schools.com/xml/xml_whatis.asp                    |
| ProtoBuf | Google Protobuf            | https://github.com/protocolbuffers/protobuf                     |
| AVRO     | Apache AVRO                | https://en.wikipedia.org/wiki/Apache_Avro                       |
| CSV      | Comma seperated Values     | https://en.wikipedia.org/wiki/Comma-separated_values            |
| QPID-JMS | Apache QPID JMS Messages   |                                                                 |
| RAW      | Opaque data load           | This schema does no field loading                               |
| Native   | Single Native Java types   | This schema supports single value like what a sensor might send |

# Usage

## Using Formatters

There are 2 parts to using the formatters

* Admin and Repository
* Lookup and using


### Admin and Repository

The administration side of formatters covers the addition, update and deletion of configuration using a Schema Repository. 
The Schema Repository is simply a central place to store and retrieve Schemas. This could be a RestAPI, a file backed repository or in the case of MapsMessaging the messaging server itself.

Each Schema is defined by a unique id that is used to bind the format to an application 'Context'. For example, in MapsMessaging a Schema is bound to a topic or queue. The topic or queue name is used as the context when binding the schema to the topic or queue.

### Lookup and Using

The schema is retrieved from the Repository as the example below

```java
repository.getSchemaByContext("/root");
```
This would return a list of any schemas bound to '/root'. The list provides the ability to return multiple schemas that could support versions or slightly different configurations that could be received.

or
```java
repository.getSchema(unique_id);
```

This returns the single unique schema defined by the supplied unique_id.

To then parse the data from the byte[] using the schema 

```java
      byte[] data = getData();
      ParsedObject parsedObject = formatter.parse(data);
      parsedObject.get("stringId");
```
This would return the field called "stringId" from the data byte[] 

## Extending Formatters

To add a new, potentially company internal format, simply extend MessageFormatter, below is the JSON implementation.

```java
package io.mapsmessaging.schemas.formatters.impl;

import static io.mapsmessaging.schemas.logging.SchemaLogMessages.FORMATTER_UNEXPECTED_OBJECT;

import io.mapsmessaging.schemas.config.SchemaConfig;
import io.mapsmessaging.schemas.formatters.MessageFormatter;
import io.mapsmessaging.schemas.formatters.ParsedObject;
import io.mapsmessaging.schemas.formatters.walker.MapResolver;
import io.mapsmessaging.schemas.formatters.walker.StructuredResolver;
import java.io.IOException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * The type Json formatter.
 */
public class JsonFormatter extends MessageFormatter {

  /**
   * Instantiates a new Json formatter.
   */
  public JsonFormatter() {
    // Used by the service loader, there is nothing to do
  }

  @Override
  public ParsedObject parse(byte[] payload) {
    JSONObject json;
    try {
      json = new JSONObject(new String(payload));
      return new StructuredResolver(new MapResolver(json.toMap()), json);
    } catch (JSONException e) {
      logger.log(FORMATTER_UNEXPECTED_OBJECT, getName(), payload);
      return new DefaultParser(payload);
    }
  }

  @Override
  public JSONObject parseToJson(byte[] payload) throws IOException {
    return new JSONObject(new String(payload));
  }

  @Override
  public MessageFormatter getInstance(SchemaConfig config) throws IOException {
    return this;
  }

  @Override
  public String getName() {
    return "JSON";
  }

}

```

Then simply add the new formatter class into the file in resources/META-INF.servers/io.mapsmessaging.schemas.formatters.MessageFormatter

```text
io.mapsmessaging.schemas.formatters.impl.AvroFormatter
io.mapsmessaging.schemas.formatters.impl.CsvFormatter
io.mapsmessaging.schemas.formatters.impl.JsonFormatter
io.mapsmessaging.schemas.formatters.impl.NativeFormatter
io.mapsmessaging.schemas.formatters.impl.ProtoBufFormatter
io.mapsmessaging.schemas.formatters.impl.RawFormatter
io.mapsmessaging.schemas.formatters.impl.XmlFormatter

```

This allows data in a byte[] to be parsed, the next step is to be able to pack and unpack the configuration for the new formatter.
Since all formatters are slightly different the configuration required will be different, for this you extend the class SchemaConfig.

```java
public class JsonSchemaConfig extends SimpleSchemaConfig {

  private static final String NAME = "JSON";

  /**
   * Instantiates a new Json schema config.
   */
  public JsonSchemaConfig() {
    super(NAME);
    setMimeType("application/json");
  }

  private JsonSchemaConfig(Map<String, Object> config) {
    super(NAME, config);
  }

  protected SchemaConfig getInstance(Map<String, Object> config) {
    return new JsonSchemaConfig(config);
  }
}
```

Then add it to the file resources/META-INF.servers/io.mapsmessaging.schemas.config.SchemaConfig as follows

```text
io.mapsmessaging.schemas.config.impl.AvroSchemaConfig
io.mapsmessaging.schemas.config.impl.CsvSchemaConfig
io.mapsmessaging.schemas.config.impl.JsonSchemaConfig
io.mapsmessaging.schemas.config.impl.NativeSchemaConfig
io.mapsmessaging.schemas.config.impl.ProtoBufSchemaConfig
io.mapsmessaging.schemas.config.impl.RawSchemaConfig
io.mapsmessaging.schemas.config.impl.XmlSchemaConfig
```

For more complex implementations refer to the Avro or the Protobuf implementation.

## pom.xml setup

All MapsMessaging libraries are hosted on the [maven central server.](https://central.sonatype.com/search?smo=true&q=mapsmessaging)

Include the dependency

``` xml
    <!-- Message Schema module -->
    <dependency>
      <groupId>io.mapsmessaging</groupId>
      <artifactId>Schemas</artifactId>
      <version>2.0.7</version>
    </dependency>
```    

[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/summary/new_code?id=Schemas)