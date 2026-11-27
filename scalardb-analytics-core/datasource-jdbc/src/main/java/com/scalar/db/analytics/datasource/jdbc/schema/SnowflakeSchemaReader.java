/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.datasource.jdbc.schema;

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake;
import java.util.Properties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * This class provides the schema reader for Snowflake data source.
 *
 * <p>This resolves all the databases and schemas in Snowflake.
 *
 * <p>This ignores the system database "SNOWFLAKE" and the system schema "INFORMATION_SCHEMA".
 */
public class SnowflakeSchemaReader extends JdbcSchemaReader {
  private SnowflakeSchemaReader(
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
        new SnowflakeSchemaReaderStrategy(onlyDatabaseToResolve));
  }

  /**
   * Constructs a schema reader for a Snowflake source. This also opens a JDBC connection to the
   * data source. It is user's responsibility to close the connection after using the schema reader
   * by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param snowflake the Snowflake data source information
   * @param option the option for the schema reader
   * @param onlyDatabaseToResolve the database name to resolve. If null, all databases are resolved.
   */
  public static SnowflakeSchemaReader open(
      UUID dataSourceId,
      Snowflake snowflake,
      JdbcSchemaReaderOption option,
      @Nullable String onlyDatabaseToResolve) {
    return open(
        dataSourceId, snowflake.getUrl(), snowflake.getProperties(), option, onlyDatabaseToResolve);
  }

  /**
   * Constructs a schema reader for a Snowflake source. This also opens a JDBC connection to the
   * data source. It is user's responsibility to close the connection after using the schema reader
   * by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param connectionUrl the JDBC connection URL
   * @param connectionProperties the JDBC connection properties
   * @param option the option for the schema reader
   * @param onlyDatabaseToResolve the database name to resolve. If null, all databases are resolved.
   */
  public static SnowflakeSchemaReader open(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      JdbcSchemaReaderOption option,
      @Nullable String onlyDatabaseToResolve) {
    SnowflakeSchemaReader reader =
        new SnowflakeSchemaReader(
            dataSourceId, connectionUrl, connectionProperties, option, onlyDatabaseToResolve);
    reader.openConnection();
    return reader;
  }
}
