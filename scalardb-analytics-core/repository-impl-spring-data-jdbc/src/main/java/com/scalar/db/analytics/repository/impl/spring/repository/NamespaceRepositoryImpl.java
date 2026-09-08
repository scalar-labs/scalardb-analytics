/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.NAMESPACE;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.impl.spring.converter.StringList;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.NamespaceEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.NamespaceJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class NamespaceRepositoryImpl
    implements NamespaceRepository<SpringDataJdbcTransactionContext> {

  private final NamespaceJdbcRepository springDataRepository;
  private final SpringDataJdbcErrorMapper errorMapper;

  public NamespaceRepositoryImpl(
      NamespaceJdbcRepository springDataRepository, SpringDataJdbcErrorMapper errorMapper) {
    this.springDataRepository = springDataRepository;
    this.errorMapper = errorMapper;
  }

  @Override
  public Optional<Namespace> findById(SpringDataJdbcTransactionContext ctx, UUID id) {
    try {
      return springDataRepository.findById(id).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, NAMESPACE, id.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, NAMESPACE, id.toString());
    }
  }

  @Override
  public Optional<Namespace> findByDataSourceIdAndNames(
      SpringDataJdbcTransactionContext ctx, UUID dataSourceId, List<String> names) {
    try {
      return springDataRepository
          .findByDataSourceIdAndNames(dataSourceId, new StringList(names))
          .map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, NAMESPACE, dataSourceId + "/" + String.join(".", names));
    } catch (Exception e) {
      throw errorMapper.mapException(e, NAMESPACE, dataSourceId + "/" + String.join(".", names));
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void create(SpringDataJdbcTransactionContext ctx, Namespace namespace) {
    try {
      // ScalarDB SQL does not enforce UNIQUE constraints; guard natural-key collisions here.
      if (springDataRepository
          .findByDataSourceIdAndNames(
              namespace.getDataSourceId(), new StringList(namespace.getNames()))
          .isPresent()) {
        throw new AnalyticsException(
            AnalyticsErrorCode.NAMESPACE_ALREADY_EXISTS,
            Map.of("namespace_name", String.join(".", namespace.getNames())));
      }

      NamespaceEntity entity =
          new NamespaceEntity(
              namespace.getId(), namespace.getDataSourceId(), new StringList(namespace.getNames()));
      springDataRepository.insert(entity);
    } catch (AnalyticsException e) {
      // Preserve explicit EntityAlreadyExists signal for callers.
      throw e;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, NAMESPACE, null);
    } catch (Exception e) {
      throw errorMapper.mapException(e, NAMESPACE, null);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteById(SpringDataJdbcTransactionContext ctx, UUID id) {
    try {
      // ScalarDB SQL lacks COUNT; use a simple lookup to confirm presence
      if (springDataRepository.findById(id).isPresent()) {
        springDataRepository.deleteById(id);
      }
    } catch (org.springframework.dao.EmptyResultDataAccessException e) {
      // Ignore if the namespace does not exist; behaviour matches previous implementation
      return;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, NAMESPACE, id.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, NAMESPACE, id.toString());
    }
  }

  private Namespace toModel(NamespaceEntity entity) {
    return new Namespace(entity.getNamespaceId(), entity.getDataSourceId(), entity.getNames());
  }
}
