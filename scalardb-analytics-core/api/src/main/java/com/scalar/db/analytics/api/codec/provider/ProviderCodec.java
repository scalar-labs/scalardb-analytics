/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;

/**
 * Codec for converting between {@link DataSourceProvider} implementations and their JSON envelope.
 *
 * <p>All implementations MUST operate on the same envelope shape: a JSON object that contains one
 * textual {@code type} field naming the provider (matching {@link DataSourceProvider#getType()})
 * alongside provider-specific fields. For example:
 *
 * <pre>{@code
 * {
 *   "type": "postgresql",
 *   "host": "db.example.com",
 *   "port": 5432,
 *   "username": "analytics",
 *   "password": "secret"
 * }
 * }</pre>
 *
 * @param <T> the specific DataSourceProvider type
 */
public interface ProviderCodec<T extends DataSourceProvider> {

  /**
   * Deserializes a JSON node to the specific provider type.
   *
   * <p><strong>Contract:</strong> Callers provide a JSON envelope as described in the class-level
   * documentation. Implementations MUST defensively validate that {@code json} is an object, that
   * it contains a textual {@code type} field, and that the value matches the provider type handled
   * by this codec before attempting to read provider-specific fields.
   *
   * @param json the JSON node containing provider configuration
   * @return the deserialized provider instance
   * @throws IllegalArgumentException if the JSON does not conform to the envelope described in the
   *     class-level documentation, if the {@code type} field names a different provider, or if
   *     provider-specific validation fails because the configuration is invalid
   * @throws ProviderCodecException if deserialization fails for reasons unrelated to input
   *     validation (for example, a required external dependency could not be initialized)
   */
  T deserialize(JsonNode json);

  /**
   * Serializes the provider to a JSON node.
   *
   * <p><strong>Contract:</strong> implementations MUST return a JSON object that follows the shared
   * envelope (a textual {@code type} equal to {@code provider.getType()} plus provider-specific
   * fields). Supplying the returned JSON to {@link #deserialize(JsonNode)} MUST reconstruct a
   * provider that is equal to the input instance (per {@code equals(Object)}).
   *
   * @param provider the provider instance to serialize
   * @return the JSON representation
   * @throws ProviderCodecException if serialization fails for reasons unrelated to input validation
   *     (for example, interacting with external dependencies)
   */
  JsonNode serialize(T provider);
}
