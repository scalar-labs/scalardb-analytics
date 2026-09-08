/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.google.common.collect.ImmutableSet;
import com.scalar.db.analytics.api.model.DataType;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/** This class provides the schema resolution logic for an SQL Server data source. */
class SchemaResolverStrategySqlServer implements RdbmsSchemaResolverStrategy {
  @Nullable private final String onlyDatabaseToResolve;

  /**
   * The default schemas of SQL Server. These schemas are not considered as user schemas and are
   * ignored. The 'dbo' is also a default schema but is not included in this list as it is the
   * default schema for user-created objects.
   */
  private static final ImmutableSet<String> DEFAULT_SCHEMA =
      ImmutableSet.of("sys", "guest", "INFORMATION_SCHEMA");

  /**
   * The legacy default schemas of SQL Server. These schemas are not considered as user schemas and
   * are ignored.
   */
  private static final ImmutableSet<String> LEGACY_DEFAULT_SCHEMA =
      ImmutableSet.of(
          "db_accessadmin",
          "db_backupoperator",
          "db_datareader",
          "db_datawriter",
          "db_ddladmin",
          "db_denydatareader",
          "db_denydatawriter",
          "db_owner",
          "db_securityadmin");

  private static final ImmutableSet<String> SCHEMAS_TO_IGNORE =
      Stream.concat(DEFAULT_SCHEMA.stream(), LEGACY_DEFAULT_SCHEMA.stream())
          .collect(ImmutableSet.toImmutableSet());

  private static final ImmutableSet<String> DATABASES_TO_IGNORE =
      ImmutableSet.of("master", "model", "msdb", "tempdb");

  private static final ImmutableSet<String> SUPPORTED_BINARY_TYPES =
      ImmutableSet.of("binary", "varbinary");

  SchemaResolverStrategySqlServer(@Nullable String onlyDatabaseToResolve) {
    this.onlyDatabaseToResolve = onlyDatabaseToResolve;
  }

  /**
   * Resolves all the databases and schemas in the SQL Server. This ignores the default databases of
   * SQL Server, including 'master', 'model', 'msdb', and 'tempdb'. This also ignores the default
   * schemas of SQL Server, including 'sys', 'guest', 'INFORMATION_SCHEMA', and the legacy default
   * schemas.
   *
   * <p>The built-in 'dbo' schema is always resolved as a user-facing namespace because it is the
   * default location for user-created objects. mssql-jdbc 13.x stopped returning built-in schemas
   * (including 'dbo') from {@code getSchemas(catalog, null)} when a catalog is provided, so the
   * built-in schemas are obtained separately via {@link JdbcMetaData#getSchemasWithoutCatalog()}
   * and merged into every catalog. The merge is idempotent for older drivers that still include
   * them, and {@link #SCHEMAS_TO_IGNORE} removes everything except 'dbo'.
   */
  @Override
  public List<JdbcNamespaceInfo> resolveNamespaces(JdbcMetaData metaData) throws JdbcException {
    List<String> commonSchemas = getCommonSchemas(metaData);
    return getCatalogs(metaData).stream()
        .flatMap(catalog -> getNamespacesForCatalog(metaData, catalog, commonSchemas))
        .collect(Collectors.toList());
  }

  @Override
  public DataType jdbcTypeToDataType(JdbcColumnInfo column) throws UnsupportedJdbcTypeException {
    switch (column.getJdbcType()) {
      case BIT:
        if (column.getSize() == 1) {
          return DataType.Boolean.INSTANCE;
        }
        return DataType.Blob.INSTANCE;
      case TINYINT:
      case SMALLINT:
        return DataType.SmallInt.INSTANCE;
      case INTEGER:
        return DataType.Int.INSTANCE;
      case BIGINT:
        return DataType.BigInt.INSTANCE;
      case REAL:
        return DataType.Float.INSTANCE;
      case DOUBLE:
        return DataType.Double.INSTANCE;
      case CHAR:
      case NCHAR:
      case VARCHAR:
      case NVARCHAR:
        if (column.getTypeName().equalsIgnoreCase("uniqueidentifier")) {
          throw new UnsupportedJdbcTypeException(column);
        }
        return DataType.Text.INSTANCE;
      case LONGVARCHAR:
      case LONGNVARCHAR:
        if (column.getTypeName().equalsIgnoreCase("xml")) {
          throw new UnsupportedJdbcTypeException(column);
        }
        return DataType.Text.INSTANCE;
      case BINARY:
      case VARBINARY:
        if (!SUPPORTED_BINARY_TYPES.contains(column.getTypeName().toLowerCase())) {
          throw new UnsupportedJdbcTypeException(column);
        }
        return DataType.Blob.INSTANCE;
      case DATE:
        return DataType.Date.INSTANCE;
      case TIME:
        return DataType.Time.INSTANCE;
      case TIMESTAMP:
        return DataType.Timestamp.INSTANCE;
      case OTHER:
        if (column.getTypeName().equalsIgnoreCase("datetimeoffset")) {
          return DataType.TimestampTZ.INSTANCE;
        }
        throw new UnsupportedJdbcTypeException(column);
      default:
        throw new UnsupportedJdbcTypeException(column);
    }
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

  private Stream<NamespaceInfoSqlServer> getNamespacesForCatalog(
      JdbcMetaData metaData, String catalog, List<String> commonSchemas) throws JdbcException {
    if (DATABASES_TO_IGNORE.contains(catalog)) {
      return Stream.empty();
    }
    // Schemas specific to this catalog. Newer mssql-jdbc versions exclude built-in schemas here.
    Set<String> schemaNames = new LinkedHashSet<>();
    try (ResultSet schemas = metaData.getSchemasForCatalog(catalog)) {
      schemaNames.addAll(JdbcUtil.iterateOverResultSet(schemas, metaData::getSchemasTableSchem));
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
    // Built-in schemas (e.g. 'dbo') exist in every database. SCHEMAS_TO_IGNORE removes all of them
    // except 'dbo', leaving the catalog-specific schemas plus 'dbo'.
    schemaNames.addAll(commonSchemas);
    return schemaNames.stream()
        .filter(schema -> !SCHEMAS_TO_IGNORE.contains(schema))
        .map(schema -> new NamespaceInfoSqlServer(catalog, schema));
  }

  private List<String> getCommonSchemas(JdbcMetaData metaData) throws JdbcException {
    try (ResultSet schemas = metaData.getSchemasWithoutCatalog()) {
      return JdbcUtil.iterateOverResultSet(schemas, metaData::getSchemasTableSchem);
    } catch (SQLException e) {
      throw new JdbcException(e);
    }
  }
}
