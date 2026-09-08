/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.db.analytics.client.exception.ClientException;
import com.scalar.db.analytics.client.exception.ErrorDetail.DataSourceDefinitionError;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Handles file prefix substitution for provider JSON payloads.
 *
 * <p>Supports ${file:path/to/file} syntax where:
 *
 * <ul>
 *   <li>.properties files are loaded and converted to JSON object with string values
 *   <li>.json files are loaded as-is (any valid JSON structure)
 * </ul>
 *
 * <p>Substitution is performed at the JsonNode level, traversing the JSON tree and replacing any
 * string node matching the ${file:...} pattern with the contents of the referenced file.
 */
public class FilePrefixSubstitutor {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
  private static final Pattern FILE_PREFIX_PATTERN = Pattern.compile("\\$\\{file:(.+)\\}");

  /**
   * Substitutes all ${file:...} references in the given JsonNode with the contents of the
   * referenced files.
   *
   * @param node the JsonNode containing ${file:...} references
   * @return a new JsonNode with all file references resolved
   * @throws ClientException if file loading or parsing fails
   */
  public static JsonNode substitute(JsonNode node) {
    return traverse(node);
  }

  private static JsonNode traverse(JsonNode node) {
    if (node.isTextual()) {
      return substituteIfFileReference(node);
    } else if (node.isObject()) {
      return traverseObject(node);
    } else if (node.isArray()) {
      return traverseArray(node);
    }
    // For number, boolean, null nodes, return as-is
    return node;
  }

  private static JsonNode substituteIfFileReference(JsonNode textNode) {
    String value = textNode.asText();
    Matcher matcher = FILE_PREFIX_PATTERN.matcher(value);
    if (matcher.matches()) {
      String filePath = matcher.group(1);
      return loadFileAsJsonNode(filePath);
    }
    return textNode;
  }

  private static JsonNode traverseObject(JsonNode objectNode) {
    ObjectNode result = OBJECT_MAPPER.createObjectNode();
    objectNode
        .fieldNames()
        .forEachRemaining(
            fieldName -> {
              result.set(fieldName, traverse(objectNode.get(fieldName)));
            });
    return result;
  }

  private static JsonNode traverseArray(JsonNode arrayNode) {
    ArrayNode result = OBJECT_MAPPER.createArrayNode();
    arrayNode.forEach(item -> result.add(traverse(item)));
    return result;
  }

  private static JsonNode loadFileAsJsonNode(String filePath) {
    Path path = Paths.get(filePath);

    if (!Files.exists(path)) {
      throw new ClientException(new DataSourceDefinitionError("File not found: " + filePath));
    }

    if (!Files.isRegularFile(path)) {
      throw new ClientException(new DataSourceDefinitionError("Not a regular file: " + filePath));
    }

    Path fileNamePath = path.getFileName();
    if (fileNamePath == null) {
      throw new ClientException(new DataSourceDefinitionError("Invalid file path: " + filePath));
    }
    String fileName = fileNamePath.toString();
    if (fileName.endsWith(".properties")) {
      return loadPropertiesFileAsJsonNode(path);
    } else if (fileName.endsWith(".json")) {
      return loadJsonFileAsJsonNode(path);
    } else {
      throw new ClientException(
          new DataSourceDefinitionError(
              "Unsupported file type: "
                  + fileName
                  + ". Only .properties and .json are supported."));
    }
  }

  private static JsonNode loadPropertiesFileAsJsonNode(Path path) {
    Properties properties = new Properties();
    try (FileInputStream fis = new FileInputStream(path.toFile())) {
      properties.load(fis);
    } catch (IOException e) {
      throw new ClientException(
          new DataSourceDefinitionError(
              "Failed to load properties file: " + path + ". " + e.getMessage(), e));
    }

    // Convert Properties to ObjectNode
    return OBJECT_MAPPER.valueToTree(properties);
  }

  private static JsonNode loadJsonFileAsJsonNode(Path path) {
    try {
      // Load JSON file as-is, accepting any valid JSON structure
      return OBJECT_MAPPER.readTree(path.toFile());
    } catch (IOException e) {
      throw new ClientException(
          new DataSourceDefinitionError(
              "Failed to load JSON file: " + path + ". " + e.getMessage(), e));
    }
  }
}
