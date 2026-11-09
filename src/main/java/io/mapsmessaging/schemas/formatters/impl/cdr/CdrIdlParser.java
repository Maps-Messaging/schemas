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
package io.mapsmessaging.schemas.formatters.impl.cdr;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Compiles the supported IDL struct/typedef/module subset into ordered CDR field descriptors.
 * Unsupported declarations fail explicitly instead of being skipped.
 */
public final class CdrIdlParser {
  private static final Pattern TOKEN = Pattern.compile("\\s+|//[^\\r\\n]*|/\\*[\\s\\S]*?\\*/|::|[A-Za-z_][A-Za-z_0-9]*|[0-9]+|[{}<>;,\\[\\]]");
  private static final int MAX_TOKENS = 20_000;
  private static final int MAX_DEPTH = 64;

  private final List<String> tokens = new ArrayList<>();
  private final Map<String, JsonArray> structs = new LinkedHashMap<>();
  private final Map<String, JsonObject> aliases = new HashMap<>();
  private int index;

  private CdrIdlParser(String idl) throws IOException {
    if (idl == null || idl.isBlank()) {
      throw new IOException("IDL definition is empty");
    }
    Matcher matcher = TOKEN.matcher(idl);
    for (int offset = 0; offset < idl.length();) {
      matcher.region(offset, idl.length());
      if (!matcher.lookingAt()) {
        throw new IOException("Unexpected IDL character at offset " + offset);
      }
      String token = matcher.group();
      if (!token.isBlank() && !token.startsWith("//") && !token.startsWith("/*")) {
        if (tokens.size() >= MAX_TOKENS) {
          throw new IOException("IDL token limit exceeded");
        }
        tokens.add(token);
      }
      offset = matcher.end();
    }
  }

  public static JsonObject parse(String idl, String typeName) throws IOException {
    CdrIdlParser parser = new CdrIdlParser(idl);
    parser.declarations("");
    if (parser.index != parser.tokens.size()) {
      throw parser.error("Unexpected trailing IDL tokens");
    }
    String selected;
    if (typeName == null || typeName.isBlank()) {
      if (parser.structs.size() != 1) {
        throw new IOException("IDL typeName is required when there are zero or multiple structs");
      }
      selected = parser.structs.keySet().iterator().next();
    } else {
      selected = typeName.startsWith("::") ? typeName.substring(2) : typeName;
    }
    if (!parser.structs.containsKey(selected)) {
      throw new IOException("Unknown IDL struct: " + selected);
    }
    JsonObject result = new JsonObject();
    result.addProperty("name", selected);
    result.add("fields", parser.expandFields(parser.structs.get(selected), selected, new HashSet<>(), 0));
    return result;
  }

  private void declarations(String scope) throws IOException {
    while (index < tokens.size() && !peek("}")) {
      if (accept("module")) {
        String module = identifier();
        expect("{");
        declarations(scope + module + "::");
        expect("}");
        expect(";");
      } else if (accept("struct")) {
        String name = identifier();
        String qualified = scope + name;
        expect("{");
        JsonArray fields = new JsonArray();
        while (!peek("}")) {
          if (index >= tokens.size()) {
            throw error("Unterminated struct " + name);
          }
          fields.addAll(memberFields(scope));
        }
        expect("}");
        expect(";");
        if (structs.putIfAbsent(qualified, fields) != null || aliases.containsKey(qualified)) {
          throw error("Duplicate IDL definition " + qualified);
        }
      } else if (accept("typedef")) {
        JsonObject type = type(scope);
        type = arrays(type);
        String name = pendingName;
        pendingName = null;
        expect(";");
        String qualified = scope + name;
        if (aliases.putIfAbsent(qualified, type) != null || structs.containsKey(qualified)) {
          throw error("Duplicate IDL definition " + qualified);
        }
      } else {
        throw error("Unsupported IDL declaration");
      }
    }
  }

  private JsonArray memberFields(String scope) throws IOException {
    JsonObject type = type(scope);
    JsonArray fields = new JsonArray();
    do {
      JsonObject member = arrays(type.deepCopy());
      String name = pendingName;
      pendingName = null;
      member.addProperty("name", name);
      fields.add(member);
    } while (accept(","));
    expect(";");
    return fields;
  }

  private String pendingName;

  private JsonObject arrays(JsonObject type) throws IOException {
    if (pendingName != null) {
      throw error("Invalid declarator");
    }
    pendingName = identifier();
    List<Integer> dimensions = new ArrayList<>();
    while (accept("[")) {
      int count = number();
      if (count <= 0 || count > 1_000_000) {
        throw error("Invalid IDL array dimension");
      }
      expect("]");
      dimensions.add(count);
    }
    for (int i = dimensions.size() - 1; i >= 0; i--) {
      JsonObject array = new JsonObject();
      array.addProperty("type", "array");
      array.addProperty("length", dimensions.get(i));
      array.add("element", type);
      type = array;
    }
    return type;
  }

