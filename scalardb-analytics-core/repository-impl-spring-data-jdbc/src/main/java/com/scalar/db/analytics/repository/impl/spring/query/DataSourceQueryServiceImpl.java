/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.query;

import com.scalar.db.analytics.api.codec.provider.DataSourceProviderCodec;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.repository.DataSourceQueryService;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class DataSourceQueryServiceImpl
    implements DataSourceQueryService<SpringDataJdbcTransactionContext> {

  private final NamedParameterJdbcOperations jdbcOperations;
  private final DataSourceProviderCodec providerCodec;
  private final SpringDataJdbcErrorMapper errorMapper;

  public DataSourceQueryServiceImpl(
      NamedParameterJdbcOperations jdbcOperations,
      DataSourceProviderCodec providerCodec,
      SpringDataJdbcErrorMapper errorMapper) {
    this.jdbcOperations = jdbcOperations;
    this.providerCodec = providerCodec;
    this.errorMapper = errorMapper;
  }

  @Override
  public Optional<DataSource> findByName(
      SpringDataJdbcTransactionContext ctx, String catalogName, String dataSourceName) {
    Optional<UUID> catalogIdOpt = fetchCatalogId(catalogName);
    if (catalogIdOpt.isEmpty()) {
      return Optional.empty();
    }
    UUID catalogId = catalogIdOpt.get();

    try {
      String sql =
          "SELECT data_source_id, catalog_id, name, provider_type, provider_payload_json "
              + "FROM registry_data_sources "
              + "WHERE catalog_id = :catalogId AND name = :dataSourceName";

      MapSqlParameterSource params = new MapSqlParameterSource();
      params.addValue("catalogId", catalogId.toString());
      params.addValue("dataSourceName", dataSourceName);

      List<DataSource> results =
          jdbcOperations.query(
              sql,
              params,
              (rs, rowNum) -> {
                UUID dataSourceId = UUID.fromString(rs.getString("data_source_id"));
                UUID catalogIdFromRow = UUID.fromString(rs.getString("catalog_id"));
                String name = rs.getString("name");
                String providerJson = rs.getString("provider_payload_json");

                DataSourceProvider provider = providerCodec.deserialize(providerJson);

                return new DataSource(dataSourceId, catalogIdFromRow, name, provider);
              });

      if (results.isEmpty()) {
        return Optional.empty();
      }

      return Optional.of(results.get(0));
    } catch (Exception e) {
      throw errorMapper.mapException(
          e, "DataSource", dataSourceName != null ? dataSourceName : "unknown");
    }
  }

  @Override
  public List<DataSource> listByCatalogName(
      SpringDataJdbcTransactionContext ctx, String catalogName) {
    Optional<UUID> catalogIdOpt = fetchCatalogId(catalogName);
    if (catalogIdOpt.isEmpty()) {
      return List.of();
    }
    UUID catalogId = catalogIdOpt.get();

    try {
      String sql =
          "SELECT data_source_id, catalog_id, name, provider_payload_json "
              + "FROM registry_data_sources "
              + "WHERE catalog_id = :catalogId";

      MapSqlParameterSource params = new MapSqlParameterSource("catalogId", catalogId.toString());

      return jdbcOperations.query(
          sql,
          params,
          (rs, rowNum) -> {
            UUID dataSourceId = UUID.fromString(rs.getString("data_source_id"));
            UUID catalogIdFromRow = UUID.fromString(rs.getString("catalog_id"));
            String name = rs.getString("name");
            String providerJson = rs.getString("provider_payload_json");
            DataSourceProvider provider = providerCodec.deserialize(providerJson);
            return new DataSource(dataSourceId, catalogIdFromRow, name, provider);
          });
    } catch (Exception e) {
      throw errorMapper.mapException(e, "DataSource", "list for catalog: " + catalogName);
    }
  }

  private Optional<UUID> fetchCatalogId(String catalogName) {
    try {
      String sql = "SELECT catalog_id FROM catalogs WHERE name = :catalogName";

      MapSqlParameterSource params = new MapSqlParameterSource("catalogName", catalogName);
      List<String> rows =
          jdbcOperations.query(sql, params, (rs, rowNum) -> rs.getString("catalog_id"));
      if (rows.isEmpty()) {
        return Optional.empty();
      }
      try {
        return Optional.of(UUID.fromString(rows.get(0)));
      } catch (IllegalArgumentException e) {
        throw new AnalyticsException(
            AnalyticsErrorCode.DATA_INCONSISTENCY,
            Map.of("field_name", "catalog_id", "catalog_name", catalogName),
            e);
      }
    } catch (AnalyticsException e) {
      throw e;
    } catch (Exception e) {
      throw errorMapper.mapException(e, "Catalog", catalogName);
    }
  }
}
