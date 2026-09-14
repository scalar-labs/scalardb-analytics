/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.datatype;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.DataTypeKind;
import lombok.extern.slf4j.Slf4j;

/**
 * Codec for converting between {@link DataType} objects and JSON representation.
 *
 * <p>This class provides a single entry point for JSON serialization and deserialization of data
 * types. It supports two JSON formats for deserialization:
 *
 * <ul>
 *   <li><b>String literal format:</b> Simple type kind as a JSON string (e.g., {@code "TEXT"},
 *       {@code "INT"})
 *   <li><b>Object format:</b> JSON object with {@code kind} field and optional parameters (e.g.,
 *       {@code {"kind":"TEXT"}}, {@code {"kind":"DECIMAL","precision":10,"scale":2}})
 * </ul>
 *
 * <p>The string literal format is convenient for simple types without parameters. For types that
 * require parameters (like DECIMAL), the object format should be used to specify parameters
 * explicitly. When deserializing string literals for parameterized types, default values are
 * applied (DECIMAL: precision=38, scale=0).
 *
 * <p>Serialization always produces the object format for consistency and to preserve all type
 * parameters.
 *
 * <p><b>Example usage:</b>
 *
 * <pre>{@code
 * ObjectMapper mapper = new ObjectMapper();
 * DataTypeCodec codec = new DataTypeCodec(mapper);
 *
 * // Deserialize string literal format (simple types)
 * DataType text = codec.deserialize("\"TEXT\"");
 * DataType intType = codec.deserialize("\"INT\"");
 *
 * // Deserialize object format (with parameters)
 * DataType decimal = codec.deserialize("{\"kind\":\"DECIMAL\",\"precision\":10,\"scale\":2}");
 *
 * // Serialization (always object format)
 * String json = codec.serialize(text); // Produces: {"kind":"TEXT"}
 * }</pre>
 */
@Slf4j
public class DataTypeCodec {

  private final ObjectMapper objectMapper;

