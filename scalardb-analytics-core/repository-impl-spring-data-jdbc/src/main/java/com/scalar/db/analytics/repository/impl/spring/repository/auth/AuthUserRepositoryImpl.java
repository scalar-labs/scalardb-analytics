/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.auth;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.USER;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth.UserEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.auth.UserJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class AuthUserRepositoryImpl
    implements AuthUserRepository<SpringDataJdbcTransactionContext> {

  private final UserJdbcRepository springDataRepository;
  private final SpringDataJdbcErrorMapper errorMapper;

  public AuthUserRepositoryImpl(
      UserJdbcRepository springDataRepository, SpringDataJdbcErrorMapper errorMapper) {
    this.springDataRepository = springDataRepository;
    this.errorMapper = errorMapper;
  }

  @Override
  public Optional<AuthUser> findById(SpringDataJdbcTransactionContext ctx, UUID userId) {
    try {
      return springDataRepository.findById(userId).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, USER, userId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, USER, userId.toString());
    }
  }

  @Override
  public List<AuthUser> findAll(SpringDataJdbcTransactionContext ctx) {
    try {
      List<AuthUser> users = new ArrayList<>();
      springDataRepository.findAll().forEach(e -> users.add(toModel(e)));
      return users;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, USER, null);
    } catch (Exception e) {
      throw errorMapper.mapException(e, USER, null);
    }
  }

  @Override
  public Optional<AuthUser> findByUsername(SpringDataJdbcTransactionContext ctx, String username) {
    try {
      return springDataRepository.findByUsername(username).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, USER, username);
    } catch (Exception e) {
      throw errorMapper.mapException(e, USER, username);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void create(SpringDataJdbcTransactionContext ctx, AuthUser user) {
    try {
      UserEntity entity = new UserEntity(user.getUserId(), user.getUsername());
      springDataRepository.insert(entity);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, USER, user.getUserId().toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, USER, user.getUserId().toString());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteById(SpringDataJdbcTransactionContext ctx, UUID userId) {
    try {
      if (springDataRepository.existsById(userId)) {
        springDataRepository.deleteById(userId);
      }
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, USER, userId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, USER, userId.toString());
    }
  }

  private AuthUser toModel(UserEntity entity) {
    return new AuthUser(entity.getUserId(), entity.getUsername());
  }
}
