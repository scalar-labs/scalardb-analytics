/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import java.util.Properties;
import java.util.UUID;

/**
 * This class provides the schema resolver for a PostgreSQL data source.
 *
 * <p>Resolves all the schemas in the specified database.
 *
 * <p>This ignores the default schemas of PostgreSQL, including 'information_schema' and
 * 'pg_catalog'.
 */
public class SchemaResolverPostgreSql extends RdbmsSchemaResolver {
  private SchemaResolverPostgreSql(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      RdbmsSchemaResolverOption option) {
    super(
        dataSourceId,
        connectionUrl,
        connectionProperties,
        option,
        new SchemaResolverStrategyPostgreSql());
  }

  /**
   * Constructs a schema resolver for a PostgreSQL data source. This also opens a JDBC connection to
   * the data source. It is user's responsibility to close the connection after using the schema
   * resolver by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param postgreSql the PostgreSQL data source information
   * @param option the option for the schema resolver
   */
  public static SchemaResolverPostgreSql open(
      UUID dataSourceId, PostgreSql postgreSql, RdbmsSchemaResolverOption option) {
    return open(dataSourceId, postgreSql.getUrl(), postgreSql.getProperties(), option);
  }

  /**
   * Constructs a schema resolver for a PostgreSQL data source. This also opens a JDBC connection to
   * the data source. It is user's responsibility to close the connection after using the schema
   * resolver by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param connectionUrl the JDBC connection URL
   * @param connectionProperties the JDBC connection properties
   * @param option the option for the schema resolver
   */
  public static SchemaResolverPostgreSql open(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      RdbmsSchemaResolverOption option) {
    SchemaResolverPostgreSql resolver =
        new SchemaResolverPostgreSql(dataSourceId, connectionUrl, connectionProperties, option);
    resolver.openConnection();
    return resolver;
  }
}
