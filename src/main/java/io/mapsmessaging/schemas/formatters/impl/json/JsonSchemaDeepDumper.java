/*
 *
 *  Copyright [ 2020 - 2024 ] Matthew Buckton
 *  Copyright [ 2024 - 2026 ] MapsMessaging B.V.
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

package io.mapsmessaging.schemas.formatters.impl.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public final class JsonSchemaDeepDumper {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private JsonSchemaDeepDumper() {
  }

  public static ObjectNode dumpSchema(JsonNode rootSchemaNode, JsonNode selectedSchemaNode, String definitionPointer) {
    ObjectNode expandedSchema = OBJECT_MAPPER.createObjectNode();

    if (rootSchemaNode == null) {
      expandedSchema.put("type", "object");
      return expandedSchema;
    }

    JsonNode startSchemaNode = selectedSchemaNode;
    if (startSchemaNode == null) {
      startSchemaNode = rootSchemaNode;
    }

    copyRootMetadata(rootSchemaNode, expandedSchema);

    JsonNode expandedNode = expandNode(rootSchemaNode, startSchemaNode, new HashSet<>());

    if (expandedNode != null && expandedNode.isObject()) {
      expandedSchema.setAll((ObjectNode) expandedNode);
    } else {
      expandedSchema.set("const", expandedNode);
    }

    expandedSchema.remove("$defs");
    expandedSchema.remove("definitions");

    return expandedSchema;
  }

  private static void copyRootMetadata(JsonNode rootSchemaNode, ObjectNode expandedSchema) {
    copyFieldIfPresent(rootSchemaNode, expandedSchema, "$schema");
    copyFieldIfPresent(rootSchemaNode, expandedSchema, "$id");
  }

  private static void copyFieldIfPresent(JsonNode sourceNode, ObjectNode targetNode, String fieldName) {
    if (sourceNode.has(fieldName)) {
      targetNode.set(fieldName, sourceNode.get(fieldName).deepCopy());
    }
  }

  private static JsonNode expandNode(JsonNode rootSchemaNode, JsonNode currentNode, Set<String> activeReferences) {
    if (currentNode == null || currentNode.isNull()) {
      return currentNode;
    }

    if (currentNode.isObject()) {
      return expandObject(rootSchemaNode, currentNode, activeReferences);
    }

    if (currentNode.isArray()) {
      return expandArray(rootSchemaNode, currentNode, activeReferences);
    }

    return currentNode.deepCopy();
  }

  private static JsonNode expandObject(JsonNode rootSchemaNode, JsonNode currentNode, Set<String> activeReferences) {
    if (currentNode.has("$ref")) {
      return expandReferenceObject(rootSchemaNode, currentNode, activeReferences);
    }

    ObjectNode expandedObject = OBJECT_MAPPER.createObjectNode();

    Iterator<Map.Entry<String, JsonNode>> fieldIterator = currentNode.fields();
    while (fieldIterator.hasNext()) {
      Map.Entry<String, JsonNode> field = fieldIterator.next();
      String fieldName = field.getKey();

      if ("$defs".equals(fieldName) || "definitions".equals(fieldName)) {
        continue;
      }

      JsonNode expandedFieldValue = expandNode(rootSchemaNode, field.getValue(), activeReferences);
      expandedObject.set(fieldName, expandedFieldValue);
    }

    return expandedObject;
  }

  private static JsonNode expandReferenceObject(JsonNode rootSchemaNode, JsonNode currentNode, Set<String> activeReferences) {
    String reference = currentNode.get("$ref").asText();
    JsonNode resolvedReferenceNode = resolveReference(rootSchemaNode, reference);

    if (resolvedReferenceNode == null) {
      return currentNode.deepCopy();
    }

    if (activeReferences.contains(reference)) {
      ObjectNode recursiveReference = OBJECT_MAPPER.createObjectNode();
      recursiveReference.put("$ref", reference);
      return recursiveReference;
    }

    activeReferences.add(reference);
    JsonNode expandedReferenceNode = expandNode(rootSchemaNode, resolvedReferenceNode, activeReferences);
    activeReferences.remove(reference);

    if (!expandedReferenceNode.isObject()) {
      return expandedReferenceNode;
    }

    ObjectNode mergedObject = OBJECT_MAPPER.createObjectNode();
    mergedObject.setAll((ObjectNode) expandedReferenceNode);

    Iterator<Map.Entry<String, JsonNode>> fieldIterator = currentNode.fields();
    while (fieldIterator.hasNext()) {
      Map.Entry<String, JsonNode> field = fieldIterator.next();
      String fieldName = field.getKey();

      if ("$ref".equals(fieldName)) {
        continue;
      }

      if ("$defs".equals(fieldName) || "definitions".equals(fieldName)) {
        continue;
      }

      JsonNode expandedFieldValue = expandNode(rootSchemaNode, field.getValue(), activeReferences);
      mergedObject.set(fieldName, expandedFieldValue);
    }

    return mergedObject;
  }

  private static JsonNode expandArray(JsonNode rootSchemaNode, JsonNode currentNode, Set<String> activeReferences) {
    ArrayNode expandedArray = OBJECT_MAPPER.createArrayNode();

    for (JsonNode itemNode : currentNode) {
      JsonNode expandedItemNode = expandNode(rootSchemaNode, itemNode, activeReferences);
      expandedArray.add(expandedItemNode);
    }

    return expandedArray;
  }

  private static JsonNode resolveReference(JsonNode rootSchemaNode, String reference) {
    if (reference == null || reference.isEmpty()) {
      return null;
    }

    String fragment = extractFragment(reference);
    if (fragment == null) {
      return null;
    }

    return resolvePointer(rootSchemaNode, fragment);
  }

  private static String extractFragment(String reference) {
    if ("#".equals(reference)) {
      return "#";
    }

    if (reference.startsWith("#/")) {
      return reference;
    }

    int fragmentIndex = reference.indexOf('#');
    if (fragmentIndex >= 0) {
      String fragment = reference.substring(fragmentIndex);
      if (fragment.isEmpty()) {
        return "#";
      }
      return fragment;
    }

    return null;
  }

  private static JsonNode resolvePointer(JsonNode rootSchemaNode, String pointer) {
    if ("#".equals(pointer)) {
      return rootSchemaNode;
    }

    if (!pointer.startsWith("#/")) {
      return null;
    }

    JsonNode currentNode = rootSchemaNode;
    String[] parts = pointer.substring(2).split("/");

    for (String part : parts) {
      String token = unescape(part);

      if (currentNode == null) {
        return null;
      }

      if (currentNode.isObject()) {
        currentNode = currentNode.get(token);
        continue;
      }

      if (currentNode.isArray()) {
        Integer index = parseArrayIndex(token);
        if (index == null || index < 0 || index >= currentNode.size()) {
          return null;
        }

        currentNode = currentNode.get(index);
        continue;
      }

      return null;
    }

    return currentNode;
  }

  private static Integer parseArrayIndex(String token) {
    try {
      return Integer.parseInt(token);
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private static String unescape(String token) {
    return token.replace("~1", "/").replace("~0", "~");
  }
}