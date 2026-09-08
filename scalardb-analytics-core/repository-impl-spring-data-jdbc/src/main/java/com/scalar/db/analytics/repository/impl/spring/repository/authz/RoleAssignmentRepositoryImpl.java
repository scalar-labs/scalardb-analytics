/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.authz;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.ROLE_ASSIGNMENT;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.RoleAssignmentEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.RoleAssignmentJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class RoleAssignmentRepositoryImpl
    implements RoleAssignmentRepository<SpringDataJdbcTransactionContext> {

  private final RoleAssignmentJdbcRepository springDataRepository;
  private final SpringDataJdbcErrorMapper errorMapper;

  public RoleAssignmentRepositoryImpl(
      RoleAssignmentJdbcRepository springDataRepository, SpringDataJdbcErrorMapper errorMapper) {
    this.springDataRepository = springDataRepository;
    this.errorMapper = errorMapper;
  }

  @Override
  public List<RoleAssignment> findByUserId(SpringDataJdbcTransactionContext ctx, UUID userId) {
    try {
      return springDataRepository.findByUserId(userId).stream().map(this::toModel).toList();
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ROLE_ASSIGNMENT, userId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ROLE_ASSIGNMENT, userId.toString());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void create(SpringDataJdbcTransactionContext ctx, RoleAssignment assignment) {
    try {
      boolean exists =
          springDataRepository
              .findByUserIdAndRoleId(assignment.userId(), assignment.roleId())
              .isPresent();
      if (exists) {
        throw new AnalyticsException(
            AnalyticsErrorCode.ROLE_ALREADY_ASSIGNED,
            Map.of(
                "user_id",
                assignment.userId().toString(),
                "role_id",
                assignment.roleId().toString()));
      }
      RoleAssignmentEntity entity =
          new RoleAssignmentEntity(assignment.userId(), assignment.roleId());
      springDataRepository.insert(entity);
    } catch (AnalyticsException e) {
      throw e;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(
          e, ROLE_ASSIGNMENT, assignment.userId() + ":" + assignment.roleId());
    } catch (Exception e) {
      throw errorMapper.mapException(
          e, ROLE_ASSIGNMENT, assignment.userId() + ":" + assignment.roleId());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void delete(SpringDataJdbcTransactionContext ctx, UUID userId, UUID roleId) {
    try {
      springDataRepository.deleteByUserIdAndRoleId(userId, roleId);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ROLE_ASSIGNMENT, userId + ":" + roleId);
    } catch (Exception e) {
      throw errorMapper.mapException(e, ROLE_ASSIGNMENT, userId + ":" + roleId);
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteByUserId(SpringDataJdbcTransactionContext ctx, UUID userId) {
    try {
      springDataRepository.deleteByUserId(userId);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ROLE_ASSIGNMENT, userId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ROLE_ASSIGNMENT, userId.toString());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteByRoleId(SpringDataJdbcTransactionContext ctx, UUID roleId) {
    try {
      springDataRepository.deleteByRoleId(roleId);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ROLE_ASSIGNMENT, roleId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ROLE_ASSIGNMENT, roleId.toString());
    }
  }

  @Override
  public List<RoleAssignment> findByRoleId(SpringDataJdbcTransactionContext ctx, UUID roleId) {
    try {
      return springDataRepository.findByRoleId(roleId).stream().map(this::toModel).toList();
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ROLE_ASSIGNMENT, roleId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ROLE_ASSIGNMENT, roleId.toString());
    }
  }

  private RoleAssignment toModel(RoleAssignmentEntity entity) {
    return new RoleAssignment(entity.getUserId(), entity.getRoleId());
  }
}
