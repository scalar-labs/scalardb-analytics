/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service;

import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.service.datasource.NoOpSchemaResolver;
import com.scalar.db.analytics.service.datasource.ResolvedSchema;
import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * A {@link SchemaResolution} that uses the schema supplied with the request, for tests of the code
 * that consumes the resolved schema.
 */
public final class SuppliedSchemaResolution implements SchemaResolution {

  @Override
  public ResolvedSchema resolve(
      UUID dataSourceId, DataSourceProvider provider, @Nullable DataSourceSchema schema)
      throws SchemaResolverException, SuppliedSchemaRejectedException {
    if (schema == null) {
      throw new SuppliedSchemaRejectedException(
          SuppliedSchemaRejectedException.Reason.REQUIRED_FOR_PROVIDER,
          "A schema must be supplied");
    }
    return new NoOpSchemaResolver(dataSourceId, schema).resolveSchema();
  }
}