  /**
   * Constructs a new DataTypeCodec.
   *
   * @param objectMapper the Jackson ObjectMapper for JSON processing
   */
  public DataTypeCodec(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Deserializes a JSON string to a DataType instance.
   *
   * <p>Supports two JSON formats:
   *
   * <ul>
   *   <li><b>String literal:</b> {@code "TEXT"}, {@code "INT"}, etc. type kinds are
   *       case-insensitive.
   *   <li><b>Object:</b> {@code {"kind":"TEXT"}}, {@code
   *       {"kind":"DECIMAL","precision":10,"scale":2}}
   * </ul>
   *
   * <p>String literals are convenient for simple types. For parameterized types like DECIMAL, use
   * the object format to specify parameters explicitly. String literal "DECIMAL" will use default
   * values (precision=38, scale=0).
   *
   * @param json the JSON string (string literal or object)
   * @return the deserialized DataType instance
   * @throws IllegalArgumentException if the JSON is null or empty or not string literal or object
   * @throws DataTypeCodecException if deserialization fails due to invalid JSON structure, unknown
   *     type, or codec errors
   */
  public DataType deserialize(String json) {
    if (json == null || json.trim().isEmpty()) {
      throw new IllegalArgumentException("JSON cannot be null or empty");
    }

    try {
      logger.debug("Deserializing JSON to DataType");
      JsonNode jsonNode = objectMapper.readTree(json);

      // Case 1: String literal format - "TEXT"
      if (jsonNode.isTextual()) {
        String typeKindStr = jsonNode.asText();
        DataTypeKind typeKind = parseDataTypeKind(typeKindStr);
        return createSimpleDataType(typeKind);
      }

      // Case 2: Object format - {"kind": "TEXT"} or with parameters
      if (jsonNode.isObject()) {
        DataTypeKind typeKind = extractTypeName(jsonNode);
        Class<? extends DataType> dataTypeClass = getDataTypeClassByName(typeKind);

        // Remove the 'kind' field before deserialization since it's an immutable field
        // and Jackson can't be used to set immutable fields
        ((ObjectNode) jsonNode).remove("kind");

        return objectMapper.treeToValue(jsonNode, dataTypeClass);
      }

      throw new IllegalArgumentException(
          "Invalid JSON structure for DataType. Expected string or object.");
    } catch (IllegalArgumentException e) {
      logger.error("Invalid JSON structure for DataType or unknown kind of DataType", e);
      throw e;
    } catch (DataTypeCodecException e) {
      // Let upstream handling keep the original codec exception without double-wrapping.
      throw e;
    } catch (Exception e) {
      logger.error("Failed to deserialize JSON to DataType: {}", json, e);
      throw new DataTypeCodecException(
          "Failed to deserialize JSON to DataType. Please check the JSON format.", e);
    }
  }

  private DataTypeKind extractTypeName(JsonNode jsonNode) {
    if (jsonNode instanceof ObjectNode) {
      JsonNode nameNode = jsonNode.get("kind");
      if (nameNode == null) {
        throw new DataTypeCodecException("Missing 'kind' field in DataType JSON");
      }
      return DataTypeKind.valueOf(nameNode.asText());
    } else {
      throw new DataTypeCodecException("Invalid JSON structure for DataType");
    }
  }

  /**
   * Parses a string to DataTypeKind enum.
   *
   * @param typeKindStr the type kind string (e.g., "TEXT", "INT", case-insensitive)
   * @return the parsed DataTypeKind
   * @throws DataTypeCodecException if the string is not a valid DataTypeKind
   */
  private DataTypeKind parseDataTypeKind(String typeKindStr) {
    try {
      return DataTypeKind.valueOf(typeKindStr.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new DataTypeCodecException("Unknown DataType: " + typeKindStr, e);
    }
  }

  /**
   * Creates a simple DataType instance without additional parameters.
   *
   * <p>This is used when deserializing from string literals like "TEXT" or "INT". For types that
   * require parameters (like DECIMAL), default values are used: precision=38, scale=0.
   *
   * @param kind the data type kind
   * @return the DataType instance
   * @throws DataTypeCodecException if the type cannot be instantiated
   */
  private DataType createSimpleDataType(DataTypeKind kind) {
    switch (kind) {
      case BYTE:
        return DataType.Byte.INSTANCE;
      case SMALLINT:
        return DataType.SmallInt.INSTANCE;
      case INT:
        return DataType.Int.INSTANCE;
      case BIGINT:
        return DataType.BigInt.INSTANCE;
      case FLOAT:
        return DataType.Float.INSTANCE;
      case DOUBLE:
        return DataType.Double.INSTANCE;
      case DECIMAL:
        // Default precision and scale for string literal format
        // Precision 38 and scale 0 are chosen as practical defaults compatible with most databases.
        // (PostgreSQL DECIMAL without precision is unbounded, but many systems use 38 as a
        // maximum.)
        return DataType.Decimal.DEFAULT_INSTANCE;
      case TEXT:
        return DataType.Text.INSTANCE;
      case BLOB:
        return DataType.Blob.INSTANCE;
      case BOOLEAN:
        return DataType.Boolean.INSTANCE;
      case DATE:
        return DataType.Date.INSTANCE;
      case TIME:
        return DataType.Time.INSTANCE;
      case TIMESTAMP:
        return DataType.Timestamp.INSTANCE;
      case TIMESTAMPTZ:
        return DataType.TimestampTZ.INSTANCE;
      case DURATION:
        return DataType.Duration.INSTANCE;
      case INTERVAL:
        return DataType.Interval.INSTANCE;
      default:
        throw new DataTypeCodecException("Unsupported DataType: " + kind);
    }
  }

  private Class<? extends DataType> getDataTypeClassByName(DataTypeKind kind) {
    switch (kind) {
      case BYTE:
        return DataType.Byte.class;
      case SMALLINT:
        return DataType.SmallInt.class;
      case INT:
        return DataType.Int.class;
      case BIGINT:
        return DataType.BigInt.class;
      case FLOAT:
        return DataType.Float.class;
      case DOUBLE:
        return DataType.Double.class;
      case DECIMAL:
        return DataType.Decimal.class;
      case TEXT:
        return DataType.Text.class;
      case BLOB:
        return DataType.Blob.class;
      case BOOLEAN:
        return DataType.Boolean.class;
      case DATE:
        return DataType.Date.class;
      case TIME:
        return DataType.Time.class;
      case TIMESTAMP:
        return DataType.Timestamp.class;
      case TIMESTAMPTZ:
        return DataType.TimestampTZ.class;
      case DURATION:
        return DataType.Duration.class;
      case INTERVAL:
        return DataType.Interval.class;
      default:
        throw new DataTypeCodecException("Unknown DataType: " + kind);
    }
  }

  /**
   * Serializes a DataType instance to a JSON string.
   *
   * <p>The returned JSON will include a {@code kind} field identifying the data type, plus any
   * type-specific fields required by that data type.
   *
   * @param dataType the data type instance to serialize
   * @return JSON string representation
   * @throws IllegalArgumentException if dataType is null
   * @throws DataTypeCodecException if serialization fails due to codec errors
   */
  public String serialize(DataType dataType) {
    if (dataType == null) {
      throw new IllegalArgumentException("DataType cannot be null");
    }

    try {
      logger.debug("Serializing DataType: {}", dataType.getKind());

      JsonNode jsonNode = objectMapper.valueToTree(dataType);
      return objectMapper.writeValueAsString(jsonNode);
    } catch (Exception e) {
      logger.error("Failed to serialize DataType: {}", dataType.getKind(), e);
      throw new DataTypeCodecException(
          String.format("Failed to serialize %s to JSON", dataType.getKind()), e);
    }
  }
}
