/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource;

/**
 * This interface is used to resolve the schema of a data source. Each data source has its own way
 * of resolving the schema, and this interface provides a way to resolve the schema of a data
 * source.
 */
public interface SchemaResolver {
  /**
   * Resolves the schema of a data source.
   *
   * @return the resolved schema
   * @throws SchemaResolverException if an error occurs while resolving the schema
   */
  ResolvedSchema resolveSchema() throws SchemaResolverException;
}
