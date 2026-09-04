/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql;
import java.util.Properties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * This class provides the schema resolver for a MySQL data source.
 *
 * <p>This resolves all the databases in the MySQL server.
 *
 * <p>This ignores the default databases of MySQL, including 'mysql', 'sys', 'information_schema',
 * and 'performance_schema'.
 */
public class SchemaResolverMySql extends RdbmsSchemaResolver {
  private SchemaResolverMySql(
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
        new SchemaResolverStrategyMysql(onlyDatabaseToResolve));
  }

  /**
   * Constructs a schema resolver for a MySQL data source. This also opens a JDBC connection to the
   * data source. It is user's responsibility to close the connection after using the schema
   * resolver by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param mySql the MySQL data source information
   * @param option the option for the schema resolver
   * @param onlyDatabaseToResolve the database name to resolve. If null, all databases are resolved.
   */
  public static SchemaResolverMySql open(
      UUID dataSourceId,
      MySql mySql,
      RdbmsSchemaResolverOption option,
      @Nullable String onlyDatabaseToResolve) {
    return open(dataSourceId, mySql.getUrl(), mySql.getProperties(), option, onlyDatabaseToResolve);
  }

  /**
   * Constructs a schema resolver for a MySQL data source. This also opens a JDBC connection to the
   *
   * @param dataSourceId the ID of the data source
   * @param connectionUrl the JDBC connection URL
   * @param connectionProperties the JDBC connection properties
   * @param option the option for the schema resolver
   * @param onlyDatabaseToResolve the database name to resolve. If null, all databases are resolved.
   * @return
   */
  public static SchemaResolverMySql open(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      RdbmsSchemaResolverOption option,
      @Nullable String onlyDatabaseToResolve) {
    SchemaResolverMySql resolver =
        new SchemaResolverMySql(
            dataSourceId, connectionUrl, connectionProperties, option, onlyDatabaseToResolve);
    resolver.openConnection();
    return resolver;
  }
}
