/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.query;

import com.scalar.db.analytics.api.codec.provider.DataSourceProviderCodec;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.repository.CatalogRepository;
import com.scalar.db.analytics.repository.ColumnRepository;
import com.scalar.db.analytics.repository.DataSourceNamespaceTableQueryService;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.query.util.NameParsingUtility;
import com.scalar.db.analytics.repository.impl.spring.query.util.RowMapperThrowables;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class DataSourceNamespaceTableQueryServiceImpl
    implements DataSourceNamespaceTableQueryService<SpringDataJdbcTransactionContext> {

  private final NamedParameterJdbcOperations jdbcOperations;
  private final NameParsingUtility nameParsingUtility;
  private final DataSourceProviderCodec providerCodec;
  private final CatalogRepository<SpringDataJdbcTransactionContext> catalogRepository;
  private final TableRepository<SpringDataJdbcTransactionContext> tableRepository;
  private final DataSourceRepository<SpringDataJdbcTransactionContext> dataSourceRepository;
  private final NamespaceRepository<SpringDataJdbcTransactionContext> namespaceRepository;
  private final ColumnRepository<SpringDataJdbcTransactionContext> columnRepository;
  private final SpringDataJdbcErrorMapper errorMapper;

  public DataSourceNamespaceTableQueryServiceImpl(
      NamedParameterJdbcOperations jdbcOperations,
      NameParsingUtility nameParsingUtility,
      DataSourceProviderCodec providerCodec,
      CatalogRepository<SpringDataJdbcTransactionContext> catalogRepository,
      TableRepository<SpringDataJdbcTransactionContext> tableRepository,
      DataSourceRepository<SpringDataJdbcTransactionContext> dataSourceRepository,
      NamespaceRepository<SpringDataJdbcTransactionContext> namespaceRepository,
      ColumnRepository<SpringDataJdbcTransactionContext> columnRepository,
      SpringDataJdbcErrorMapper errorMapper) {
    this.jdbcOperations = jdbcOperations;
    this.nameParsingUtility = nameParsingUtility;
    this.providerCodec = providerCodec;
    this.catalogRepository = catalogRepository;
    this.tableRepository = tableRepository;
    this.dataSourceRepository = dataSourceRepository;
    this.namespaceRepository = namespaceRepository;
    this.columnRepository = columnRepository;
    this.errorMapper = errorMapper;
  }

  @Override
  public List<DataSourceNamespaceTable> listByCatalogName(
      SpringDataJdbcTransactionContext ctx, String catalogName) {
    String sql =
        "SELECT t.table_id AS table_id, "
            + "t.namespace_id AS namespace_id, "
            + "t.name AS table_name, "
            + "n.data_source_id AS data_source_id, "
            + "n.names AS names, "
            + "d.catalog_id AS catalog_id, "
            + "d.name AS data_source_name, "
            + "d.provider_payload_json AS provider_payload_json "
            + "FROM catalogs AS c "
            + "JOIN registry_data_sources AS d ON d.catalog_id = c.catalog_id "
            + "JOIN registry_namespaces AS n ON n.data_source_id = d.data_source_id "
            + "JOIN registry_tables AS t ON t.namespace_id = n.namespace_id "
            + "WHERE c.name = :catalogName";

    MapSqlParameterSource params = new MapSqlParameterSource("catalogName", catalogName);

    try {
      return jdbcOperations.query(
          sql,
          params,
          RowMapperThrowables.throwableRowMapper(rs -> mapDataSourceNamespaceTable(rs)));
    } catch (Exception e) {
      throw errorMapper.mapException(e, "Table", "list for catalog: " + catalogName);
    }
  }

  @Override
  public List<DataSourceNamespaceTable> listByNamespaceNames(
      SpringDataJdbcTransactionContext ctx,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames) {
    try {
      // ScalarDB SQL limitation: WHERE clause may reference only a single table, so resolve
      // Catalog→DataSource→Namespace via repositories before loading tables.
      Optional<Catalog> catalogOpt = catalogRepository.findByName(ctx, catalogName);
      if (catalogOpt.isEmpty()) {
        return List.of();
      }
      Catalog catalog = catalogOpt.get();

      Optional<DataSource> dataSourceOpt =
          dataSourceRepository.findByCatalogIdAndName(ctx, catalog.getId(), dataSourceName);
      if (dataSourceOpt.isEmpty()) {
        return List.of();
      }
      DataSource dataSource = dataSourceOpt.get();

      Optional<Namespace> namespaceOpt =
          namespaceRepository.findByDataSourceIdAndNames(ctx, dataSource.getId(), namespaceNames);
      if (namespaceOpt.isEmpty()) {
        return List.of();
      }
      Namespace namespace = namespaceOpt.get();

      return tableRepository.listByNamespaceId(ctx, namespace.getId()).stream()
          .map(table -> new DataSourceNamespaceTable(dataSource, namespace, table))
          .collect(Collectors.toList());
    } catch (AnalyticsException ex) {
      throw ex;
    } catch (Exception e) {
      throw errorMapper.mapException(
          e, "Table", dataSourceName + "." + String.join(".", namespaceNames));
    }
  }

  @Override
  public Optional<DataSourceNamespaceTableDetail> findDetailByName(
      SpringDataJdbcTransactionContext ctx,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName) {
    try {
      // ScalarDB SQL limitation: WHERE clause may reference only a single table, so resolve
      // Catalog→DataSource→Namespace via repositories before assembling table detail.
      Optional<Catalog> catalogOpt = catalogRepository.findByName(ctx, catalogName);
      if (catalogOpt.isEmpty()) {
        return Optional.empty();
      }

      Catalog catalog = catalogOpt.get();
      Optional<DataSource> dataSourceOpt =
          dataSourceRepository.findByCatalogIdAndName(ctx, catalog.getId(), dataSourceName);
      if (dataSourceOpt.isEmpty()) {
        return Optional.empty();
      }

      DataSource dataSource = dataSourceOpt.get();
      Optional<Namespace> namespaceOpt =
          namespaceRepository.findByDataSourceIdAndNames(ctx, dataSource.getId(), namespaceNames);
      if (namespaceOpt.isEmpty()) {
        return Optional.empty();
      }

      Namespace namespace = namespaceOpt.get();
      Optional<Table> tableOpt =
          tableRepository.findByNamespaceIdAndName(ctx, namespace.getId(), tableName);
      if (tableOpt.isEmpty()) {
        return Optional.empty();
      }

      Table table = tableOpt.get();
      DataSourceNamespaceTable dsnt = new DataSourceNamespaceTable(dataSource, namespace, table);

      List<Column> columns = columnRepository.listByTableId(ctx, table.getInfo().getId());

      // DataSource already contains provider in main branch model
      return Optional.of(dsnt.toDetail(columns));
    } catch (AnalyticsException ex) {
      throw ex;
    } catch (Exception e) {
      throw errorMapper.mapException(e, "Table", tableName);
    }
  }

  @Override
  public List<DataSourceNamespaceTable> listByNamespaceId(
      SpringDataJdbcTransactionContext ctx, UUID namespaceId) {
    String sql =
        "SELECT t.table_id AS table_id, "
            + "t.namespace_id AS namespace_id, "
            + "t.name AS table_name, "
            + "n.data_source_id AS data_source_id, "
            + "n.names AS namespace_names, "
            + "d.catalog_id AS catalog_id, "
            + "d.name AS data_source_name, "
            + "d.provider_payload_json AS provider_payload_json "
            + "FROM registry_tables AS t "
            + "JOIN registry_namespaces AS n ON t.namespace_id = n.namespace_id "
            + "JOIN registry_data_sources AS d ON n.data_source_id = d.data_source_id "
            + "WHERE t.namespace_id = :namespaceId";

    MapSqlParameterSource params = new MapSqlParameterSource("namespaceId", namespaceId.toString());

    try {
      return jdbcOperations.query(
          sql,
          params,
          RowMapperThrowables.throwableRowMapper(rs -> mapDataSourceNamespaceTable(rs)));
    } catch (Exception e) {
      throw errorMapper.mapException(e, "Table", "list for namespace: " + namespaceId);
    }
  }

  @Override
  public Optional<DataSourceNamespaceTableDetail> findDetailById(
      SpringDataJdbcTransactionContext ctx, UUID tableId) {
    String sql =
        "SELECT t.table_id, "
            + "t.namespace_id, "
            + "t.name AS table_name, "
            + "n.data_source_id, "
            + "n.names, "
            + "d.catalog_id, "
            + "d.name AS data_source_name, "
            + "d.provider_payload_json "
            + "FROM registry_tables AS t "
            + "JOIN registry_namespaces AS n ON t.namespace_id = n.namespace_id "
            + "JOIN registry_data_sources AS d ON n.data_source_id = d.data_source_id "
            + "WHERE t.table_id = :tableId";

    MapSqlParameterSource params = new MapSqlParameterSource("tableId", tableId.toString());

    try {
      List<DataSourceNamespaceTable> results =
          jdbcOperations.query(
              sql,
              params,
              RowMapperThrowables.throwableRowMapper(rs -> mapDataSourceNamespaceTable(rs)));

      if (results.isEmpty()) {
        return Optional.empty();
      }

      DataSourceNamespaceTable dsnt = results.get(0);
      List<Column> columns = columnRepository.listByTableId(ctx, dsnt.getTable().getInfo().getId());

      // DataSource already contains provider in main branch model
      return Optional.of(dsnt.toDetail(columns));
    } catch (AnalyticsException ex) {
      throw ex;
    } catch (Exception e) {
      throw errorMapper.mapException(e, "Table", tableId.toString());
    }
  }

  private DataSourceNamespaceTable mapDataSourceNamespaceTable(ResultSet rs) {
    String namespaceIdRaw = requiredString(rs, "namespace_id", null);
    String dataSourceIdRaw = requiredString(rs, "data_source_id", namespaceIdRaw);
    String identifier =
        String.format("namespace_id=%s, data_source_id=%s", namespaceIdRaw, dataSourceIdRaw);
    String catalogIdRaw = requiredString(rs, "catalog_id", identifier);
    String providerPayloadJson = requiredString(rs, "provider_payload_json", identifier);
    String tableIdRaw = requiredString(rs, "table_id", identifier);
    String tableName = requiredString(rs, "table_name", identifier);

    UUID namespaceId = parseUuid(namespaceIdRaw, "namespace_id", identifier);
    UUID dataSourceId = parseUuid(dataSourceIdRaw, "data_source_id", identifier);
    UUID catalogId = parseUuid(catalogIdRaw, "catalog_id", identifier);
    UUID tableId = parseUuid(tableIdRaw, "table_id", identifier);
    DataSourceProvider provider = deserializeProvider(providerPayloadJson, identifier);
    List<String> names = parseNames(rs, identifier);

    DataSource dataSource =
        new DataSource(dataSourceId, catalogId, stringValue(rs, "data_source_name"), provider);
    Namespace namespace = new Namespace(namespaceId, dataSourceId, names);
    Table table = new Table(new TableInfo(tableId, namespaceId, tableName));

    return new DataSourceNamespaceTable(dataSource, namespace, table);
  }

  private String requiredString(ResultSet rs, String column, @Nullable String identifier) {
    try {
      String value = rs.getString(column);
      if (value == null) {
        throw new AnalyticsException(
            AnalyticsErrorCode.DATA_INCONSISTENCY,
            Map.of(
                "field_name", column, "entity_name", identifier != null ? identifier : "unknown"));
      }
      return value;
    } catch (SQLException e) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of("field_name", column, "entity_name", identifier != null ? identifier : "unknown"),
          e);
    }
  }

  private String stringValue(ResultSet rs, String column) {
    try {
      return rs.getString(column);
    } catch (SQLException e) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY, Map.of("field_name", column), e);
    }
  }

  private static UUID parseUuid(String raw, String fieldName, String identifier) {
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of(
              "field_name", fieldName, "entity_name", identifier != null ? identifier : "unknown"),
          e);
    }
  }

  private DataSourceProvider deserializeProvider(String providerJson, String identifier) {
    try {
      return providerCodec.deserialize(providerJson);
    } catch (Exception e) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of(
              "field_name",
              "provider_payload_json",
              "entity_name",
              identifier != null ? identifier : "unknown"),
          e);
    }
  }

  private List<String> parseNames(ResultSet rs, String identifier) {
    try {
      return nameParsingUtility.parseNamesFromResultSet(rs);
    } catch (IOException primary) {
      try {
        return nameParsingUtility.parseNamesFromColumn(rs, "namespace_names");
      } catch (IOException | SQLException secondary) {
        throw new AnalyticsException(
            AnalyticsErrorCode.DATA_INCONSISTENCY,
            Map.of(
                "field_name", "names", "entity_name", identifier != null ? identifier : "unknown"),
            primary);
      }
    }
  }
}
