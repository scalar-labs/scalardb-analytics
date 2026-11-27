/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service;

import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.service.datasource.InvalidDataSourceConfigurationException;
import com.scalar.db.analytics.service.datasource.ResolvedSchema;
import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * Resolves the schema of a data source that is being registered.
 *
 * <p>How a schema is resolved is a choice of the environment the catalog runs in, not of the
 * provider alone. The server connects to the source itself, while an environment that embeds the
 * catalog can resolve the schema from metadata it already has. An implementation therefore also
 * decides whether it accepts a schema supplied with the registration request.
 *
 * <p>An implementation reports only why it cannot resolve a schema; {@link DataSourceService} turns
 * that into the error returned to the caller and validates the returned schema before it persists
 * anything.
 */
@FunctionalInterface
public interface SchemaResolution {

  /**
   * Resolves the schema of a data source.
   *
   * @param dataSourceId the ID of the data source being registered
   * @param provider the provider of the data source
   * @param schema the schema supplied with the registration request, or {@code null} if none was
   *     supplied
   * @return the namespaces and tables of the data source
   * @throws SuppliedSchemaRejectedException if this implementation requires a supplied schema for
   *     the provider and none was given, or does not accept one and one was given
   * @throws InvalidDataSourceConfigurationException if the data source's client library rejects the
   *     provider's configuration
   * @throws SchemaResolverException if the schema cannot be resolved for another reason
   */
  ResolvedSchema resolve(
      UUID dataSourceId, DataSourceProvider provider, @Nullable DataSourceSchema schema)
      throws SchemaResolverException, SuppliedSchemaRejectedException;
}
