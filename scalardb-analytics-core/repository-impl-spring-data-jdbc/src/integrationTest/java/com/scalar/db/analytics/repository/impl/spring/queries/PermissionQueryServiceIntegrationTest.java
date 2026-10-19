/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.queries;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceNames;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.repository.PermissionQueryService.GrantedPermission;
import com.scalar.db.analytics.repository.impl.spring.query.PermissionQueryServiceImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.auth.AuthUserRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.authz.AccessControlEntryRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.authz.RoleAssignmentRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.authz.RoleRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PermissionQueryServiceIntegrationTest extends AbstractScalarDbIntegrationTest {

  private static final ResourceNames CATALOG_NAMES =
      new ResourceNames("cat", null, List.of(), null);
  private static final ResourceNames DATA_SOURCE_NAMES =
      new ResourceNames("cat", "ds", List.of(), null);
  private static final ResourceNames NAMESPACE_NAMES =
      new ResourceNames("cat", "ds", List.of("db", "public"), null);
  private static final ResourceNames TABLE_NAMES =
      new ResourceNames("cat", "ds", List.of("db", "public"), "orders");

  @Autowired private PermissionQueryServiceImpl permissionQueryService;
  @Autowired private AccessControlEntryRepositoryImpl aceRepository;
  @Autowired private AuthUserRepositoryImpl authUserRepository;
  @Autowired private RoleRepositoryImpl roleRepository;
  @Autowired private RoleAssignmentRepositoryImpl roleAssignmentRepository;

  private UUID catalogId;
  private UUID dataSourceId;
  private UUID namespaceId;
  private UUID tableId;
  private UUID userId;
  private Role readers;
  private Role admins;

  @BeforeEach
  void setUp() {
    catalogId = createCatalog("cat");
    dataSourceId = createPostgresDataSource(catalogId, "ds");
    namespaceId = createNamespace(dataSourceId, List.of("db", "public"));
    tableId = createTableWithDefaultColumns(namespaceId, "orders");
    // Resources the grantees have no permission on, which must not leak into the results.
    UUID otherDataSourceId = createPostgresDataSource(createCatalog("other-cat"), "other-ds");
    createTableWithDefaultColumns(
        createNamespace(otherDataSourceId, List.of("other-db", "other-schema")), "other-table");

    AuthUser user = AuthUser.create("alice");
    authUserRepository.create(ctx, user);
    userId = user.getUserId();
    readers = Role.create("readers");
    roleRepository.create(ctx, readers);
    admins = Role.create("admins");
    roleRepository.create(ctx, admins);
    roleAssignmentRepository.create(ctx, new RoleAssignment(userId, readers.id()));
    roleAssignmentRepository.create(ctx, new RoleAssignment(userId, admins.id()));
  }

  @Test
  void listByGrantee_forUserAndRoles_shouldResolveNamesAtEveryResourceLevel() {
    grant(GranteeType.USER, userId, catalogId, Permission.CATALOG_READ);
    grant(GranteeType.USER, userId, dataSourceId, Permission.DATA_SOURCE_READ);
    grant(GranteeType.USER, userId, namespaceId, Permission.NAMESPACE_READ);
    grant(GranteeType.USER, userId, tableId, Permission.TABLE_READ);
    grant(GranteeType.ROLE, readers.id(), catalogId, Permission.CATALOG_READ);
    grant(GranteeType.ROLE, readers.id(), namespaceId, Permission.NAMESPACE_READ);
    grant(GranteeType.ROLE, readers.id(), tableId, Permission.TABLE_READ);
    grant(GranteeType.ROLE, admins.id(), catalogId, Permission.CATALOG_ADMIN);
    grant(GranteeType.ROLE, admins.id(), catalogId, Permission.CATALOG_WRITE);
    grant(GranteeType.ROLE, admins.id(), dataSourceId, Permission.DATA_SOURCE_ADMIN);

    assertThat(permissionQueryService.listByGrantee(ctx, GranteeType.USER, userId))
        .containsExactlyElementsOf(
            withNames(
                GranteeType.USER,
                userId,
                Map.of(
                    catalogId, CATALOG_NAMES,
                    dataSourceId, DATA_SOURCE_NAMES,
                    namespaceId, NAMESPACE_NAMES,
                    tableId, TABLE_NAMES)))
        .hasSize(4);
    assertThat(permissionQueryService.listByGrantee(ctx, GranteeType.ROLE, readers.id()))
        .containsExactlyElementsOf(
            withNames(
                GranteeType.ROLE,
                readers.id(),
                Map.of(
                    catalogId, CATALOG_NAMES,
                    namespaceId, NAMESPACE_NAMES,
                    tableId, TABLE_NAMES)))
        .hasSize(3);
    assertThat(permissionQueryService.listByGrantee(ctx, GranteeType.ROLE, admins.id()))
        .containsExactlyElementsOf(
            withNames(
                GranteeType.ROLE,
                admins.id(),
                Map.of(catalogId, CATALOG_NAMES, dataSourceId, DATA_SOURCE_NAMES)))
        .hasSize(3);
  }

  @Test
  void listByGrantee_whenResourceIsMissing_shouldReturnPermissionWithoutNames() {
    UUID missingTableId = UUID.randomUUID();
    UUID missingCatalogId = UUID.randomUUID();
    grant(GranteeType.USER, userId, tableId, Permission.TABLE_READ);
    grant(GranteeType.USER, userId, missingTableId, Permission.TABLE_READ);
    grant(GranteeType.USER, userId, missingCatalogId, Permission.CATALOG_READ);

    List<GrantedPermission> result =
        permissionQueryService.listByGrantee(ctx, GranteeType.USER, userId);

    assertThat(result)
        .containsExactlyElementsOf(
            withNames(GranteeType.USER, userId, Map.of(tableId, TABLE_NAMES)))
        .hasSize(3)
        .contains(
            new GrantedPermission(Permission.TABLE_READ, missingTableId, null),
            new GrantedPermission(Permission.CATALOG_READ, missingCatalogId, null));
  }

  @Test
  void listByGrantee_whenParentIsMissing_shouldReturnPermissionWithoutNames() {
    // A table whose namespace no longer exists.
    UUID orphanTableId = createTableWithDefaultColumns(UUID.randomUUID(), "orphan");
    grant(GranteeType.USER, userId, orphanTableId, Permission.TABLE_READ);

    assertThat(permissionQueryService.listByGrantee(ctx, GranteeType.USER, userId))
        .containsExactly(new GrantedPermission(Permission.TABLE_READ, orphanTableId, null));
  }

  @Test
  void listByGrantee_withoutPermissions_shouldReturnEmptyList() {
    assertThat(permissionQueryService.listByGrantee(ctx, GranteeType.USER, userId)).isEmpty();
  }

  @Test
  void listRolesByUserId_shouldReturnAssignedRolesWithNames() {
    assertThat(permissionQueryService.listRolesByUserId(ctx, userId))
        .containsExactlyInAnyOrder(readers, admins);
  }

  @Test
  void listRolesByUserId_withoutAssignments_shouldReturnEmptyList() {
    assertThat(permissionQueryService.listRolesByUserId(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void listRolesByUserId_whenRoleIsMissing_shouldThrowDataInconsistency() {
    UUID missingRoleId = UUID.randomUUID();
    roleAssignmentRepository.create(ctx, new RoleAssignment(userId, missingRoleId));

    assertThatThrownBy(() -> permissionQueryService.listRolesByUserId(ctx, userId))
        .isInstanceOf(AnalyticsException.class)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.DATA_INCONSISTENCY);
  }

  private void grant(
      GranteeType granteeType, UUID granteeId, UUID resourceId, Permission permission) {
    aceRepository.create(
        ctx, AccessControlEntry.create(granteeType, granteeId, resourceId, permission));
  }

  /**
   * Returns the grantee's entries, in the order in which they are stored, with the given names by
   * resource ID.
   */
  private List<GrantedPermission> withNames(
      GranteeType granteeType, UUID granteeId, Map<UUID, ResourceNames> namesByResourceId) {
    return aceRepository.findByGrantee(ctx, granteeType, granteeId).stream()
        .map(
            ace ->
                new GrantedPermission(
                    ace.permission(), ace.resourceId(), namesByResourceId.get(ace.resourceId())))
        .collect(Collectors.toCollection(ArrayList::new));
  }
}
