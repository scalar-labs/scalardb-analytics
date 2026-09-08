/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.authz;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.EffectivePermission;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.PermissionSource;
import com.scalar.db.analytics.server.support.UseCaseIntegrationTestBase;
import com.scalar.db.analytics.usecase.CatalogUseCase;
import com.scalar.db.analytics.usecase.auth.UserUseCase;
import com.scalar.db.analytics.usecase.authz.PermissionUseCase;
import com.scalar.db.analytics.usecase.authz.RoleUseCase;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PermissionUseCaseIntegrationTest extends UseCaseIntegrationTestBase {

  @Autowired private PermissionUseCase permissionUseCase;
  @Autowired private CatalogUseCase catalogUseCase;
  @Autowired private UserUseCase userUseCase;
  @Autowired private RoleUseCase roleUseCase;

  private UUID adminId;
  private UUID catalogId;
  private UUID aliceUserId;

  @BeforeEach
  void setUp() {
    adminId = adminUserId();
    var catalog = catalogUseCase.createCatalog(adminId, "perm-test-catalog");
    catalogId = catalog.getId();
    aliceUserId =
        userUseCase
            .createUserWithBackendUser(adminId, "alice-perm", "alice-perm", "alice-pass")
            .getUserId();
  }

  @AfterEach
  void tearDown() {
    try {
      permissionUseCase.revokeCatalogPermission(
          adminId, GranteeType.USER, "alice-perm", Permission.CATALOG_READ, "perm-test-catalog");
    } catch (AnalyticsException ignored) {
    }
    try {
      roleUseCase.deleteRole(adminId, "perm-test-role");
    } catch (AnalyticsException ignored) {
    }
    try {
      userUseCase.deleteUser(adminId, "alice-perm", true);
    } catch (AnalyticsException ignored) {
    }
    try {
      catalogUseCase.deleteCatalog(adminId, "perm-test-catalog", true);
    } catch (AnalyticsException ignored) {
    }
  }

  @Test
  void grantAndRevokeCatalogPermission_byName_shouldWork() {
    permissionUseCase.grantCatalogPermission(
        adminId, GranteeType.USER, "alice-perm", Permission.CATALOG_READ, "perm-test-catalog");

    List<EffectivePermission> perms = permissionUseCase.listPermissions(adminId, "alice-perm");
    assertThat(perms)
        .anyMatch(
            p ->
                p.permission() == Permission.CATALOG_READ && p.source() == PermissionSource.DIRECT);

    permissionUseCase.revokeCatalogPermission(
        adminId, GranteeType.USER, "alice-perm", Permission.CATALOG_READ, "perm-test-catalog");

    perms = permissionUseCase.listPermissions(adminId, "alice-perm");
    assertThat(perms)
        .noneMatch(
            p -> p.permission() == Permission.CATALOG_READ && p.resourceId().equals(catalogId));
  }

  @Test
  void grantPermissionById_shouldWork() {
    permissionUseCase.grantPermissionById(
        adminId, GranteeType.USER, aliceUserId, Permission.CATALOG_READ, catalogId);

    List<EffectivePermission> perms = permissionUseCase.listPermissionsById(adminId, aliceUserId);
    assertThat(perms)
        .anyMatch(
            p -> p.permission() == Permission.CATALOG_READ && p.resourceId().equals(catalogId));

    permissionUseCase.revokePermissionById(
        adminId, GranteeType.USER, aliceUserId, Permission.CATALOG_READ, catalogId);
  }

  @Test
  void roleBasedPermission_shouldShowViaRoleSource() {
    roleUseCase.createRole(adminId, "perm-test-role");
    permissionUseCase.grantCatalogPermission(
        adminId, GranteeType.ROLE, "perm-test-role", Permission.CATALOG_READ, "perm-test-catalog");
    roleUseCase.grantRole(adminId, "perm-test-role", "alice-perm");

    List<EffectivePermission> perms = permissionUseCase.listPermissions(adminId, "alice-perm");
    assertThat(perms)
        .anyMatch(
            p ->
                p.permission() == Permission.CATALOG_READ
                    && p.source() == PermissionSource.VIA_ROLE);

    roleUseCase.revokeRole(adminId, "perm-test-role", "alice-perm");
    roleUseCase.deleteRole(adminId, "perm-test-role");
  }
}
