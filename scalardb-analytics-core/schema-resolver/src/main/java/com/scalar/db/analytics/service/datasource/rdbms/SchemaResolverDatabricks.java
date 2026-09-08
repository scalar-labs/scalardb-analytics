/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks;
import java.util.Properties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * This class provides the schema resolver for Databricks data source.
 *
 * <p>This resolves all the catalogs and schemas in Databricks.
 *
 * <p>This also ignores the system "system" catalog and system "information_schema" schema of
 * Databricks.
 */
public class SchemaResolverDatabricks extends RdbmsSchemaResolver {
  private SchemaResolverDatabricks(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      RdbmsSchemaResolverOption option,
      @Nullable String onlyCatalogToResolve) {
    super(
        dataSourceId,
        connectionUrl,
        connectionProperties,
        option,
        new SchemaResolverStrategyDatabricks(onlyCatalogToResolve));
  }

  /**
   * Constructs a schema resolver for a Databricks source. This also opens a JDBC connection to the
   * data source. It is user's responsibility to close the connection after using the schema
   * resolver by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param databricks the Databricks data source information
   * @param option the option for the schema resolver
   * @param onlyCatalogToResolve the catalog name to resolve. If null, all catalogs are resolved.
   */
  public static SchemaResolverDatabricks open(
      UUID dataSourceId,
      Databricks databricks,
      RdbmsSchemaResolverOption option,
      @Nullable String onlyCatalogToResolve) {
    return open(
        dataSourceId,
        databricks.getUrl(),
        databricks.getProperties(),
        option,
        onlyCatalogToResolve);
  }

  /**
   * Constructs a schema resolver for a Databricks source. This also opens a JDBC connection to the
   * data source. It is user's responsibility to close the connection after using the schema
   * resolver by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param connectionUrl the JDBC connection URL
   * @param connectionProperties the JDBC connection properties
   * @param option the option for the schema resolver
   * @param onlyCatalogToResolve the catalog name to resolve. If null, all catalogs are resolved.
   */
  public static SchemaResolverDatabricks open(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      RdbmsSchemaResolverOption option,
      @Nullable String onlyCatalogToResolve) {
    SchemaResolverDatabricks resolver =
        new SchemaResolverDatabricks(
            dataSourceId, connectionUrl, connectionProperties, option, onlyCatalogToResolve);
    resolver.openConnection();
    return resolver;
  }
}
