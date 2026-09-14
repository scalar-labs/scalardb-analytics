/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.auth;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.PASSWORD_IDENTITY;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth.PasswordIdentityEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth.PasswordIdentityJdbcRepository;
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
public class PasswordIdentityRepositoryImpl
    implements PasswordIdentityRepository<SpringDataJdbcTransactionContext> {

  private final PasswordIdentityJdbcRepository springDataRepository;
  private final SpringDataJdbcErrorMapper errorMapper;

  public PasswordIdentityRepositoryImpl(
      PasswordIdentityJdbcRepository springDataRepository, SpringDataJdbcErrorMapper errorMapper) {
    this.springDataRepository = springDataRepository;
    this.errorMapper = errorMapper;
  }

  @Override
  public Optional<PasswordIdentity> findByBackendAndBackendUserId(
      SpringDataJdbcTransactionContext ctx, PasswordBackendType backend, String backendUserId) {
    String errorKey = backend + ":" + backendUserId;
    try {
      return springDataRepository
          .findByBackendAndBackendUserId(backend, backendUserId)
          .map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, errorKey);
    } catch (Exception e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, errorKey);
    }
  }

  @Override
  public List<PasswordIdentity> findByUserId(SpringDataJdbcTransactionContext ctx, UUID userId) {
    try {
      return springDataRepository.findByUserId(userId).stream().map(this::toModel).toList();
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, userId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, userId.toString());
    }
  }

  @Override
  public List<PasswordIdentity> findByBackend(
      SpringDataJdbcTransactionContext ctx, PasswordBackendType backend) {
    try {
      return springDataRepository.findByBackend(backend).stream().map(this::toModel).toList();
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, backend.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, backend.toString());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void create(SpringDataJdbcTransactionContext ctx, PasswordIdentity identity) {
    try {
      boolean backendExists =
          springDataRepository.findByUserId(identity.userId()).stream()
              .anyMatch(e -> e.getBackend().equals(identity.backend()));
      if (backendExists) {
        throw new AnalyticsException(
            AnalyticsErrorCode.USER_ALREADY_EXISTS,
            Map.of("user_id", identity.userId().toString()));
      }
      PasswordIdentityEntity entity =
          new PasswordIdentityEntity(
              identity.identityId(),
              identity.userId(),
              identity.backend(),
              identity.backendUserId());
      springDataRepository.insert(entity);
    } catch (AnalyticsException e) {
      throw e;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, identity.identityId().toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, identity.identityId().toString());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteByUserId(SpringDataJdbcTransactionContext ctx, UUID userId) {
    try {
      springDataRepository.deleteByUserId(userId);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, userId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, userId.toString());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteByUserIdAndBackend(
      SpringDataJdbcTransactionContext ctx, UUID userId, PasswordBackendType backend) {
    String errorKey = userId + ":" + backend;
    try {
      springDataRepository.deleteByUserIdAndBackend(userId, backend);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, errorKey);
    } catch (Exception e) {
      throw errorMapper.mapException(e, PASSWORD_IDENTITY, errorKey);
    }
  }

  private PasswordIdentity toModel(PasswordIdentityEntity entity) {
    return new PasswordIdentity(
        entity.getIdentityId(), entity.getUserId(), entity.getBackend(), entity.getBackendUserId());
  }
}
