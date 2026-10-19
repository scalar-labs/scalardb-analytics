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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.EffectivePermission;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.PermissionSource;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.ResourceType;
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
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
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

  @Nested
  class GrantOnDelegatedResource {
    private final DataSource scalarDbDataSource =
        new DataSource(
            UUID.randomUUID(),
            CATALOG_ID,
            "scalardb_ds",
            ScalarDbProvider.builder()
                .configs(Collections.singletonMap("scalar.db.storage", "cassandra"))
                .build());
    private final Namespace namespace =
        new Namespace(UUID.randomUUID(), scalarDbDataSource.getId(), List.of("ns"));
    private final Table table =
        new Table(new TableInfo(UUID.randomUUID(), namespace.getId(), "tbl"));

    @BeforeEach
    void setUpResources() throws Exception {
      lenient()
          .when(namespaceRepository.findById(txContext, namespace.getId()))
          .thenReturn(Optional.of(namespace));
      lenient()
          .when(tableRepository.findById(txContext, table.getInfo().getId()))
          .thenReturn(Optional.of(table));
      lenient()
          .when(dataSourceRepository.findById(txContext, scalarDbDataSource.getId()))
          .thenReturn(Optional.of(scalarDbDataSource));
    }

    @Test
    void grantNamespacePermissionById_shouldRejectWithoutCreatingAce() throws Exception {
      when(authorizationService.authorizePermissionManagement(USER_ID, CATALOG_ID))
          .thenReturn(true);
      when(authorizationService.isAclDelegated(
              new ResourceRef(ResourceType.NAMESPACE, namespace.getId(), ScalarDbProvider.TYPE)))
          .thenReturn(true);

      assertThatThrownBy(
              () ->
                  permissionUseCase.grantPermissionById(
                      USER_ID,
                      GranteeType.USER,
                      TARGET_USER_ID,
                      Permission.NAMESPACE_READ,
                      namespace.getId()))
          .isInstanceOfSatisfying(
              AnalyticsException.class,
              e -> {
                assertThat(e.getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.RESOURCE_UNDER_ACL_DELEGATION);
                assertThat(e.getMetadata())
                    .containsEntry("permission", "NAMESPACE_READ")
                    .containsEntry("resource_id", namespace.getId().toString())
                    .containsEntry("data_source_name", "scalardb_ds");
              });
      verify(aceRepository, never()).create(any(), any());
    }

    @Test
    void grantTablePermissionById_shouldRejectWithoutCreatingAce() throws Exception {
      when(authorizationService.authorizePermissionManagement(USER_ID, CATALOG_ID))
          .thenReturn(true);
      when(authorizationService.isAclDelegated(
              new ResourceRef(ResourceType.TABLE, table.getInfo().getId(), ScalarDbProvider.TYPE)))
          .thenReturn(true);

      assertThatThrownBy(
              () ->
                  permissionUseCase.grantPermissionById(
                      USER_ID,
                      GranteeType.ROLE,
                      ROLE_ID,
                      Permission.TABLE_READ,
                      table.getInfo().getId()))
          .isInstanceOfSatisfying(
              AnalyticsException.class,
              e ->
                  assertThat(e.getErrorCode())
                      .isEqualTo(AnalyticsErrorCode.RESOURCE_UNDER_ACL_DELEGATION));
      verify(aceRepository, never()).create(any(), any());
    }

    @Test
    void grantNamespacePermissionByName_shouldRejectWithoutCreatingAce() throws Exception {
      when(authUserRepository.findByUsername(txContext, "alice"))
          .thenReturn(Optional.of(new AuthUser(TARGET_USER_ID, "alice")));
      when(catalogRepository.findByName(txContext, "catalog"))
          .thenReturn(Optional.of(Catalog.of(CATALOG_ID, "catalog")));
      when(dataSourceRepository.findByCatalogIdAndName(txContext, CATALOG_ID, "scalardb_ds"))
          .thenReturn(Optional.of(scalarDbDataSource));
      when(namespaceRepository.findByDataSourceIdAndNames(
              txContext, scalarDbDataSource.getId(), List.of("ns")))
          .thenReturn(Optional.of(namespace));
      when(authorizationService.authorizePermissionManagement(USER_ID, CATALOG_ID))
          .thenReturn(true);
      when(authorizationService.isAclDelegated(any())).thenReturn(true);

      assertThatThrownBy(
              () ->
                  permissionUseCase.grantNamespacePermission(
                      USER_ID,
                      GranteeType.USER,
                      "alice",
                      Permission.NAMESPACE_READ,
                      "catalog",
                      "scalardb_ds",
                      List.of("ns")))
          .isInstanceOfSatisfying(
              AnalyticsException.class,
              e ->
                  assertThat(e.getErrorCode())
                      .isEqualTo(AnalyticsErrorCode.RESOURCE_UNDER_ACL_DELEGATION));
      verify(aceRepository, never()).create(any(), any());
    }

    @Test
    void grantNamespacePermission_whenNotDelegated_shouldCreateAce() throws Exception {
      when(authorizationService.authorizePermissionManagement(USER_ID, CATALOG_ID))
          .thenReturn(true);
      when(authorizationService.isAclDelegated(any())).thenReturn(false);

      permissionUseCase.grantPermissionById(
          USER_ID, GranteeType.USER, TARGET_USER_ID, Permission.NAMESPACE_READ, namespace.getId());

      verify(aceRepository)
          .create(
              txContext,
              AccessControlEntry.create(
                  GranteeType.USER, TARGET_USER_ID, namespace.getId(), Permission.NAMESPACE_READ));
    }

    @Test
    void grantNamespacePermission_whenNotAuthorized_shouldDenyBeforeCheckingDelegation() {
      when(authorizationService.authorizePermissionManagement(USER_ID, CATALOG_ID))
          .thenReturn(false);

      assertThatThrownBy(
              () ->
                  permissionUseCase.grantPermissionById(
                      USER_ID,
                      GranteeType.USER,
                      TARGET_USER_ID,
                      Permission.NAMESPACE_READ,
                      namespace.getId()))
          .isInstanceOfSatisfying(
              AnalyticsException.class,
              e -> assertThat(e.getErrorCode()).isEqualTo(AnalyticsErrorCode.ACCESS_DENIED));
      verify(authorizationService, never()).isAclDelegated(any());
    }

    @Test
    void revokeNamespacePermission_shouldStillDeleteAce() throws Exception {
      // Revoking stays allowed so that ACEs stored before delegation was enabled can be removed.
      when(authorizationService.authorizePermissionManagement(USER_ID, CATALOG_ID))
          .thenReturn(true);

      permissionUseCase.revokePermissionById(
          USER_ID, GranteeType.USER, TARGET_USER_ID, Permission.NAMESPACE_READ, namespace.getId());

      verify(aceRepository)
          .delete(
              txContext,
              GranteeType.USER,
              TARGET_USER_ID,
              namespace.getId(),
              Permission.NAMESPACE_READ);
    }
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
