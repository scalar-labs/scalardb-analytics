/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake;
import java.util.Properties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * This class provides the schema resolver for Snowflake data source.
 *
 * <p>This resolves all the databases and schemas in Snowflake.
 *
 * <p>This ignores the system database "SNOWFLAKE" and the system schema "INFORMATION_SCHEMA".
 */
public class SchemaResolverSnowflake extends RdbmsSchemaResolver {
  private SchemaResolverSnowflake(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      RdbmsSchemaResolverOption option,
      @Nullable String onlyDatabaseToResolve) {
    super(
        dataSourceId,
        connectionUrl,
        connectionProperties,
        option,
        new SchemaResolverStrategySnowflake(onlyDatabaseToResolve));
  }

  /**
   * Constructs a schema resolver for a Snowflake source. This also opens a JDBC connection to the
   * data source. It is user's responsibility to close the connection after using the schema
   * resolver by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param snowflake the Snowflake data source information
   * @param option the option for the schema resolver
   * @param onlyDatabaseToResolve the database name to resolve. If null, all databases are resolved.
   */
  public static SchemaResolverSnowflake open(
      UUID dataSourceId,
      Snowflake snowflake,
      RdbmsSchemaResolverOption option,
      @Nullable String onlyDatabaseToResolve) {
    return open(
        dataSourceId, snowflake.getUrl(), snowflake.getProperties(), option, onlyDatabaseToResolve);
  }

  /**
   * Constructs a schema resolver for a Snowflake source. This also opens a JDBC connection to the
   * data source. It is user's responsibility to close the connection after using the schema
   * resolver by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param connectionUrl the JDBC connection URL
   * @param connectionProperties the JDBC connection properties
   * @param option the option for the schema resolver
   * @param onlyDatabaseToResolve the database name to resolve. If null, all databases are resolved.
   */
  public static SchemaResolverSnowflake open(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      RdbmsSchemaResolverOption option,
      @Nullable String onlyDatabaseToResolve) {
    SchemaResolverSnowflake resolver =
        new SchemaResolverSnowflake(
            dataSourceId, connectionUrl, connectionProperties, option, onlyDatabaseToResolve);
    resolver.openConnection();
    return resolver;
  }
}
