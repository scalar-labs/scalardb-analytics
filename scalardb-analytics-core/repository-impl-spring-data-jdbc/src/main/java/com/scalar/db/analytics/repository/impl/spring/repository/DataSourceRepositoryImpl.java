/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.DATA_SOURCE;

import com.scalar.db.analytics.api.codec.provider.DataSourceProviderCodec;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.DataSourceEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.DataSourceJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class DataSourceRepositoryImpl
    implements DataSourceRepository<SpringDataJdbcTransactionContext> {

  private final DataSourceJdbcRepository springDataRepository;
  private final SpringDataJdbcErrorMapper errorMapper;
  private final DataSourceProviderCodec providerCodec;

  public DataSourceRepositoryImpl(
      DataSourceJdbcRepository springDataRepository,
      SpringDataJdbcErrorMapper errorMapper,
      DataSourceProviderCodec providerCodec) {
    this.springDataRepository = springDataRepository;
    this.errorMapper = errorMapper;
    this.providerCodec = providerCodec;
  }

  @Override
  public Optional<DataSource> findById(SpringDataJdbcTransactionContext ctx, UUID id) {
    try {
      return springDataRepository.findById(id).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, DATA_SOURCE, id.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, DATA_SOURCE, id.toString());
    }
  }

  @Override
  public Optional<DataSource> findByCatalogIdAndName(
      SpringDataJdbcTransactionContext ctx, UUID catalogId, String name) {
    try {
      return springDataRepository.findByCatalogIdAndName(catalogId, name).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, DATA_SOURCE, catalogId + "/" + name);
    } catch (Exception e) {
      throw errorMapper.mapException(e, DATA_SOURCE, catalogId + "/" + name);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void create(SpringDataJdbcTransactionContext ctx, DataSource dataSource) {
    try {
      // ScalarDB SQL does not enforce UNIQUE constraints; guard natural-key collisions here.
      if (springDataRepository
          .findByCatalogIdAndName(dataSource.getCatalogId(), dataSource.getName())
          .isPresent()) {
        throw new AnalyticsException(
            AnalyticsErrorCode.DATA_SOURCE_ALREADY_EXISTS,
            Map.of("data_source_name", dataSource.getName()));
      }

      // Serialize provider to JSON
      DataSourceProvider provider = dataSource.getProvider();
      String providerJson = providerCodec.serialize(provider);

      DataSourceEntity entity =
          new DataSourceEntity(
              dataSource.getId(),
              dataSource.getCatalogId(),
              dataSource.getName(),
              provider.getType(),
              providerJson);
      springDataRepository.insert(entity);
    } catch (AnalyticsException e) {
      // Preserve explicit EntityAlreadyExists signal for callers.
      throw e;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, DATA_SOURCE, null);
    } catch (Exception e) {
      throw errorMapper.mapException(e, DATA_SOURCE, null);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteById(SpringDataJdbcTransactionContext ctx, UUID id) {
    try {
      // Simply delete the data source (provider data is in JSON column)
      if (springDataRepository.existsById(id)) {
        springDataRepository.deleteById(id);
      }
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, DATA_SOURCE, id.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, DATA_SOURCE, id.toString());
    }
  }

  private DataSource toModel(DataSourceEntity entity) {
    DataSourceProvider provider = providerCodec.deserialize(entity.getProviderPayloadJson());
    return new DataSource(
        entity.getDataSourceId(), entity.getCatalogId(), entity.getName(), provider);
  }
}
