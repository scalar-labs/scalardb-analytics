/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import lombok.extern.slf4j.Slf4j;

/**
 * Base codec implementation that leverages @Jacksonized annotation on provider models for automatic
 * JSON serialization/deserialization.
 *
 * @param <T> the specific DataSourceProvider type
 */
@Slf4j
public abstract class JacksonizedProviderCodec<T extends DataSourceProvider>
    implements ProviderCodec<T> {

  private final ObjectMapper objectMapper;
  private final Class<T> providerClass;
  private final String providerType;

  protected JacksonizedProviderCodec(
      Class<T> providerClass, String providerType, ObjectMapper objectMapper) {
    this.providerClass = providerClass;
    this.providerType = providerType;
    this.objectMapper = objectMapper;
  }

  @Override
  public T deserialize(JsonNode json) {
    ObjectNode payload = extractAndValidatePayload(json);
    try {
      return objectMapper.treeToValue(payload, providerClass);
    } catch (IllegalArgumentException e) {
      throw e;
    } catch (Exception e) {
      logger.error(
          "Failed to deserialize JSON for provider {}: {}", providerClass.getSimpleName(), json, e);
      throw new ProviderCodecException(
          String.format(
              "Invalid JSON for %s provider. Please check the JSON structure matches the expected schema.",
              providerClass.getSimpleName()),
          e);
    }
  }

  @Override
  public JsonNode serialize(T provider) {
    try {
      JsonNode serialized = objectMapper.valueToTree(provider);
      validateSerializedNode(serialized);
      return serialized;
    } catch (Exception e) {
      logger.error("Failed to serialize {} provider", provider.getClass().getSimpleName(), e);
      throw new ProviderCodecException(
          String.format(
              "Failed to serialize %s provider to JSON", provider.getClass().getSimpleName()),
          e);
    }
  }

  private ObjectNode extractAndValidatePayload(JsonNode json) {
    if (!(json instanceof ObjectNode)) {
      throw new IllegalArgumentException(
          String.format(
              "Provider JSON must be an object with a 'type' field but payload was %s", json));
    }

    ObjectNode envelope = ((ObjectNode) json).deepCopy();
    JsonNode typeNode = envelope.get("type");
    if (typeNode == null || !typeNode.isTextual()) {
      throw new IllegalArgumentException(
          String.format(
              "Provider JSON must include a textual 'type' field but payload was %s", json));
    }

    String actualType = typeNode.asText();
    if (!providerType.equals(actualType)) {
      throw new IllegalArgumentException(
          String.format(
              "Provider JSON type mismatch: expected '%s' but was '%s'", providerType, actualType));
    }

    envelope.remove("type");
    return envelope;
  }

  private void validateSerializedNode(JsonNode serialized) {
    if (!(serialized instanceof ObjectNode)) {
      throw new ProviderCodecException(
          String.format(
              "Provider %s produced non-object JSON payload", providerClass.getSimpleName()));
    }

    ObjectNode objectNode = (ObjectNode) serialized;
    JsonNode typeNode = objectNode.get("type");
    if (typeNode == null || !typeNode.isTextual()) {
      throw new ProviderCodecException(
          String.format(
              "Provider %s produced JSON without a textual 'type' field",
              providerClass.getSimpleName()));
    }

    if (!providerType.equals(typeNode.asText())) {
      throw new ProviderCodecException(
          String.format(
              "Provider type mismatch: codec handles '%s' but JSON reported '%s'",
              providerType, typeNode.asText()));
    }
  }
}
