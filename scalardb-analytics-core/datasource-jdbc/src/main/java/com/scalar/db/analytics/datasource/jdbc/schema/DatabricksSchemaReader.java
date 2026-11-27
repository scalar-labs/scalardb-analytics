/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.datasource.jdbc.schema;

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks;
import java.util.Properties;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * This class provides the schema reader for Databricks data source.
 *
 * <p>This resolves all the catalogs and schemas in Databricks.
 *
 * <p>This also ignores the system "system" catalog and system "information_schema" schema of
 * Databricks.
 */
public class DatabricksSchemaReader extends JdbcSchemaReader {
  private DatabricksSchemaReader(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      JdbcSchemaReaderOption option,
      @Nullable String onlyCatalogToResolve) {
    super(
        dataSourceId,
        connectionUrl,
        connectionProperties,
        option,
        new DatabricksSchemaReaderStrategy(onlyCatalogToResolve));
  }

  /**
   * Constructs a schema reader for a Databricks source. This also opens a JDBC connection to the
   * data source. It is user's responsibility to close the connection after using the schema reader
   * by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param databricks the Databricks data source information
   * @param option the option for the schema reader
   * @param onlyCatalogToResolve the catalog name to resolve. If null, all catalogs are resolved.
   */
  public static DatabricksSchemaReader open(
      UUID dataSourceId,
      Databricks databricks,
      JdbcSchemaReaderOption option,
      @Nullable String onlyCatalogToResolve) {
    return open(
        dataSourceId,
        databricks.getUrl(),
        databricks.getProperties(),
        option,
        onlyCatalogToResolve);
  }

  /**
   * Constructs a schema reader for a Databricks source. This also opens a JDBC connection to the
   * data source. It is user's responsibility to close the connection after using the schema reader
   * by calling the {@code close()} method.
   *
   * @param dataSourceId the ID of the data source
   * @param connectionUrl the JDBC connection URL
   * @param connectionProperties the JDBC connection properties
   * @param option the option for the schema reader
   * @param onlyCatalogToResolve the catalog name to resolve. If null, all catalogs are resolved.
   */
  public static DatabricksSchemaReader open(
      UUID dataSourceId,
      String connectionUrl,
      Properties connectionProperties,
      JdbcSchemaReaderOption option,
      @Nullable String onlyCatalogToResolve) {
    DatabricksSchemaReader reader =
        new DatabricksSchemaReader(
            dataSourceId, connectionUrl, connectionProperties, option, onlyCatalogToResolve);
    reader.openConnection();
    return reader;
  }
}
