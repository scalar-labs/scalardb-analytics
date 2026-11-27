/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.datasource.jdbc.schema;

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.SqlServer;
import java.util.Properties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * This class provides the schema reader for an SQL Server data source.
 *
 * <p>This resolves all the databases and schemas in the SQL Server.
 *
 * <p>This ignores the default databases of SQL Server, including 'master', 'model', 'msdb', and
 * 'tempdb'.
 *
 * <p>This also ignores the default schemas of SQL Server, including 'sys', 'guest',
 * 'INFORMATION_SCHEMA', and the legacy default schemas.
 */
public class SqlServerSchemaReader extends JdbcSchemaReader {
  private SqlServerSchemaReader(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      JdbcSchemaReaderOption option,
      @Nullable String onlyDatabaseToResolve) {
    super(
        dataSourceId,
        connectionUrl,
        connectionProperties,
        option,
        new SqlServerSchemaReaderStrategy(onlyDatabaseToResolve));
  }

  /**
   * Constructs a schema reader for an SQL Server data source. This also opens a JDBC connection to
   * the data source. It is user's responsibility to close the connection after using the schema
   * reader by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param sqlServer the SQL Server data source information
   * @param option the option for the schema reader
   * @param onlyDatabaseToResolve the database name to resolve. If null, all databases are resolved.
   */
  public static SqlServerSchemaReader open(
      UUID dataSourceId,
      SqlServer sqlServer,
      JdbcSchemaReaderOption option,
      @Nullable String onlyDatabaseToResolve) {
    return open(
        dataSourceId, sqlServer.getUrl(), sqlServer.getProperties(), option, onlyDatabaseToResolve);
  }

  /**
   * Constructs a schema reader for an SQL Server data source. This also opens a JDBC connection to
   * the data source. It is user's responsibility to close the connection after using the schema
   * reader by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param connectionUrl the JDBC connection URL
   * @param connectionProperties the JDBC connection properties
   * @param option the option for the schema reader
   * @param onlyDatabaseToResolve the database name to resolve. If null, all databases are resolved.
   */
  public static SqlServerSchemaReader open(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      JdbcSchemaReaderOption option,
      @Nullable String onlyDatabaseToResolve) {
    SqlServerSchemaReader reader =
        new SqlServerSchemaReader(
            dataSourceId, connectionUrl, connectionProperties, option, onlyDatabaseToResolve);
    reader.openConnection();
    return reader;
  }
}
