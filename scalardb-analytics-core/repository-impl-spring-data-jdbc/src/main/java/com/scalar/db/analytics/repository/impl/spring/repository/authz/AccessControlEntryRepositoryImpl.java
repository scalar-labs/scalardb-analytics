/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.authz;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.ACCESS_CONTROL_ENTRY;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.AccessControlEntryEntity;
import com.scalar.db.analytics.repository.impl.spring.repository.jdbc.authz.AccessControlEntryJdbcRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class AccessControlEntryRepositoryImpl
    implements AccessControlEntryRepository<SpringDataJdbcTransactionContext> {

  private final AccessControlEntryJdbcRepository springDataRepository;
  private final PermissionIdResolver permissionIdResolver;
  private final SpringDataJdbcErrorMapper errorMapper;

  public AccessControlEntryRepositoryImpl(
      AccessControlEntryJdbcRepository springDataRepository,
      PermissionIdResolver permissionIdResolver,
      SpringDataJdbcErrorMapper errorMapper) {
    this.springDataRepository = springDataRepository;
    this.permissionIdResolver = permissionIdResolver;
    this.errorMapper = errorMapper;
  }

  @Override
  public List<AccessControlEntry> findByGrantee(
      SpringDataJdbcTransactionContext ctx, GranteeType granteeType, UUID granteeId) {
    try {
      List<AccessControlEntryEntity> entities = springDataRepository.findByGranteeId(granteeId);
      List<AccessControlEntry> result = new ArrayList<>();
      for (AccessControlEntryEntity entity : entities) {
        verifyGranteeType(granteeType, entity);
        result.add(toModel(entity));
      }
      return result;
    } catch (AnalyticsException e) {
      throw e;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ACCESS_CONTROL_ENTRY, granteeId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ACCESS_CONTROL_ENTRY, granteeId.toString());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void create(SpringDataJdbcTransactionContext ctx, AccessControlEntry entry) {
    try {
      UUID permissionId = permissionIdResolver.resolveId(entry.permission());

      boolean duplicate =
          springDataRepository
              .findByGranteeAndPermissionAndResource(
                  entry.granteeId(), permissionId, entry.resourceId())
              .isPresent();
      if (duplicate) {
        throw new AnalyticsException(
            AnalyticsErrorCode.PERMISSION_ALREADY_GRANTED,
            Map.of(
                "role_name", entry.granteeId().toString(),
                "permission", entry.permission().toString(),
                "resource", entry.resourceId().toString()));
      }
      AccessControlEntryEntity entity =
          new AccessControlEntryEntity(
              entry.granteeType().name(), entry.granteeId(), entry.resourceId(), permissionId);
      springDataRepository.insert(entity);
    } catch (AnalyticsException e) {
      throw e;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ACCESS_CONTROL_ENTRY, entry.granteeId().toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ACCESS_CONTROL_ENTRY, entry.granteeId().toString());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void delete(
      SpringDataJdbcTransactionContext ctx,
      GranteeType granteeType,
      UUID granteeId,
      UUID resourceId,
      Permission permission) {
    try {
      UUID permissionId = permissionIdResolver.resolveId(permission);
      springDataRepository.deleteByGranteeAndPermissionAndResource(
          granteeType.name(), granteeId, permissionId, resourceId);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ACCESS_CONTROL_ENTRY, granteeId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ACCESS_CONTROL_ENTRY, granteeId.toString());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteByGrantee(
      SpringDataJdbcTransactionContext ctx, GranteeType granteeType, UUID granteeId) {
    try {
      List<AccessControlEntryEntity> entities = springDataRepository.findByGranteeId(granteeId);
      for (AccessControlEntryEntity entity : entities) {
        verifyGranteeType(granteeType, entity);
      }
      springDataRepository.deleteByGranteeId(granteeId);
    } catch (AnalyticsException e) {
      throw e;
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ACCESS_CONTROL_ENTRY, granteeId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ACCESS_CONTROL_ENTRY, granteeId.toString());
    }
  }

  @Override
  @Transactional(readOnly = false)
  public void deleteByResourceId(SpringDataJdbcTransactionContext ctx, UUID resourceId) {
    try {
      springDataRepository.deleteByResourceId(resourceId);
    } catch (DataAccessException e) {
      throw errorMapper.mapException(e, ACCESS_CONTROL_ENTRY, resourceId.toString());
    } catch (Exception e) {
      throw errorMapper.mapException(e, ACCESS_CONTROL_ENTRY, resourceId.toString());
    }
  }

  private void verifyGranteeType(GranteeType expected, AccessControlEntryEntity entity) {
    String actual = entity.getGranteeType();
    if (!expected.name().equals(actual)) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of("field_name", "grantee_type", "entity_name", entity.getGranteeId().toString()));
    }
  }

  private AccessControlEntry toModel(AccessControlEntryEntity entity) {
    Permission permission = permissionIdResolver.resolvePermission(entity.getPermissionId());
    return new AccessControlEntry(
        parseEnum(
            GranteeType.class, entity.getGranteeType(), "grantee_type", entity.getGranteeId()),
        entity.getGranteeId(),
        entity.getResourceId(),
        permission);
  }

  private <E extends Enum<E>> E parseEnum(
      Class<E> enumType, String value, String fieldName, UUID entityId) {
    try {
      return Enum.valueOf(enumType, value);
    } catch (IllegalArgumentException e) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of("field_name", fieldName, "entity_name", entityId.toString()),
          e);
    }
  }
}
