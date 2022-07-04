# Schemas

This package allows for different schema configuration and message formatters.

Current supported protocols are

| Name      | Description                 | Link                                                            |
|-----------|-----------------------------|-----------------------------------------------------------------|
| JSON      | Javascript Notation         | https://www.json.org/json-en.html                               |
| XML       | extensible markup language  | https://www.w3schools.com/xml/xml_whatis.asp                    |
| ProtoBuf  | Google Protobuf             | https://github.com/protocolbuffers/protobuf                     |
| AVRO      | Apache AVRO                 | https://en.wikipedia.org/wiki/Apache_Avro                       |
| CSV       | Comma seperated Values      | https://en.wikipedia.org/wiki/Comma-separated_values            |
| QPID-JMS  | Apache QPID JMS Messages    |                                                                 |
| RAW       | Opaque data load            | This schema does no field loading                               |
| Native    | Single Native Java types    | This schema supports single value like what a sensor might send |

[![SonarCloud](https://sonarcloud.io/images/project_badges/sonarcloud-white.svg)](https://sonarcloud.io/summary/new_code?id=Schemas)