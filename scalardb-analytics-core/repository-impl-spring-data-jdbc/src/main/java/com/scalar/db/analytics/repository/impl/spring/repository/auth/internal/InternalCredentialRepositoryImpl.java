/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.auth.internal;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.INTERNAL_CREDENTIAL;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.internal.InternalCredential;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth.internal.InternalCredentialEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth.internal.InternalCredentialJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.Map;
import java.util.Optional;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class InternalCredentialRepositoryImpl
    implements InternalCredentialRepository<SpringDataJdbcTransactionContext> {

  private final InternalCredentialJdbcRepository springDataRepository;
  private final SpringDataJdbcErrorMapper errorMapper;

  public InternalCredentialRepositoryImpl(
      InternalCredentialJdbcRepository springDataRepository,
      SpringDataJdbcErrorMapper errorMapper) {
    this.springDataRepository = springDataRepository;
    this.errorMapper = errorMapper;
  }

  @Override
  @Transactional(readOnly = false)
  public void create(SpringDataJdbcTransactionContext ctx, InternalCredential credential) {
    try {
      boolean exists = springDataRepository.findByUsername(credential.username()).isPresent();
      if (exists) {
        throw new AnalyticsException(
            AnalyticsErrorCode.USER_ALREADY_EXISTS, Map.of("username", credential.username()));
      }
      InternalCredentialEntity entity =
          new InternalCredentialEntity(credential.username(), credential.passwordHash());
      springDataRepository.insert(entity);
    } catch (AnalyticsException e) {
      throw e;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, INTERNAL_CREDENTIAL, credential.username());
    } catch (Exception e) {
      throw errorMapper.mapException(e, INTERNAL_CREDENTIAL, credential.username());
    }
  }

  @Override
  public Optional<InternalCredential> findByUsername(
      SpringDataJdbcTransactionContext ctx, String username) {
    try {
      return springDataRepository.findByUsername(username).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, INTERNAL_CREDENTIAL, username);
    } catch (Exception e) {
      throw errorMapper.mapException(e, INTERNAL_CREDENTIAL, username);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteByUsername(SpringDataJdbcTransactionContext ctx, String username) {
    try {
      springDataRepository.deleteByUsername(username);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, INTERNAL_CREDENTIAL, username);
    } catch (Exception e) {
      throw errorMapper.mapException(e, INTERNAL_CREDENTIAL, username);
    }
  }

  private InternalCredential toModel(InternalCredentialEntity entity) {
    return new InternalCredential(entity.getUsername(), entity.getPasswordHash());
  }
}