  private JsonObject type(String scope) throws IOException {
    if (accept("sequence")) {
      expect("<");
      JsonObject item = type(scope);
      if (accept(",")) {
        throw error("Bounded sequences are not yet supported");
      }
      expect(">");
      JsonObject result = new JsonObject();
      result.addProperty("type", "sequence");
      result.add("element", item);
      return result;
    }
    String name = identifier();
    if (name.equals("unsigned")) {
      String width = identifier();
      if (width.equals("long") && accept("long")) {
        name = "uint64";
      } else {
        name = switch (width) {
          case "short" -> "uint16";
          case "long" -> "uint32";
          default -> throw error("Unsupported unsigned IDL type " + width);
        };
      }
    } else if (name.equals("long")) {
      name = accept("long") ? "int64" : "int32";
    } else if (name.equals("short")) {
      name = "int16";
    } else {
      name = switch (name) {
        case "float" -> "float32";
        case "double" -> "float64";
        case "boolean" -> "boolean";
        case "char" -> "char";
        case "octet" -> "octet";
        case "string" -> "string";
        default -> name;
      };
    }
    if (peek("::")) {
      StringBuilder qualified = new StringBuilder(name);
      while (accept("::")) {
        qualified.append("::").append(identifier());
      }
      name = qualified.toString();
    }
    JsonObject result = new JsonObject();
    switch (name) {
      case "uint16", "uint32", "uint64", "int16", "int32", "int64", "float32", "float64", "boolean", "char", "octet", "string" -> result.addProperty("type", name);
      default -> {
        result.addProperty("type", "reference");
        result.addProperty("ref", name);
        result.addProperty("scope", scope);
      }
    }
    if (peek("<")) {
      throw error("Unsupported bounded or parameterized IDL type");
    }
    return result;
  }

  private JsonArray expandFields(JsonArray source, String scope, Set<String> active, int depth) throws IOException {
    if (depth > MAX_DEPTH) {
      throw new IOException("IDL type nesting limit exceeded");
    }
    JsonArray result = new JsonArray();
    for (JsonElement entry : source) {
      result.add(expand(entry.getAsJsonObject(), scope, active, depth));
    }
    return result;
  }

  private JsonObject expand(JsonObject value, String scope, Set<String> active, int depth) throws IOException {
    if (depth > MAX_DEPTH) {
      throw new IOException("IDL type nesting limit exceeded");
    }
    String type = value.get("type").getAsString();
    JsonObject result;
    if (type.equals("reference")) {
      String resolved = lookup(value.get("ref").getAsString(), value.get("scope").getAsString());
      if (!active.add(resolved)) {
        throw new IOException("Recursive IDL definition: " + resolved);
      }
      if (structs.containsKey(resolved)) {
        result = new JsonObject();
        result.addProperty("type", "struct");
        result.add("fields", expandFields(structs.get(resolved), resolved, active, depth + 1));
      } else {
        result = expand(aliases.get(resolved), resolved, active, depth + 1);
      }
      active.remove(resolved);
    } else if (type.equals("sequence") || type.equals("array")) {
      result = new JsonObject();
      result.addProperty("type", type);
      if (value.has("length")) {
        result.add("length", value.get("length").deepCopy());
      }
      result.add("element", expand(value.getAsJsonObject("element"), scope, active, depth + 1));
    } else {
      result = value.deepCopy();
    }
    if (value.has("name")) {
      result.add("name", value.get("name").deepCopy());
    }
    return result;
  }

  private String lookup(String name, String scope) throws IOException {
    if (name.startsWith("::")) {
      name = name.substring(2);
    }
    for (String candidate = scope; ; ) {
      String full = candidate + name;
      if (structs.containsKey(full) || aliases.containsKey(full)) {
        return full;
      }
      int end = candidate.lastIndexOf("::", Math.max(0, candidate.length() - 3));
      if (end < 0) {
        break;
      }
      candidate = candidate.substring(0, end + 2);
    }
    if (structs.containsKey(name) || aliases.containsKey(name)) {
      return name;
    }
    throw new IOException("Unknown IDL type: " + name);
  }

  private boolean peek(String expected) {
    return index < tokens.size() && tokens.get(index).equals(expected);
  }

  private boolean accept(String expected) {
    if (peek(expected)) {
      index++;
      return true;
    }
    return false;
  }

  private void expect(String expected) throws IOException {
    if (!accept(expected)) {
      throw error("Expected '" + expected + "'");
    }
  }

  private String identifier() throws IOException {
    if (index >= tokens.size() || !tokens.get(index).matches("[A-Za-z_][A-Za-z_0-9]*")) {
      throw error("Expected identifier");
    }
    return tokens.get(index++);
  }

  private int number() throws IOException {
    if (index >= tokens.size() || !tokens.get(index).matches("[0-9]+")) {
      throw error("Expected array dimension");
    }
    try {
      return Integer.parseInt(tokens.get(index++));
    } catch (NumberFormatException e) {
      throw error("IDL array dimension overflow");
    }
  }

  private IOException error(String message) {
    return new IOException(message + " at IDL token " + index);
  }
}
