/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.query;

import com.scalar.db.analytics.api.codec.provider.DataSourceProviderCodec;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.repository.CatalogRepository;
import com.scalar.db.analytics.repository.DataSourceNamespaceQueryService;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
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
import org.jspecify.annotations.Nullable;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class DataSourceNamespaceQueryServiceImpl
    implements DataSourceNamespaceQueryService<SpringDataJdbcTransactionContext> {

  private final NamedParameterJdbcOperations jdbcOperations;
  private final NameParsingUtility nameParsingUtility;
  private final DataSourceProviderCodec providerCodec;
  private final CatalogRepository<SpringDataJdbcTransactionContext> catalogRepository;
  private final DataSourceRepository<SpringDataJdbcTransactionContext> dataSourceRepository;
  private final NamespaceRepository<SpringDataJdbcTransactionContext> namespaceRepository;
  private final SpringDataJdbcErrorMapper errorMapper;

  public DataSourceNamespaceQueryServiceImpl(
      NamedParameterJdbcOperations jdbcOperations,
      NameParsingUtility nameParsingUtility,
      DataSourceProviderCodec providerCodec,
      CatalogRepository<SpringDataJdbcTransactionContext> catalogRepository,
      DataSourceRepository<SpringDataJdbcTransactionContext> dataSourceRepository,
      NamespaceRepository<SpringDataJdbcTransactionContext> namespaceRepository,
      SpringDataJdbcErrorMapper errorMapper) {
    this.jdbcOperations = jdbcOperations;
    this.nameParsingUtility = nameParsingUtility;
    this.providerCodec = providerCodec;
    this.catalogRepository = catalogRepository;
    this.dataSourceRepository = dataSourceRepository;
    this.namespaceRepository = namespaceRepository;
    this.errorMapper = errorMapper;
  }

  @Override
  public List<DataSourceNamespace> listByCatalogName(
      SpringDataJdbcTransactionContext ctx, String catalogName) {
    String sql =
        "SELECT n.namespace_id AS namespace_id, "
            + "n.data_source_id AS data_source_id, "
            + "n.names AS names, "
            + "d.catalog_id AS catalog_id, "
            + "d.name AS data_source_name, "
            + "d.provider_payload_json AS provider_payload_json "
            + "FROM catalogs AS c "
            + "JOIN registry_data_sources AS d ON d.catalog_id = c.catalog_id "
            + "JOIN registry_namespaces AS n ON n.data_source_id = d.data_source_id "
            + "WHERE c.name = :catalogName";

    MapSqlParameterSource params = new MapSqlParameterSource("catalogName", catalogName);

    try {
      return jdbcOperations.query(
          sql, params, RowMapperThrowables.throwableRowMapper(rs -> mapDataSourceNamespace(rs)));
    } catch (Exception e) {
      throw errorMapper.mapException(e, "Namespace", catalogName);
    }
  }

  @Override
  public List<DataSourceNamespace> listByDataSourceId(
      SpringDataJdbcTransactionContext ctx, UUID dataSourceId) {
    String sql =
        "SELECT n.namespace_id AS namespace_id, "
            + "n.data_source_id AS data_source_id, "
            + "n.names AS names, "
            + "d.catalog_id AS catalog_id, "
            + "d.name AS data_source_name, "
            + "d.provider_payload_json AS provider_payload_json "
            + "FROM registry_namespaces AS n "
            + "JOIN registry_data_sources AS d ON n.data_source_id = d.data_source_id "
            + "WHERE n.data_source_id = :dataSourceId";

    MapSqlParameterSource params =
        new MapSqlParameterSource("dataSourceId", dataSourceId.toString());

    try {
      return jdbcOperations.query(
          sql, params, RowMapperThrowables.throwableRowMapper(rs -> mapDataSourceNamespace(rs)));
    } catch (Exception e) {
      throw errorMapper.mapException(e, "Namespace", dataSourceId.toString());
    }
  }

  @Override
  public Optional<DataSourceNamespace> findByCatalogAndDataSourceAndNames(
      SpringDataJdbcTransactionContext ctx,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames) {
    try {
      // ScalarDB SQL limitation: resolve Catalog→DataSource→Namespace via repositories
      // before issuing namespace lookups to keep WHERE clauses single-table.
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
      return Optional.of(new DataSourceNamespace(dataSource, namespace));
    } catch (AnalyticsException ex) {
      throw ex;
    } catch (Exception e) {
      throw errorMapper.mapException(e, "Namespace", String.join(".", namespaceNames));
    }
  }

  private DataSourceNamespace mapDataSourceNamespace(ResultSet rs) {
    String namespaceIdRaw = getRequiredString(rs, "namespace_id", null);
    String dataSourceIdRaw =
        getRequiredString(rs, "data_source_id", "namespace_id=" + namespaceIdRaw);
    String identifier = buildIdentifier(namespaceIdRaw, dataSourceIdRaw);
    String catalogIdRaw = getRequiredString(rs, "catalog_id", identifier);
    String providerPayloadJson = getRequiredString(rs, "provider_payload_json", identifier);

    UUID namespaceId = parseUuid(namespaceIdRaw, "namespace_id", identifier);
    UUID dataSourceId = parseUuid(dataSourceIdRaw, "data_source_id", identifier);
    UUID catalogId = parseUuid(catalogIdRaw, "catalog_id", identifier);
    DataSourceProvider provider = deserializeProvider(providerPayloadJson, identifier);
    List<String> names = parseNames(rs, identifier);

    DataSource dataSource =
        new DataSource(
            dataSourceId,
            catalogId,
            getRequiredString(rs, "data_source_name", identifier),
            provider);
    Namespace namespace = new Namespace(namespaceId, dataSourceId, names);
    return new DataSourceNamespace(dataSource, namespace);
  }

  private static String buildIdentifier(String namespaceIdRaw, String dataSourceIdRaw) {
    return String.format("namespace_id=%s, data_source_id=%s", namespaceIdRaw, dataSourceIdRaw);
  }

  private @Nullable String getString(ResultSet rs, String column, @Nullable String identifier) {
    try {
      return rs.getString(column);
    } catch (SQLException e) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of("field_name", column, "entity_name", identifier != null ? identifier : "unknown"),
          e);
    }
  }

  private String getRequiredString(ResultSet rs, String column, @Nullable String identifier) {
    @Nullable String value = getString(rs, column, identifier);
    if (value == null) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of("field_name", column, "entity_name", identifier != null ? identifier : "unknown"));
    }
    return value;
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
    } catch (IOException e) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of("field_name", "names", "entity_name", identifier != null ? identifier : "unknown"),
          e);
    }
  }
}
