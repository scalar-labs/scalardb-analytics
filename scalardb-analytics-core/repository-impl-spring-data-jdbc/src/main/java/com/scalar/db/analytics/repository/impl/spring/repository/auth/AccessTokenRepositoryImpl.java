/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.auth;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.ACCESS_TOKEN;

import com.scalar.db.analytics.domain.auth.IssuedToken;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth.AccessTokenEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth.AccessTokenJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class AccessTokenRepositoryImpl
    implements AccessTokenRepository<SpringDataJdbcTransactionContext> {

  private final AccessTokenJdbcRepository springDataRepository;
  private final SpringDataJdbcErrorMapper errorMapper;

  public AccessTokenRepositoryImpl(
      AccessTokenJdbcRepository springDataRepository, SpringDataJdbcErrorMapper errorMapper) {
    this.springDataRepository = springDataRepository;
    this.errorMapper = errorMapper;
  }

  @Override
  public Optional<IssuedToken> findByUserIdAndToken(
      SpringDataJdbcTransactionContext ctx, UUID userId, String token) {
    try {
      return springDataRepository.findByUserIdAndToken(userId, token).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ACCESS_TOKEN, token);
    } catch (Exception e) {
      throw errorMapper.mapException(e, ACCESS_TOKEN, token);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void create(SpringDataJdbcTransactionContext ctx, IssuedToken token) {
    try {
      AccessTokenEntity entity =
          new AccessTokenEntity(token.token(), token.userId(), token.expiresAt());
      springDataRepository.insert(entity);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ACCESS_TOKEN, token.token());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ACCESS_TOKEN, token.token());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteByUserId(SpringDataJdbcTransactionContext ctx, UUID userId) {
    try {
      if (springDataRepository.existsById(userId)) {
        springDataRepository.deleteById(userId);
      }
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ACCESS_TOKEN, userId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ACCESS_TOKEN, userId.toString());
    }
  }

  private IssuedToken toModel(AccessTokenEntity entity) {
    return new IssuedToken(entity.getToken(), entity.getUserId(), entity.getExpiresAt());
  }
}
