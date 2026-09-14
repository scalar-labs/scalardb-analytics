/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.EffectivePermission;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.PermissionSource;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.repository.CatalogRepository;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PermissionUseCaseImplTest {
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID TARGET_USER_ID = UUID.randomUUID();
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final UUID ROLE_ID = UUID.randomUUID();

  @Mock private AccessControlEntryRepository<RepositoryTransactionContext> aceRepository;
  @Mock private RoleRepository<RepositoryTransactionContext> roleRepository;
  @Mock private RoleAssignmentRepository<RepositoryTransactionContext> roleAssignmentRepository;
  @Mock private CatalogRepository<RepositoryTransactionContext> catalogRepository;
  @Mock private DataSourceRepository<RepositoryTransactionContext> dataSourceRepository;
  @Mock private NamespaceRepository<RepositoryTransactionContext> namespaceRepository;
  @Mock private TableRepository<RepositoryTransactionContext> tableRepository;

  @Mock private AuthUserRepository<RepositoryTransactionContext> authUserRepository;

  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> txManager;
  @Mock private RepositoryTransactionContext txContext;
  @Mock private AuthorizationService authorizationService;

  private PermissionUseCaseImpl<RepositoryTransactionContext> permissionUseCase;

  @BeforeEach
  void setUp() throws Exception {
    permissionUseCase =
        new PermissionUseCaseImpl<>(
            aceRepository,
            roleRepository,
            roleAssignmentRepository,
            catalogRepository,
            dataSourceRepository,
            namespaceRepository,
            tableRepository,
            authUserRepository,
            txManager,
            authorizationService);

    lenient().when(txManager.single()).thenReturn(txContext);
    lenient()
        .when(txManager.withTransaction(any()))
        .thenAnswer(
            inv -> {
              var fn =
                  (com.scalar.db.analytics.lib.functional.ThrowableFunction<
                          RepositoryTransactionContext, ?, ?>)
                      inv.getArgument(0);
              return fn.apply(txContext);
            });
  }

  @Test
  void grantPermissionById_shouldCreateAce() throws Exception {
    when(authorizationService.authorizePermissionManagement(USER_ID, CATALOG_ID)).thenReturn(true);

    permissionUseCase.grantPermissionById(
        USER_ID, GranteeType.USER, TARGET_USER_ID, Permission.CATALOG_READ, CATALOG_ID);

    verify(aceRepository)
        .create(
            eq(txContext),
            eq(
                AccessControlEntry.create(
                    GranteeType.USER, TARGET_USER_ID, CATALOG_ID, Permission.CATALOG_READ)));
  }

  @Test
  void grantPermissionById_shouldThrowAccessDeniedWhenNotAuthorized() {
    when(authorizationService.authorizePermissionManagement(USER_ID, CATALOG_ID)).thenReturn(false);

    assertThatThrownBy(
            () ->
                permissionUseCase.grantPermissionById(
                    USER_ID, GranteeType.USER, TARGET_USER_ID, Permission.CATALOG_READ, CATALOG_ID))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void revokePermissionById_shouldDeleteAce() throws Exception {
    when(authorizationService.authorizePermissionManagement(USER_ID, CATALOG_ID)).thenReturn(true);

    permissionUseCase.revokePermissionById(
        USER_ID, GranteeType.USER, TARGET_USER_ID, Permission.CATALOG_READ, CATALOG_ID);

    verify(aceRepository)
        .delete(txContext, GranteeType.USER, TARGET_USER_ID, CATALOG_ID, Permission.CATALOG_READ);
  }

  @Test
  void listPermissionsById_selfQuery_shouldReturnDirectAndRolePermissions() throws Exception {
    // Direct ACEs
    AccessControlEntry directAce =
        AccessControlEntry.create(GranteeType.USER, USER_ID, CATALOG_ID, Permission.CATALOG_READ);
    when(aceRepository.findByGrantee(txContext, GranteeType.USER, USER_ID))
        .thenReturn(List.of(directAce));

    // Role-based ACEs
    Role role = new Role(ROLE_ID, "viewer", false);
    when(roleAssignmentRepository.findByUserId(txContext, USER_ID))
        .thenReturn(List.of(new RoleAssignment(USER_ID, ROLE_ID)));
    when(roleRepository.findById(txContext, ROLE_ID)).thenReturn(Optional.of(role));

    AccessControlEntry roleAce =
        AccessControlEntry.create(
            GranteeType.ROLE, ROLE_ID, CATALOG_ID, Permission.DATA_SOURCE_READ);
    when(aceRepository.findByGrantee(txContext, GranteeType.ROLE, ROLE_ID))
        .thenReturn(List.of(roleAce));

    List<EffectivePermission> result = permissionUseCase.listPermissionsById(USER_ID, USER_ID);

    assertThat(result).hasSize(2);
    assertThat(result.get(0))
        .isEqualTo(
            new EffectivePermission(
                Permission.CATALOG_READ, CATALOG_ID, PermissionSource.DIRECT, null, null));
    assertThat(result.get(1))
        .isEqualTo(
            new EffectivePermission(
                Permission.DATA_SOURCE_READ,
                CATALOG_ID,
                PermissionSource.VIA_ROLE,
                ROLE_ID,
                "viewer"));
  }

  @Test
  void listPermissionsById_otherUser_shouldRequireSuperAdmin() {
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> permissionUseCase.listPermissionsById(USER_ID, TARGET_USER_ID))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void listPermissionsById_otherUser_shouldSucceedForSuperAdmin() throws Exception {
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(true);
    when(aceRepository.findByGrantee(txContext, GranteeType.USER, TARGET_USER_ID))
        .thenReturn(List.of());
    when(roleAssignmentRepository.findByUserId(txContext, TARGET_USER_ID)).thenReturn(List.of());

    List<EffectivePermission> result =
        permissionUseCase.listPermissionsById(USER_ID, TARGET_USER_ID);

    assertThat(result).isEmpty();
  }

  @Test
  void listPermissionsByName_shouldResolveAndFetchInSingleTransaction() throws Exception {
    // The name → id resolution and the ACE/role-assignment fetches must run in one transaction
    // so a concurrent modification between them cannot race the result.
    String username = "alice";
    when(authUserRepository.findByUsername(txContext, username))
        .thenReturn(Optional.of(new AuthUser(TARGET_USER_ID, username)));
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(true);
    when(aceRepository.findByGrantee(txContext, GranteeType.USER, TARGET_USER_ID))
        .thenReturn(List.of());
    when(roleAssignmentRepository.findByUserId(txContext, TARGET_USER_ID)).thenReturn(List.of());

    List<EffectivePermission> result = permissionUseCase.listPermissions(USER_ID, username);

    assertThat(result).isEmpty();
    verify(txManager, times(1)).withTransaction(any());
  }

  @Test
  void listPermissionsForRoleByName_shouldResolveAndFetchInSingleTransaction() throws Exception {
    String roleName = "viewer";
    when(roleRepository.findByName(txContext, roleName))
        .thenReturn(Optional.of(new Role(ROLE_ID, roleName, false)));
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(true);
    when(aceRepository.findByGrantee(txContext, GranteeType.ROLE, ROLE_ID)).thenReturn(List.of());

    List<EffectivePermission> result = permissionUseCase.listPermissionsForRole(USER_ID, roleName);

    assertThat(result).isEmpty();
    verify(txManager, times(1)).withTransaction(any());
  }
}
