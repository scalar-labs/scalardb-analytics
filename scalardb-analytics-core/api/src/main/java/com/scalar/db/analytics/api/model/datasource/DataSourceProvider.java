/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model.datasource;

/**
 * Marker interface for all data source providers.
 *
 * <p>Implementations must expose their provider type as a lowercase string so that codecs and
 * mappers can embed the type alongside provider-specific fields while serializing to JSON.
 */
public interface DataSourceProvider {

  /**
   * Returns the canonical provider type identifier (e.g. {@code "postgresql"}).
   *
   * @return provider type identifier
   */
  String getType();

  /**
   * Indicates whether the provider supports automatic schema discovery via a resolver.
   *
   * @return {@code true} if the schema can be resolved automatically, {@code false} if a schema
   *     must be supplied manually.
   */
  default boolean supportsSchemaResolution() {
    return true;
  }

  <T> T accept(DataSourceProviderVisitor<T> visitor);
}
