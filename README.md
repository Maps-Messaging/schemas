# MapsMessaging Schemas

## Introduction

Messages arrive in many formats, with different field types, layouts and encodings. The MapsMessaging Schemas library provides a common interface for configuring payload formats, encoding and decoding messages, and accessing fields for filtering and processing.

It is used by the MapsMessaging server and can also be embedded in other applications. Schema configurations may be stored in a repository and bound to contexts such as topics or queues.

## Supported formats

These **14 payload formatters** are registered in `src/main/resources/META-INF/services/io.mapsmessaging.schemas.formatters.MessageFormatter` on `development`:

| Format | Description | Reference |
|---|---|---|
| Avro | Apache Avro schema-based binary encoding | [Apache Avro](https://avro.apache.org/) |
| CANAerospace | CANAerospace message data | [CANAerospace](https://www.canaerospace.net/) |
| CANbus | CAN frame data with identifier interpretation | [CAN](https://en.wikipedia.org/wiki/CAN_bus) |
| CBC | Compact Binary Coding; schema-defined bit-packed fields | [CBC documentation](https://github.com/Maps-Messaging/docs-site/blob/main/docs/schemas/impl/cbc.md) |
| CBOR | Concise Binary Object Representation | [RFC 8949](https://www.rfc-editor.org/rfc/rfc8949) |
| CDR | DDS Common Data Representation; plain XCDR1 with IDL or JSON schema definitions | [CDR documentation](https://github.com/Maps-Messaging/docs-site/blob/main/docs/schemas/impl/cdr.md) |
| CSV | Comma-separated values with configurable fields | [RFC 4180](https://www.rfc-editor.org/rfc/rfc4180) |
| JSON | JSON payloads, optionally validated against JSON Schema | [JSON](https://www.json.org/) |
| MAVLink | MAVLink frames and dialect-defined messages | [MAVLink](https://mavlink.io/) |
| MessagePack | Binary encoding of JSON-like values | [MessagePack](https://msgpack.org/) |
| Native | Single Java scalar values (integer, float, double or string) | |
| Protobuf | Google Protocol Buffers with compiled descriptors | [Protocol Buffers](https://protobuf.dev/) |
| RAW | Opaque bytes without structured field decoding | |
| XML | Extensible Markup Language payloads | [XML](https://www.w3.org/XML/) |

These are **payload formats**, not transport or messaging protocols. Qpid-JMS is not a separately registered schema formatter.

Capabilities vary by format. CDR, for example, supports plain XCDR1, not XCDR2 or a complete DDS/ROS 2 client. See the [format-specific schema documentation](https://github.com/Maps-Messaging/docs-site/tree/main/docs/schemas) for details.

## Usage

### Schema configuration and lookup

A `SchemaConfig` contains format selection, schema definition, version and metadata. A schema repository can associate configurations with contexts such as topics or queues and retrieve them by context or schema ID.

Given a `SchemaConfig`, request the corresponding formatter from the service-loaded factory:

```java
MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);
```

If a format requires parent or related schema resolution, supply a `SchemaResolver`:

```java
MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config, schemaResolver);
```

### Encoding, decoding and field lookup

```java
JsonObject value = new JsonObject();
value.addProperty("stringId", "sensor-1");

byte[] encoded = formatter.parseFromJson(value);
JsonObject decoded = formatter.parseToJson(encoded, ParseMode.STRICT);
ParsedObject parsed = formatter.parse(encoded, ParseMode.STRICT);
Object identifier = parsed.get("stringId");
```

- `parseFromJson(JsonObject)` encodes a JSON object into the configured wire format.
- `parseToJson(byte[], ParseMode)` decodes a payload into a JSON object.
- `parse(byte[], ParseMode)` returns a `ParsedObject` for field lookup and selector evaluation.
- `getFormat()` exposes format information where implemented.

`ParseMode.STRICT` reports parsing errors with `ParseException`. Some implementations provide an opaque-payload fallback with `ParseMode.IGNORE`; this behaviour is format-specific.

### CDR with an IDL definition

CDR accepts a supported IDL definition, a native ordered CDR field schema, or JSON Schema with explicit CDR type and order metadata.

```java
CdrSchemaConfig config = new CdrSchemaConfig();
config.setIdl("struct Reading { unsigned long id; double temperature; };", "Reading");

MessageFormatter formatter = MessageFormatterFactory.getInstance().getFormatter(config);

JsonObject sample = new JsonObject();
sample.addProperty("id", 42);
sample.addProperty("temperature", 21.5);

byte[] payload = formatter.parseFromJson(sample);
JsonObject decoded = formatter.parseToJson(payload, ParseMode.STRICT);
```

The IDL is compiled into an ordered field layout when the formatter is constructed. The same layout is reused for subsequent messages. See the [CDR schema guide](https://github.com/Maps-Messaging/docs-site/blob/main/docs/schemas/impl/cdr.md) for supported types and limitations.

## Extending formatters

To add another payload format:

1. Extend `MessageFormatter` and implement `getName()`, `getInstance(SchemaConfig, SchemaResolver)`, `parseToJson(byte[], ParseMode)`, `parseFromJson(JsonObject)`, `parse(byte[], ParseMode)` and `getFormat()`.
2. Provide a no-argument constructor and register the implementation in `src/main/resources/META-INF/services/io.mapsmessaging.schemas.formatters.MessageFormatter`.
3. If the format needs configuration, extend `SchemaConfig` and register the configuration class in `src/main/resources/META-INF/services/io.mapsmessaging.schemas.config.SchemaConfig`.
4. Test configuration construction, factory discovery, binary/JSON round trips, malformed input and `ParsedObject` field lookup. The shared Person tests provide an example.

See `JsonFormatter`, `CdrFormatter`, `AvroFormatter` and `ProtoBufFormatter` for different implementations.

## Maven

Current **development** coordinates from `pom.xml`:

```xml
<dependency>
  <groupId>io.mapsmessaging</groupId>
  <artifactId>schemas</artifactId>
  <version>3.2.0-SNAPSHOT</version>
</dependency>
```

This is the development version, not a claim that this snapshot is published to Maven Central. Select a published release or configure the MapsMessaging snapshot repository as appropriate.

[Published artifacts on Maven Central](https://central.sonatype.com/search?q=io.mapsmessaging)

[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/summary/new_code?id=Schemas)
