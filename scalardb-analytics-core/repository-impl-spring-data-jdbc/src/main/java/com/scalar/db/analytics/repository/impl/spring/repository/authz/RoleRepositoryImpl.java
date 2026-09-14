/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.authz;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.ROLE;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.RoleEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.RoleJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class RoleRepositoryImpl implements RoleRepository<SpringDataJdbcTransactionContext> {

  private final RoleJdbcRepository springDataRepository;
  private final SpringDataJdbcErrorMapper errorMapper;

  public RoleRepositoryImpl(
      RoleJdbcRepository springDataRepository, SpringDataJdbcErrorMapper errorMapper) {
    this.springDataRepository = springDataRepository;
    this.errorMapper = errorMapper;
  }

  @Override
  public Optional<Role> findById(SpringDataJdbcTransactionContext ctx, UUID roleId) {
    try {
      return springDataRepository.findById(roleId).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ROLE, roleId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ROLE, roleId.toString());
    }
  }

  @Override
  public Optional<Role> findByName(SpringDataJdbcTransactionContext ctx, String name) {
    try {
      return springDataRepository.findByName(name).map(this::toModel);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ROLE, name);
    } catch (Exception e) {
      throw errorMapper.mapException(e, ROLE, name);
    }
  }

  @Override
  public List<Role> findAll(SpringDataJdbcTransactionContext ctx) {
    try {
      // ScalarDB does not support cross-partition scan, so we query by built_in index
      // (true and false) and merge the results.
      List<Role> roles = new ArrayList<>();
      springDataRepository.findByBuiltInTrue().forEach(e -> roles.add(toModel(e)));
      springDataRepository.findByBuiltInFalse().forEach(e -> roles.add(toModel(e)));
      return roles;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ROLE, null);
    } catch (Exception e) {
      throw errorMapper.mapException(e, ROLE, null);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void create(SpringDataJdbcTransactionContext ctx, Role role) {
    try {
      Optional<RoleEntity> existing = springDataRepository.findByName(role.name());
      if (existing.isPresent()) {
        throw new AnalyticsException(
            AnalyticsErrorCode.ROLE_ALREADY_EXISTS, Map.of("role_name", role.name()));
      }
      RoleEntity entity = new RoleEntity(role.id(), role.name(), role.builtIn());
      springDataRepository.insert(entity);
    } catch (AnalyticsException e) {
      throw e;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ROLE, role.id().toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ROLE, role.id().toString());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteById(SpringDataJdbcTransactionContext ctx, UUID roleId) {
    try {
      // Check if exists before deleting to avoid database-specific errors
      if (springDataRepository.existsById(roleId)) {
        springDataRepository.deleteById(roleId);
      }
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ROLE, roleId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ROLE, roleId.toString());
    }
  }

  private Role toModel(RoleEntity entity) {
    return new Role(entity.getId(), entity.getName(), entity.isBuiltIn());
  }
}
