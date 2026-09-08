/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.authz;

import com.scalar.db.analytics.domain.authz.Role;
import java.util.List;
import java.util.UUID;

/** Use case for managing roles. All operations require SUPERADMIN. */
public interface RoleUseCase {
  Role createRole(UUID userId, String roleName);

  boolean deleteRole(UUID userId, String roleName);

  boolean deleteRoleById(UUID userId, UUID roleId);

  List<Role> listRoles(UUID userId);

  void grantRole(UUID userId, String roleName, String username);

  void grantRoleById(UUID userId, UUID roleId, UUID targetUserId);

  void revokeRole(UUID userId, String roleName, String username);

  void revokeRoleById(UUID userId, UUID roleId, UUID targetUserId);
}
