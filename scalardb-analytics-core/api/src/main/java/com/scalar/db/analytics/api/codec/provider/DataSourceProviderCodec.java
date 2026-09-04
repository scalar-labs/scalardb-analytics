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
 * Codec for converting between DataSourceProvider objects and JSON representation. This provides a
 * single entry point for JSON serialization/deserialization of providers, using the
 * ProviderCodecRegistry to get the appropriate codec for each provider type.
 */
@Slf4j
public class DataSourceProviderCodec {

  private final ProviderCodecRegistry registry;
  private final ObjectMapper objectMapper;

  public DataSourceProviderCodec(ProviderCodecRegistry registry, ObjectMapper objectMapper) {
    this.registry = registry;
    this.objectMapper = objectMapper;
  }

  /**
   * Serializes a DataSourceProvider to JSON string.
   *
   * @param provider the provider to serialize
   * @return JSON string representation
   */
  public String serialize(DataSourceProvider provider) {
    logger.debug("Serializing provider of type: {}", provider.getType());

    @SuppressWarnings("unchecked")
    ProviderCodec<DataSourceProvider> codec;
    try {
      codec = (ProviderCodec<DataSourceProvider>) registry.getCodecForClass(provider.getClass());
    } catch (IllegalArgumentException e) {
      logger.error(
          "No codec registered for provider class {} (instance {})",
          provider.getClass().getName(),
          provider,
          e);
      throw e;
    }

    JsonNode serialized = codec.serialize(provider);
    ObjectNode envelope = validateSerializedEnvelope(provider, serialized);
    try {
      return objectMapper.writeValueAsString(envelope);
    } catch (Exception e) {
      logger.error("Failed to serialize provider instance {}", provider, e);
      throw new ProviderCodecException(
          String.format("Failed to serialize provider %s to JSON", provider), e);
    }
  }

  /**
   * Deserializes JSON string to a DataSourceProvider.
   *
   * @param json the JSON string
   * @return the deserialized DataSourceProvider
   */
  public DataSourceProvider deserialize(String json) {
    if (json.trim().isEmpty()) {
      throw new IllegalArgumentException("JSON cannot be null or empty");
    }

    logger.debug("Deserializing provider JSON");
    JsonNode jsonNode;
    try {
      jsonNode = objectMapper.readTree(json);
    } catch (Exception e) {
      logger.error("Failed to deserialize provider JSON: {}", json, e);
      throw new IllegalArgumentException("Input JSON is malformed and cannot be parsed.", e);
    }
    return deserialize(jsonNode);
  }

  /**
   * Deserializes JsonNode to a DataSourceProvider.
   *
   * @param jsonNode the JSON node
   * @return the deserialized DataSourceProvider
   */
  public DataSourceProvider deserialize(JsonNode jsonNode) {
    ObjectNode envelope = validateIncomingEnvelope(jsonNode);
    String providerType = envelope.get("type").asText();
    ProviderCodec<?> codec;
    try {
      codec = registry.getCodecForType(providerType);
    } catch (IllegalArgumentException e) {
      logger.error(
          "No codec registered for provider type {} while decoding payload {}",
          providerType,
          jsonNode,
          e);
      throw e;
    }

    try {
      return codec.deserialize(envelope);
    } catch (IllegalArgumentException e) {
      logger.error(
          "Provider JSON is invalid for registered type {}. Payload: {}",
          providerType,
          jsonNode,
          e);
      throw e;
    } catch (ProviderCodecException e) {
      // Let upstream handling keep the original codec exception without double-wrapping.
      throw e;
    } catch (Exception e) {
      logger.error(
          "Failed to deserialize provider JSON for type {}: {}", providerType, jsonNode, e);
      throw new ProviderCodecException(
          String.format("Failed to deserialize JSON to %s provider", providerType), e);
    }
  }

  private ObjectNode validateSerializedEnvelope(DataSourceProvider provider, JsonNode node) {
    if (!(node instanceof ObjectNode)) {
      throw new ProviderCodecException(
          String.format(
              "Provider %s produced non-object JSON payload: %s",
              provider.getClass().getSimpleName(), node));
    }
    ObjectNode objectNode = (ObjectNode) node;
    JsonNode typeNode = objectNode.get("type");
    if (typeNode == null || !typeNode.isTextual()) {
      throw new ProviderCodecException(
          String.format(
              "Provider %s JSON must include a textual 'type' field: %s",
              provider.getClass().getSimpleName(), objectNode));
    }
    String type = typeNode.asText();
    if (!provider.getType().equals(type)) {
      throw new ProviderCodecException(
          String.format(
              "Provider %s produced JSON with mismatched type '%s'",
              provider.getClass().getSimpleName(), type));
    }
    return objectNode;
  }

  private ObjectNode validateIncomingEnvelope(JsonNode node) {
    if (!(node instanceof ObjectNode)) {
      throw new IllegalArgumentException(
          String.format("Provider JSON must be an object but payload was %s", node));
    }
    ObjectNode objectNode = (ObjectNode) node;
    JsonNode typeNode = objectNode.get("type");
    if (typeNode == null || !typeNode.isTextual()) {
      throw new IllegalArgumentException(
          String.format(
              "Provider JSON must include a textual 'type' field but payload was %s", objectNode));
    }
    return objectNode;
  }
}
