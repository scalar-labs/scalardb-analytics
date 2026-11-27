/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.datasource.jdbc.schema;

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql;
import java.util.Properties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * This class provides the schema reader for a MySQL data source.
 *
 * <p>This resolves all the databases in the MySQL server.
 *
 * <p>This ignores the default databases of MySQL, including 'mysql', 'sys', 'information_schema',
 * and 'performance_schema'.
 */
public class MySqlSchemaReader extends JdbcSchemaReader {
  private MySqlSchemaReader(
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
        new MySqlSchemaReaderStrategy(onlyDatabaseToResolve));
  }

  /**
   * Constructs a schema reader for a MySQL data source. This also opens a JDBC connection to the
   * data source. It is user's responsibility to close the connection after using the schema reader
   * by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param mySql the MySQL data source information
   * @param option the option for the schema reader
   * @param onlyDatabaseToResolve the database name to resolve. If null, all databases are resolved.
   */
  public static MySqlSchemaReader open(
      UUID dataSourceId,
      MySql mySql,
      JdbcSchemaReaderOption option,
      @Nullable String onlyDatabaseToResolve) {
    return open(dataSourceId, mySql.getUrl(), mySql.getProperties(), option, onlyDatabaseToResolve);
  }

  /**
   * Constructs a schema reader for a MySQL data source. This also opens a JDBC connection to the
   *
   * @param dataSourceId the ID of the data source
   * @param connectionUrl the JDBC connection URL
   * @param connectionProperties the JDBC connection properties
   * @param option the option for the schema reader
   * @param onlyDatabaseToResolve the database name to resolve. If null, all databases are resolved.
   * @return
   */
  public static MySqlSchemaReader open(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      JdbcSchemaReaderOption option,
      @Nullable String onlyDatabaseToResolve) {
    MySqlSchemaReader reader =
        new MySqlSchemaReader(
            dataSourceId, connectionUrl, connectionProperties, option, onlyDatabaseToResolve);
    reader.openConnection();
    return reader;
  }
}
