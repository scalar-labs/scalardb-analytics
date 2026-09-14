/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.authz;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class RoleUseCaseImpl<T extends RepositoryTransactionContext> implements RoleUseCase {

  private final RoleRepository<T> roleRepository;
  private final RoleAssignmentRepository<T> roleAssignmentRepository;
  private final AuthUserRepository<T> authUserRepository;
  private final RepositoryTransactionManager<T> txManager;
  private final AuthorizationService authorizationService;

  public RoleUseCaseImpl(
      RoleRepository<T> roleRepository,
      RoleAssignmentRepository<T> roleAssignmentRepository,
      AuthUserRepository<T> authUserRepository,
      RepositoryTransactionManager<T> txManager,
      AuthorizationService authorizationService) {
    this.roleRepository = roleRepository;
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.authUserRepository = authUserRepository;
    this.txManager = txManager;
    this.authorizationService = authorizationService;
  }

  @Override
  public Role createRole(UUID userId, String roleName) {
    requireSuperAdmin(userId);
    Role role = Role.create(roleName);
    roleRepository.create(txManager.single(), role);
    return role;
  }

  @Override
  public boolean deleteRole(UUID userId, String roleName) {
    requireSuperAdmin(userId);
    return txManager.withTransaction(
        ctx -> {
          Optional<Role> role = roleRepository.findByName(ctx, roleName);
          if (role.isEmpty()) {
            return false;
          }
          return doDeleteRole(ctx, role.get());
        });
  }

  @Override
  public boolean deleteRoleById(UUID userId, UUID roleId) {
    requireSuperAdmin(userId);
    return txManager.withTransaction(
        ctx -> {
          Optional<Role> role = roleRepository.findById(ctx, roleId);
          if (role.isEmpty()) {
            return false;
          }
          return doDeleteRole(ctx, role.get());
        });
  }

  @Override
  public List<Role> listRoles(UUID userId) {
    requireSuperAdmin(userId);
    return roleRepository.findAll(txManager.single());
  }

  @Override
  public void grantRole(UUID userId, String roleName, String username) {
    requireSuperAdmin(userId);
    txManager.withTransaction(
        ctx -> {
          UUID roleId = resolveRoleId(ctx, roleName);
          UUID targetUserId = resolveUserId(ctx, username);
          try {
            roleAssignmentRepository.create(ctx, new RoleAssignment(targetUserId, roleId));
          } catch (AnalyticsException e) {
            // Enrich with the human-readable role name, which only the by-name path knows. The
            // repository layer deals in ids and reports role_id; here we have the name the caller
            // supplied. (A proper fix is the role-assignment aggregate redesign — see issue.)
            if (e.getErrorCode() == AnalyticsErrorCode.ROLE_ALREADY_ASSIGNED) {
              throw new AnalyticsException(
                  AnalyticsErrorCode.ROLE_ALREADY_ASSIGNED,
                  Map.of(
                      "user_id", targetUserId.toString(),
                      "username", username,
                      "role_name", roleName),
                  e);
            }
            throw e;
          }
          return null;
        });
  }

  @Override
  public void grantRoleById(UUID userId, UUID roleId, UUID targetUserId) {
    requireSuperAdmin(userId);
    txManager.withTransaction(
        ctx -> {
          requireRoleExists(ctx, roleId);
          roleAssignmentRepository.create(ctx, new RoleAssignment(targetUserId, roleId));
          return null;
        });
  }

  @Override
  public void revokeRole(UUID userId, String roleName, String username) {
    requireSuperAdmin(userId);
    txManager.withTransaction(
        ctx -> {
          UUID roleId = resolveRoleId(ctx, roleName);
          UUID targetUserId = resolveUserId(ctx, username);
          roleAssignmentRepository.delete(ctx, targetUserId, roleId);
          return null;
        });
  }

  @Override
  public void revokeRoleById(UUID userId, UUID roleId, UUID targetUserId) {
    requireSuperAdmin(userId);
    txManager.withTransaction(
        ctx -> {
          requireRoleExists(ctx, roleId);
          roleAssignmentRepository.delete(ctx, targetUserId, roleId);
          return null;
        });
  }

  private boolean doDeleteRole(T ctx, Role role) {
    if (role.builtIn()) {
      throw new AnalyticsException(
          AnalyticsErrorCode.BUILT_IN_ENTITY_NOT_MODIFIABLE, Map.of("entity_name", role.name()));
    }
    roleAssignmentRepository.deleteByRoleId(ctx, role.id());
    roleRepository.deleteById(ctx, role.id());
    return true;
  }

  private UUID resolveRoleId(T ctx, String roleName) {
    return roleRepository
        .findByName(ctx, roleName)
        .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName))
        .id();
  }

  private UUID resolveUserId(T ctx, String username) {
    return authUserRepository
        .findByUsername(ctx, username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username))
        .getUserId();
  }

  private void requireRoleExists(T ctx, UUID roleId) {
    if (roleRepository.findById(ctx, roleId).isEmpty()) {
      throw new IllegalArgumentException("Role not found: " + roleId);
    }
  }

  private void requireSuperAdmin(UUID userId) {
    if (!authorizationService.authorizeSuperAdmin(userId)) {
      // TODO(#460): include the caller's username here once AuthenticationInterceptor threads an
      // AuthUser (username) downstream, instead of surfacing only the UUID.
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED, Map.of("user_id", userId.toString()));
    }
  }
}
