/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.google.common.collect.ImmutableSet;
import com.scalar.db.analytics.api.model.DataType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;

/** This class provides the schema resolution logic for a MySQL data source. */
@Slf4j
class SchemaResolverStrategyMysql implements RdbmsSchemaResolverStrategy {
  @Nullable private final String onlyDatabaseToResolve;

  private static final ImmutableSet<String> DATABASES_TO_IGNORE =
      ImmutableSet.of("mysql", "sys", "information_schema", "performance_schema");

  private static final ImmutableSet<String> SUPPORTED_TEXT_TYPES =
      ImmutableSet.of("char", "varchar", "tinytext", "text", "mediumtext", "longtext");

  private static final ImmutableSet<String> SUPPORTED_BLOB_TYPES =
      ImmutableSet.of("binary", "varbinary", "tinyblob", "blob", "mediumblob", "longblob");

  SchemaResolverStrategyMysql(@Nullable String onlyDatabaseToResolve) {
    this.onlyDatabaseToResolve = onlyDatabaseToResolve;
  }

  /**
   * Resolves all the databases in the MySQL server. This ignores the default databases of MySQL,
   * including 'mysql', 'sys', 'information_schema', and 'performance_schema'.
   */
  @Override
  public List<JdbcNamespaceInfo> resolveNamespaces(JdbcMetaData metaData) throws JdbcException {
    try (ResultSet catalogs = metaData.getCatalogs()) {
      return JdbcUtil.iterateOverResultSet(
          catalogs,
          rs -> {
            String catalog = metaData.getCatalogsTableCat(rs);
            if (DATABASES_TO_IGNORE.contains(catalog)) {
              return null;
            }
            if (onlyDatabaseToResolve != null && !onlyDatabaseToResolve.equals(catalog)) {
              return null;
            }
            return new NamespaceInfoMySql(catalog);
          });
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  /**
   * Converts the JDBC type to the data type of the virtualized database. This method should be as
   * compatible as possible to {@code com.scalar.db.storage.jdbc.RdbEngineMySql} to keep the
   * consistency.
   *
   * <p>We also respect the JDBC type conversions exposed by MariaDB Connector/J.
   */
  @Override
  public DataType jdbcTypeToDataType(JdbcColumnInfo column) throws UnsupportedJdbcTypeException {
    switch (column.getJdbcType()) {
      case BOOLEAN:
        return DataType.Boolean.INSTANCE;
      case BIT:
        if (column.getSize() == 1) {
          return DataType.Boolean.INSTANCE;
        } else {
          return DataType.Blob.INSTANCE;
        }
      case TINYINT:
        // MariaDB Connector/J reports tinyint(1) as JDBCType.BOOLEAN, so only TINYINT and
        // TINYINT UNSIGNED reach here.
        return DataType.SmallInt.INSTANCE;
      case SMALLINT:
        if (column.getTypeName().toLowerCase().endsWith("unsigned")) {
          DataType.Int intType = DataType.Int.INSTANCE;
          warnMappingToLargerTypeForUnsignedType(column, intType);
          return intType;
        }
        return DataType.SmallInt.INSTANCE;
      case INTEGER:
        if (column.getTypeName().toLowerCase().startsWith("mediumint")) {
          return DataType.Int.INSTANCE;
        } else if (column.getTypeName().toLowerCase().endsWith("unsigned")) {
          DataType.BigInt bigIntType = DataType.BigInt.INSTANCE;
          warnMappingToLargerTypeForUnsignedType(column, bigIntType);
          return bigIntType;
        }
        return DataType.Int.INSTANCE;
      case BIGINT:
        if (column.getTypeName().toLowerCase().endsWith("unsigned")) {
          throw new UnsupportedJdbcTypeException(column);
        }
        return DataType.BigInt.INSTANCE;
      case REAL:
        // MySQL FLOAT type is mapped to REAL in JDBC
        return DataType.Float.INSTANCE;
      case DOUBLE:
        return DataType.Double.INSTANCE;
      case CHAR:
      case VARCHAR:
      case LONGVARCHAR:
        if (!SUPPORTED_TEXT_TYPES.contains(column.getTypeName().toLowerCase())) {
          // to exclude ENUM, SET, JSON, etc.
          throw new UnsupportedJdbcTypeException(column);
        }
        return DataType.Text.INSTANCE;
      case BINARY:
      case VARBINARY:
      case LONGVARBINARY:
        if (!SUPPORTED_BLOB_TYPES.contains(column.getTypeName().toLowerCase())) {
          throw new UnsupportedJdbcTypeException(column);
        }
        return DataType.Blob.INSTANCE;
      case DATE:
        if (column.getTypeName().equalsIgnoreCase("year")) {
          throw new UnsupportedJdbcTypeException(column);
        }
        return DataType.Date.INSTANCE;
      case TIME:
        return DataType.Time.INSTANCE;
      case TIMESTAMP:
        if (column.getTypeName().equalsIgnoreCase("datetime")) {
          return DataType.Timestamp.INSTANCE;
        }
        return DataType.TimestampTZ.INSTANCE;
      default:
        throw new UnsupportedJdbcTypeException(column);
    }
  }

  private void warnMappingToLargerTypeForUnsignedType(JdbcColumnInfo column, DataType dataType) {
    logger.info(
        "Data type larger than that of underlying database is assigned: {} ({} to {})",
        column.getQualifiedName(),
        column.getTypeName(),
        dataType.getKind());
  }
}
