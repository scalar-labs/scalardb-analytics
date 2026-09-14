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

class SchemaResolverStrategyDatabricks implements RdbmsSchemaResolverStrategy {

  @Nullable private final String onlyCatalogToResolve;
  private static final ImmutableSet<String> CATALOGS_TO_IGNORE = ImmutableSet.of("system");

  // "global_temp" and "routines" schemas are only visible through the JDBC driver metadata but not
  // via the Databricks web dashboard.
  private static final ImmutableSet<String> SCHEMAS_TO_IGNORE =
      ImmutableSet.of("information_schema", "global_temp", "sys", "routines");

  SchemaResolverStrategyDatabricks(@Nullable String onlyCatalogToResolve) {
    this.onlyCatalogToResolve = onlyCatalogToResolve;
  }

  @Override
  public DataType jdbcTypeToDataType(JdbcColumnInfo column) throws UnsupportedJdbcTypeException {
    switch (column.getJdbcType()) {
      case TINYINT:
      case SMALLINT:
        return DataType.SmallInt.INSTANCE;
      case INTEGER:
        return DataType.Int.INSTANCE;
      case BIGINT:
        return DataType.BigInt.INSTANCE;
      case FLOAT:
        return DataType.Float.INSTANCE;
      case DOUBLE:
        return DataType.Double.INSTANCE;
      case DECIMAL:
        int precision = column.getSize();
        int scale = column.getDigits();
        /*
         * Since standard SQL does not define default values, and they vary across databases,
         * thus, we apply precision = 38 and scale = 0 as the default values when precision
         * and scale are not specified during table creation.
         */
        if (precision == 0 && scale == 0) {
          precision = 38;
        }

        // Optimization: For integer-like decimals with small precision, use integer types
        if (scale == 0) {
          if (precision <= 2) {
            return DataType.Byte.INSTANCE;
          }
          if (precision <= 4) {
            return DataType.SmallInt.INSTANCE;
          }
          if (precision <= 9) {
            return DataType.Int.INSTANCE;
          }
          if (precision <= 18) {
            return DataType.BigInt.INSTANCE;
          }
        }

        // For all other cases (including scale != 0 or precision > 18), use DECIMAL
        return DataType.Decimal.builder().precision(precision).scale(scale).build();
      case VARCHAR:
        if (column.getTypeName().toUpperCase().contains("INTERVAL")) {
          throw new UnsupportedJdbcTypeException(column);
        }
        return DataType.Text.INSTANCE;
      case BINARY:
        return DataType.Blob.INSTANCE;
      case BOOLEAN:
        return DataType.Boolean.INSTANCE;
      case DATE:
        return DataType.Date.INSTANCE;
      case TIMESTAMP:
        if (column.getTypeName().equalsIgnoreCase("TIMESTAMP")) {
          return DataType.TimestampTZ.INSTANCE;
        }
        return DataType.Timestamp.INSTANCE;
      default:
        throw new UnsupportedJdbcTypeException(column);
    }
  }

  /**
   * Resolves all the databases and schemas in Databricks. This ignores the shared "samples" catalog
   * as well as the "system" catalog and the system "information_schema" schema.
   */
  @Override
  public List<JdbcNamespaceInfo> resolveNamespaces(JdbcMetaData metaData) throws JdbcException {
    return getCatalogs(metaData).stream()
        .flatMap(catalog -> getSchemasForCatalog(metaData, catalog))
        .collect(Collectors.toList());
  }

  private List<String> getCatalogs(JdbcMetaData metaData) {
    List<String> catalogs;
    try (ResultSet cs = metaData.getCatalogs()) {
      catalogs = JdbcUtil.iterateOverResultSet(cs, metaData::getCatalogsTableCat);
      if (onlyCatalogToResolve != null) {
        catalogs.removeIf(c -> !c.equals(onlyCatalogToResolve));
      }
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
    return catalogs;
  }

  private Stream<NamespaceInfoDatabricks> getSchemasForCatalog(
      JdbcMetaData metaData, String catalog) throws JdbcException {
    if (CATALOGS_TO_IGNORE.contains(catalog)) {
      return Stream.empty();
    }
    try (ResultSet schemas = metaData.getSchemasForCatalog(catalog)) {
      return JdbcUtil.iterateOverResultSet(
          schemas,
          rs -> {
            String schema = metaData.getSchemasTableSchem(rs);
            if (SCHEMAS_TO_IGNORE.contains(schema)) {
              return null;
            }
            return new NamespaceInfoDatabricks(catalog, schema);
          })
          .stream();
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }
}
