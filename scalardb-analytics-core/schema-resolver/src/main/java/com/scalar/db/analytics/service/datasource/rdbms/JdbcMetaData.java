/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;

/** This is a wrapper class for {@link DatabaseMetaData} to provide a more readable interface. */
class JdbcMetaData {
  private static final String TABLE_TYPE_TABLE = "TABLE";

  private static final String CATALOGS_TABLE_CAT = "TABLE_CAT";
  private static final String SCHEMAS_TABLE_SCHEM = "TABLE_SCHEM";
  private static final String SCHEMAS_TABLE_CATALOG = "TABLE_CATALOG";
  private static final String TABLES_TABLE_NAME = "TABLE_NAME";
  private static final String COLUMNS_COLUMN_NAME = "COLUMN_NAME";
  private static final String COLUMNS_DATA_TYPE = "DATA_TYPE";
  private static final String COLUMNS_TYPE_NAME = "TYPE_NAME";
  private static final String COLUMNS_COLUMN_SIZE = "COLUMN_SIZE";
  private static final String COLUMNS_DECIMAL_DIGITS = "DECIMAL_DIGITS";
  private static final String COLUMNS_NULLABLE = "NULLABLE";

  private final DatabaseMetaData metaData;

  JdbcMetaData(DatabaseMetaData metaData) {
    this.metaData = metaData;
  }

  static JdbcMetaData fromConnection(Connection connection) throws JdbcException {
    try {
      return new JdbcMetaData(connection.getMetaData());
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  ResultSet getCatalogs() throws JdbcException {
    try {
      return metaData.getCatalogs();
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  ResultSet getSchemas() throws JdbcException {
    try {
      return metaData.getSchemas();
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  ResultSet getSchemasForCatalog(String catalog) throws JdbcException {
    try {
      return metaData.getSchemas(catalog, null);
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  /**
   * Returns the schemas that do not belong to a specific catalog. Per the JDBC specification, an
   * empty string for the catalog retrieves the schemas without a catalog. For SQL Server, this
   * corresponds to the built-in schemas shipped with every database (e.g. {@code dbo}, {@code sys},
   * {@code guest}), which newer mssql-jdbc versions no longer return from {@link
   * #getSchemasForCatalog(String)} when a catalog name is provided.
   */
  ResultSet getSchemasWithoutCatalog() throws JdbcException {
    try {
      return metaData.getSchemas("", null);
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  ResultSet getTables(JdbcNamespaceInfo info) throws JdbcException {
    try {
      return metaData.getTables(
          info.jdbcCatalog(), info.jdbcSchema(), null, new String[] {TABLE_TYPE_TABLE});
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  ResultSet getColumns(JdbcNamespaceInfo info, String tableName) throws JdbcException {
    try {
      return metaData.getColumns(info.jdbcCatalog(), info.jdbcSchema(), tableName, null);
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  String getCatalogsTableCat(ResultSet catalogs) throws JdbcException {
    return getString(catalogs, CATALOGS_TABLE_CAT);
  }

  String getSchemasTableSchem(ResultSet schemas) throws JdbcException {
    return getString(schemas, SCHEMAS_TABLE_SCHEM);
  }

  Optional<String> getSchemasTableCatalog(ResultSet schemas) throws JdbcException {
    return getOptionalString(schemas, SCHEMAS_TABLE_CATALOG);
  }

  String getTablesTableName(ResultSet tables) throws JdbcException {
    return getString(tables, TABLES_TABLE_NAME);
  }

  String getColumnsColumnName(ResultSet columns) throws JdbcException {
    return getString(columns, COLUMNS_COLUMN_NAME);
  }

  int getColumnsDataType(ResultSet columns) throws JdbcException {
    return getInt(columns, COLUMNS_DATA_TYPE);
  }

  String getColumnsTypeName(ResultSet columns) throws JdbcException {
    return getString(columns, COLUMNS_TYPE_NAME);
  }

  int getColumnsColumnSize(ResultSet columns) throws JdbcException {
    return getInt(columns, COLUMNS_COLUMN_SIZE);
  }

  int getColumnsDecimalDigits(ResultSet columns) throws JdbcException {
    return getInt(columns, COLUMNS_DECIMAL_DIGITS);
  }

  int getColumnsNullable(ResultSet columns) throws JdbcException {
    return getInt(columns, COLUMNS_NULLABLE);
  }

  private String getString(ResultSet rs, String columnName) throws JdbcException {
    try {
      return rs.getString(columnName);
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  private Optional<String> getOptionalString(ResultSet rs, String columnName) throws JdbcException {
    try {
      return Optional.ofNullable(rs.getString(columnName));
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  private int getInt(ResultSet rs, String columnName) throws JdbcException {
    try {
      return rs.getInt(columnName);
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }
}
