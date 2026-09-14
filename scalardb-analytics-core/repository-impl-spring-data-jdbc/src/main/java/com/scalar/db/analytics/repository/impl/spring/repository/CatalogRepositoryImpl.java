/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.CATALOG;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.Tenant;
import com.scalar.db.analytics.repository.CatalogRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.CatalogEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.CatalogJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class CatalogRepositoryImpl implements CatalogRepository<SpringDataJdbcTransactionContext> {

  private final CatalogJdbcRepository springDataRepository;
  private final SpringDataJdbcErrorMapper errorMapper;

  public CatalogRepositoryImpl(
      CatalogJdbcRepository springDataRepository, SpringDataJdbcErrorMapper errorMapper) {
    this.springDataRepository = springDataRepository;
    this.errorMapper = errorMapper;
  }

  @Override
  public Optional<Catalog> findById(SpringDataJdbcTransactionContext ctx, UUID id) {
    try {
      return springDataRepository.findById(id).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, CATALOG, id.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, CATALOG, id.toString());
    }
  }

  @Override
  public Optional<Catalog> findByName(SpringDataJdbcTransactionContext ctx, String name) {
    try {
      return springDataRepository.findByName(name).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, CATALOG, name);
    } catch (Exception e) {
      throw errorMapper.mapException(e, CATALOG, name);
    }
  }

  @Override
  public List<Catalog> list(SpringDataJdbcTransactionContext ctx) {
    try {
      return springDataRepository.findByTenantId(Tenant.DEFAULT_ID).stream()
          .map(this::toModel)
          .collect(Collectors.toList());
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, CATALOG, null);
    } catch (Exception e) {
      throw errorMapper.mapException(e, CATALOG, null);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void create(SpringDataJdbcTransactionContext ctx, Catalog catalog) {
    try {
      if (springDataRepository.findByName(catalog.getName()).isPresent()) {
        throw new AnalyticsException(
            AnalyticsErrorCode.CATALOG_ALREADY_EXISTS, Map.of("catalog_name", catalog.getName()));
      }
      CatalogEntity entity =
          new CatalogEntity(catalog.getId(), catalog.getTenantId(), catalog.getName());
      springDataRepository.insert(entity);
    } catch (AnalyticsException e) {
      throw e;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, CATALOG, catalog.getName());
    } catch (Exception e) {
      throw errorMapper.mapException(e, CATALOG, catalog.getName());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteByName(SpringDataJdbcTransactionContext ctx, String name) {
    try {
      Optional<CatalogEntity> catalog = springDataRepository.findByName(name);
      if (catalog.isPresent()) {
        springDataRepository.deleteById(catalog.get().getCatalogId());
      }
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, CATALOG, name);
    } catch (Exception e) {
      throw errorMapper.mapException(e, CATALOG, name);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteById(SpringDataJdbcTransactionContext ctx, UUID id) {
    try {
      // Check if exists before deleting to avoid database-specific errors
      if (springDataRepository.existsById(id)) {
        springDataRepository.deleteById(id);
      }
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, CATALOG, id.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, CATALOG, id.toString());
    }
  }

  private Catalog toModel(CatalogEntity entity) {
    return Catalog.of(entity.getCatalogId(), entity.getName());
  }
}
