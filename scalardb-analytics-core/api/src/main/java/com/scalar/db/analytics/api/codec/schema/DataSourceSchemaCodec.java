/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import org.jspecify.annotations.Nullable;

/**
 * Codec for converting between {@link DataSourceSchema} and its JSON representation.
 *
 * <p>The application treats {@link DataSourceSchema} as nullable; a {@code null} schema means “the
 * provider does not require or provide a manual schema.” To remain consistent with that contract,
 * this codec accepts and produces {@code null} values. The mapping rules are:
 *
 * <ul>
 *   <li>{@link #serialize(DataSourceSchema)} returns {@code null} when given {@code null}.
 *   <li>{@link #deserialize(String)} returns {@code null} when given {@code null}, and requires
 *       non-blank JSON strings otherwise.
 *   <li>{@link #deserialize(JsonNode)} returns {@code null} when the node is {@code null}. A JSON
 *       literal {@code null} is rejected so that absence is always represented by a Java {@code
 *       null}, keeping the caller’s intent unambiguous.
 * </ul>
 *
 * <p>Any other malformed payload (empty string, JSON {@code null}, structural errors) results in an
 * {@link IllegalArgumentException} to signal invalid input early.
 */
public class DataSourceSchemaCodec {

  private final ObjectMapper objectMapper;

  public DataSourceSchemaCodec(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /**
   * Deserializes a JSON payload into a {@link DataSourceSchema}.
   *
   * <p><strong>Contract:</strong> callers may pass {@code null} to indicate that no schema is
   * present. Non-null inputs must contain a non-blank JSON document; otherwise, the payload is
   * treated as malformed. This keeps the codec idempotent: {@code serialize(null)} returns {@code
   * null}, and feeding that value back to {@code deserialize(String)} yields {@code null}.
   *
   * @param json schema payload encoded as JSON
   * @return deserialized {@link DataSourceSchema}, or {@code null} when {@code json} is {@code
   *     null}
   * @throws IllegalArgumentException if {@code json} is blank or cannot be parsed
   */
  public @Nullable DataSourceSchema deserialize(@Nullable String json) {
    if (json == null) {
      return null;
    }
    if (json.trim().isEmpty()) {
      throw new IllegalArgumentException("Schema JSON cannot be empty");
    }
    try {
      JsonNode node = objectMapper.readTree(json);
      return deserialize(node);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid data source schema JSON", e);
    }
  }

  /**
   * Deserializes a JSON node into a {@link DataSourceSchema}.
   *
   * <p>The caller may pass {@code null} to indicate that no schema is present. A JSON literal
   * {@code null} (i.e., {@code node.isNull() == true}) is considered invalid input because it most
   * likely stems from a serialization mistake; such payloads are rejected so that absence is
   * consistently represented by an actual {@code null} reference rather than JSON {@code null}.
   *
   * @param node schema payload encoded as a Jackson {@link JsonNode}
   * @return deserialized {@link DataSourceSchema}, or {@code null} when {@code node} is {@code
   *     null}
   * @throws IllegalArgumentException if {@code node} represents JSON {@code null} or cannot be
   *     converted into a {@link DataSourceSchema}
   */
  public @Nullable DataSourceSchema deserialize(@Nullable JsonNode node) {
    if (node == null) {
      return null;
    }
    if (node.isNull()) {
      // Jackson would return null for JSON literal null (treeToValue(nullNode, X) -> null).
      // Treat this as malformed input so that absence is always signalled via Java null rather
      // than JSON null, keeping callers' intent unambiguous.
      throw new IllegalArgumentException("Schema JSON cannot be JSON null");
    }
    try {
      return objectMapper.treeToValue(node, DataSourceSchema.class);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid data source schema JSON", e);
    }
  }

  /**
   * Serializes a {@link DataSourceSchema} to JSON.
   *
   * <p>Passing {@code null} is allowed and treated as “no schema,” returning {@code null} so that
   * {@link #deserialize(String)} can round-trip the absence of a schema. Non-null schema instances
   * are serialized to a JSON document.
   *
   * @param schema schema instance to serialize
   * @return JSON string representing the schema, or {@code null} when {@code schema} is {@code
   *     null}
   * @throws IllegalArgumentException if serialization fails
   */
  public @Nullable String serialize(@Nullable DataSourceSchema schema) {
    if (schema == null) {
      return null;
    }
    try {
      return objectMapper.writeValueAsString(schema);
    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to serialize data source schema", e);
    }
  }
}
