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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

class SchemaResolverStrategySnowflake implements RdbmsSchemaResolverStrategy {

  @Nullable private final String onlyDatabaseToResolve;
  private static final ImmutableSet<String> DATABASES_TO_IGNORE = ImmutableSet.of("SNOWFLAKE");

  private static final ImmutableSet<String> SCHEMAS_TO_IGNORE =
      ImmutableSet.of("INFORMATION_SCHEMA");

  SchemaResolverStrategySnowflake(@Nullable String onlyDatabaseToResolve) {
    this.onlyDatabaseToResolve = onlyDatabaseToResolve;
  }

  @Override
  public DataType jdbcTypeToDataType(JdbcColumnInfo column) throws UnsupportedJdbcTypeException {
    switch (column.getJdbcType()) {
      case BIGINT:
        if (column.getDigits() != 0) {
          throw new UnsupportedJdbcTypeException(column);
        }
        if (column.getSize() <= 2) {
          return DataType.Byte.INSTANCE;
        }
        if (column.getSize() <= 4) {
          return DataType.SmallInt.INSTANCE;
        }
        if (column.getSize() <= 9) {
          return DataType.Int.INSTANCE;
        }
        if (column.getSize() <= 18) {
          return DataType.BigInt.INSTANCE;
        }
        // For size > 18, use DECIMAL
        return DataType.Decimal.builder().precision(column.getSize()).scale(0).build();
      case NUMERIC:
      case DECIMAL:
        /*
         * Snowflake NUMERIC/DECIMAL type stores exact numeric values.
         * Since standard SQL does not define default values, and they vary across databases,
         * we apply precision = 38 and scale = 0 as the default values when precision
         * and scale are not specified during table creation.
         */
        int precision = column.getSize();
        int scale = column.getDigits();
        if (precision == 0 && scale == 0) {
          precision = 38;
        }
        return DataType.Decimal.builder().precision(precision).scale(scale).build();
      case DOUBLE:
        return DataType.Double.INSTANCE;
      case VARCHAR:
        if (List.of("ARRAY", "OBJECT", "VARIANT", "GEOGRAPHY", "GEOMETRY")
            .contains(column.getTypeName().toUpperCase())) {
          throw new UnsupportedJdbcTypeException(column);
        }
        return DataType.Text.INSTANCE;
      case BINARY:
        return DataType.Blob.INSTANCE;
      case BOOLEAN:
        return DataType.Boolean.INSTANCE;
      case DATE:
        return DataType.Date.INSTANCE;
      case TIME:
        return DataType.Time.INSTANCE;
      case TIMESTAMP:
        // Mapping for the Snowflake type TIMESTAMP_NTZ
        if (column.getTypeName().equalsIgnoreCase("TIMESTAMPNTZ")) {
          return DataType.Timestamp.INSTANCE;
        }
        // Mapping for the Snowflake type TIMESTAMP_LTZ
        return DataType.TimestampTZ.INSTANCE;
      case TIMESTAMP_WITH_TIMEZONE:
        // Mapping for the Snowflake type TIMESTAMP_TZ
        return DataType.TimestampTZ.INSTANCE;
      default:
        throw new UnsupportedJdbcTypeException(column);
    }
  }

  /** This ignores the system database "SNOWFLAKE" and the system schema "INFORMATION_SCHEMA". */
  @Override
  public List<JdbcNamespaceInfo> resolveNamespaces(JdbcMetaData metaData) throws JdbcException {
    List<JdbcNamespaceInfo> namespaceInfos =
        getCatalogs(metaData).stream()
            .flatMap(catalog -> getSchemasForCatalog(metaData, catalog))
            .collect(Collectors.toList());

    return namespaceInfos;
  }

  private List<String> getCatalogs(JdbcMetaData metaData) {
    List<String> catalogs;
    try (ResultSet cs = metaData.getCatalogs()) {
      catalogs = JdbcUtil.iterateOverResultSet(cs, metaData::getCatalogsTableCat);
      if (onlyDatabaseToResolve != null) {
        catalogs.removeIf(c -> !c.equals(onlyDatabaseToResolve));
      }
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
    return catalogs;
  }

  private Stream<NamespaceInfoSnowflake> getSchemasForCatalog(
      JdbcMetaData metaData, String database) throws JdbcException {
    if (DATABASES_TO_IGNORE.contains(database)) {
      return Stream.empty();
    }
    try (ResultSet schemas = metaData.getSchemasForCatalog(database)) {
      return JdbcUtil.iterateOverResultSet(
          schemas,
          rs -> {
            String schema = metaData.getSchemasTableSchem(rs);
            if (SCHEMAS_TO_IGNORE.contains(schema)) {
              return null;
            }
            return new NamespaceInfoSnowflake(database, schema);
          })
          .stream();
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }
}
