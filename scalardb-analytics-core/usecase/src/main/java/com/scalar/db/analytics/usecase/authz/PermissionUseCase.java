/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.authz;

import com.scalar.db.analytics.domain.authz.EffectivePermission;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import java.util.List;
import java.util.UUID;

/** Use case for managing permissions (access control entries). */
public interface PermissionUseCase {

  // --- Grant by name (per resource type) ---

  void grantCatalogPermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName);

  void grantDataSourcePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName);

  void grantNamespacePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames);

  void grantTablePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName);

  // --- Grant by ID ---

  void grantPermissionById(
      UUID userId, GranteeType granteeType, UUID granteeId, Permission permission, UUID resourceId);

  // --- Revoke by name (per resource type) ---

  void revokeCatalogPermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName);

  void revokeDataSourcePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName);

  void revokeNamespacePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames);

  void revokeTablePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName);

  // --- Revoke by ID ---

  void revokePermissionById(
      UUID userId, GranteeType granteeType, UUID granteeId, Permission permission, UUID resourceId);

  // --- List ---

  List<EffectivePermission> listPermissions(UUID userId, String username);

  List<EffectivePermission> listPermissionsById(UUID userId, UUID targetUserId);

  List<EffectivePermission> listPermissionsForRole(UUID userId, String roleName);

  List<EffectivePermission> listPermissionsForRoleById(UUID userId, UUID roleId);
}
