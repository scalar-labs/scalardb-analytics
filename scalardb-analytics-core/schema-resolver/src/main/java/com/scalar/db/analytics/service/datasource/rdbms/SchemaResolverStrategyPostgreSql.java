/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.google.common.collect.ImmutableSet;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/** This class provides the schema resolution logic for a PostgreSQL data source. */
@Slf4j
class SchemaResolverStrategyPostgreSql implements RdbmsSchemaResolverStrategy {
  private static final ImmutableSet<String> SCHEMAS_TO_IGNORE =
      ImmutableSet.of("information_schema", "pg_catalog");

  /**
   * Resolves all the schemas in the specified database. This ignores the default schemas of
   * PostgreSQL, including 'information_schema' and 'pg_catalog'.
   */
  @Override
  public List<JdbcNamespaceInfo> resolveNamespaces(JdbcMetaData metaData)
      throws SchemaResolverException {
    try (ResultSet schemas = metaData.getSchemas()) {
      return JdbcUtil.iterateOverResultSet(
              schemas, rs -> new NamespaceInfoPostgreSql(metaData.getSchemasTableSchem(rs)))
          .stream()
          .filter(schema -> !SCHEMAS_TO_IGNORE.contains(schema.getSchema()))
          .collect(Collectors.toList());
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }

  /**
   * Converts the JDBC type to the data type of the virtualized database. This method should be as
   * compatible as possible to {@code com.scalar.db.storage.jdbc.RdbEnginePostgreSql} to keep the
   * consistency.
   *
   * <p>Reference: <a href="https://www.postgresql.org/docs/current/datatype.html">PostgreSQL:
   * Documentation: Data Types</a>
   */
  @Override
  public DataType jdbcTypeToDataType(JdbcColumnInfo column) throws JdbcException {
    switch (column.getJdbcType()) {
      case BIT:
        if (column.getTypeName().equalsIgnoreCase("bool")) {
          return DataType.Boolean.INSTANCE;
        }
        return DataType.Blob.INSTANCE;
      case SMALLINT:
        return DataType.SmallInt.INSTANCE;
      case INTEGER:
        return DataType.Int.INSTANCE;
      case BIGINT:
        return DataType.BigInt.INSTANCE;
      case REAL:
        return DataType.Float.INSTANCE;
      case DOUBLE:
        if (!column.getTypeName().equalsIgnoreCase("float8")) {
          throw new UnsupportedJdbcTypeException(column);
        }
        return DataType.Double.INSTANCE;
      case CHAR:
      case VARCHAR:
        return DataType.Text.INSTANCE;
      case BINARY:
        return DataType.Blob.INSTANCE;
      case DATE:
        return DataType.Date.INSTANCE;
      case TIME:
        if (column.getTypeName().equalsIgnoreCase("timetz")) {
          logger.warn(
              "Timezone information of the time type is ignored: {}", column.getQualifiedName());
        }
        return DataType.Time.INSTANCE;
      case TIMESTAMP:
        if (column.getTypeName().equalsIgnoreCase("timestamptz")) {
          return DataType.TimestampTZ.INSTANCE;
        }
        return DataType.Timestamp.INSTANCE;
      default:
        throw new UnsupportedJdbcTypeException(column);
    }
  }
}
