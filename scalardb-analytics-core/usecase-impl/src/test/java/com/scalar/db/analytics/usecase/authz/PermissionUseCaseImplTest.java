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
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.EffectivePermission;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.PermissionSource;
import com.scalar.db.analytics.domain.authz.ResourceNames;
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
import java.util.Map;
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
  void listPermissionsById_shouldResolveResourceNamesOncePerResource() throws Exception {
    DataSource dataSource =
        new DataSource(
            UUID.randomUUID(),
            CATALOG_ID,
            "ds",
            new PostgreSql("localhost", 5432, "user", "password", "db"));
    Namespace namespace =
        new Namespace(UUID.randomUUID(), dataSource.getId(), List.of("db", "public"));
    Table table = new Table(new TableInfo(UUID.randomUUID(), namespace.getId(), "orders"));
    when(catalogRepository.findById(txContext, CATALOG_ID))
        .thenReturn(Optional.of(Catalog.of(CATALOG_ID, "cat")));
    when(dataSourceRepository.findById(txContext, dataSource.getId()))
        .thenReturn(Optional.of(dataSource));
    when(namespaceRepository.findById(txContext, namespace.getId()))
        .thenReturn(Optional.of(namespace));
    when(tableRepository.findById(txContext, table.getInfo().getId()))
        .thenReturn(Optional.of(table));

    when(aceRepository.findByGrantee(txContext, GranteeType.USER, USER_ID))
        .thenReturn(
            List.of(
                AccessControlEntry.create(
                    GranteeType.USER, USER_ID, CATALOG_ID, Permission.CATALOG_READ),
                AccessControlEntry.create(
                    GranteeType.USER, USER_ID, namespace.getId(), Permission.NAMESPACE_READ),
                AccessControlEntry.create(
                    GranteeType.USER, USER_ID, table.getInfo().getId(), Permission.TABLE_READ)));
    // The same table again through a role: its names must not be looked up a second time.
    when(roleAssignmentRepository.findByUserId(txContext, USER_ID))
        .thenReturn(List.of(new RoleAssignment(USER_ID, ROLE_ID)));
    when(roleRepository.findById(txContext, ROLE_ID))
        .thenReturn(Optional.of(new Role(ROLE_ID, "viewer", false)));
    when(aceRepository.findByGrantee(txContext, GranteeType.ROLE, ROLE_ID))
        .thenReturn(
            List.of(
                AccessControlEntry.create(
                    GranteeType.ROLE, ROLE_ID, table.getInfo().getId(), Permission.TABLE_READ)));

    List<EffectivePermission> result = permissionUseCase.listPermissionsById(USER_ID, USER_ID);

    assertThat(result)
        .map(EffectivePermission::resourceNames)
        .containsExactly(
            new ResourceNames("cat", null, List.of(), null),
            new ResourceNames("cat", "ds", List.of("db", "public"), null),
            new ResourceNames("cat", "ds", List.of("db", "public"), "orders"),
            new ResourceNames("cat", "ds", List.of("db", "public"), "orders"));
    // Each resource, including the parents shared by the namespace and the table, is looked up
    // once.
    verify(tableRepository, times(1)).findById(txContext, table.getInfo().getId());
    verify(namespaceRepository, times(1)).findById(txContext, namespace.getId());
    verify(dataSourceRepository, times(1)).findById(txContext, dataSource.getId());
    verify(catalogRepository, times(1)).findById(txContext, CATALOG_ID);
  }

  @Test
  void listPermissionsById_whenResourceIsMissing_shouldLeaveNamesUnresolved() throws Exception {
    UUID missingTableId = UUID.randomUUID();
    when(aceRepository.findByGrantee(txContext, GranteeType.USER, USER_ID))
        .thenReturn(
            List.of(
                AccessControlEntry.create(
                    GranteeType.USER, USER_ID, missingTableId, Permission.TABLE_READ)));
    when(roleAssignmentRepository.findByUserId(txContext, USER_ID)).thenReturn(List.of());
    when(tableRepository.findById(txContext, missingTableId)).thenReturn(Optional.empty());

    List<EffectivePermission> result = permissionUseCase.listPermissionsById(USER_ID, USER_ID);

    assertThat(result).hasSize(1);
    assertThat(result.get(0).resourceId()).isEqualTo(missingTableId);
    assertThat(result.get(0).resourceNames()).isNull();
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

  @Nested
  class WhenReferencedEntityDoesNotExist {

    private final DataSource dataSource =
        new DataSource(
            UUID.randomUUID(),
            CATALOG_ID,
            "ds",
            ScalarDbProvider.builder()
                .configs(Collections.singletonMap("scalar.db.storage", "cassandra"))
                .build());
    private final Namespace namespace =
        new Namespace(UUID.randomUUID(), dataSource.getId(), List.of("ns1", "ns2"));

    @BeforeEach
    void setUpGrantee() throws Exception {
      lenient()
          .when(authUserRepository.findByUsername(txContext, "alice"))
          .thenReturn(Optional.of(new AuthUser(TARGET_USER_ID, "alice")));
    }

    private void grantTablePermission() {
      permissionUseCase.grantTablePermission(
          USER_ID,
          GranteeType.USER,
          "alice",
          Permission.TABLE_READ,
          "catalog",
          "ds",
          List.of("ns1", "ns2"),
          "tbl");
    }

    private void assertAnalyticsError(
        Runnable call, AnalyticsErrorCode errorCode, Map<String, String> metadata) {
      assertThatThrownBy(call::run)
          .isInstanceOfSatisfying(
              AnalyticsException.class,
              e -> {
                assertThat(e.getErrorCode()).isEqualTo(errorCode);
                assertThat(e.getMetadata()).isEqualTo(metadata);
              });
      verify(aceRepository, never()).create(any(), any());
    }

    @Test
    void grantPermission_toMissingUser_shouldThrowUserNotFound() {
      when(authUserRepository.findByUsername(txContext, "bob")).thenReturn(Optional.empty());

      assertAnalyticsError(
          () ->
              permissionUseCase.grantCatalogPermission(
                  USER_ID, GranteeType.USER, "bob", Permission.CATALOG_READ, "catalog"),
          AnalyticsErrorCode.USER_NOT_FOUND,
          Map.of("username", "bob"));
    }

    @Test
    void grantPermission_toMissingRole_shouldThrowRoleNotFound() {
      when(roleRepository.findByName(txContext, "analyst")).thenReturn(Optional.empty());

      assertAnalyticsError(
          () ->
              permissionUseCase.grantCatalogPermission(
                  USER_ID, GranteeType.ROLE, "analyst", Permission.CATALOG_READ, "catalog"),
          AnalyticsErrorCode.ROLE_NOT_FOUND,
          Map.of("role_name", "analyst"));
    }

    @Test
    void grantTablePermission_inMissingCatalog_shouldThrowCatalogNotFound() {
      when(catalogRepository.findByName(txContext, "catalog")).thenReturn(Optional.empty());

      assertAnalyticsError(
          this::grantTablePermission,
          AnalyticsErrorCode.CATALOG_NOT_FOUND,
          Map.of("catalog_name", "catalog"));
    }

    @Test
    void grantTablePermission_inMissingDataSource_shouldThrowDataSourceNotFound() {
      when(catalogRepository.findByName(txContext, "catalog"))
          .thenReturn(Optional.of(Catalog.of(CATALOG_ID, "catalog")));
      when(dataSourceRepository.findByCatalogIdAndName(txContext, CATALOG_ID, "ds"))
          .thenReturn(Optional.empty());

      assertAnalyticsError(
          this::grantTablePermission,
          AnalyticsErrorCode.DATA_SOURCE_NOT_FOUND,
          Map.of("catalog_name", "catalog", "data_source_name", "ds"));
    }

    @Test
    void grantTablePermission_inMissingNamespace_shouldThrowNamespaceNotFound() {
      when(catalogRepository.findByName(txContext, "catalog"))
          .thenReturn(Optional.of(Catalog.of(CATALOG_ID, "catalog")));
      when(dataSourceRepository.findByCatalogIdAndName(txContext, CATALOG_ID, "ds"))
          .thenReturn(Optional.of(dataSource));
      when(namespaceRepository.findByDataSourceIdAndNames(
              txContext, dataSource.getId(), List.of("ns1", "ns2")))
          .thenReturn(Optional.empty());

      assertAnalyticsError(
          this::grantTablePermission,
          AnalyticsErrorCode.NAMESPACE_NOT_FOUND,
          Map.of("catalog_name", "catalog", "data_source_name", "ds", "namespace_name", "ns1.ns2"));
    }

    @Test
    void grantTablePermission_onMissingTable_shouldThrowTableNotFound() {
      when(catalogRepository.findByName(txContext, "catalog"))
          .thenReturn(Optional.of(Catalog.of(CATALOG_ID, "catalog")));
      when(dataSourceRepository.findByCatalogIdAndName(txContext, CATALOG_ID, "ds"))
          .thenReturn(Optional.of(dataSource));
      when(namespaceRepository.findByDataSourceIdAndNames(
              txContext, dataSource.getId(), List.of("ns1", "ns2")))
          .thenReturn(Optional.of(namespace));
      when(tableRepository.findByNamespaceIdAndName(txContext, namespace.getId(), "tbl"))
          .thenReturn(Optional.empty());

      assertAnalyticsError(
          this::grantTablePermission,
          AnalyticsErrorCode.TABLE_NOT_FOUND,
          Map.of(
              "catalog_name", "catalog",
              "data_source_name", "ds",
              "namespace_name", "ns1.ns2",
              "table_name", "tbl"));
    }

    @Test
    void grantPermissionById_onMissingTable_shouldThrowTableNotFound() {
      UUID tableId = UUID.randomUUID();
      when(tableRepository.findById(txContext, tableId)).thenReturn(Optional.empty());

      assertAnalyticsError(
          () ->
              permissionUseCase.grantPermissionById(
                  USER_ID, GranteeType.USER, TARGET_USER_ID, Permission.TABLE_READ, tableId),
          AnalyticsErrorCode.TABLE_NOT_FOUND,
          Map.of("table_id", tableId.toString()));
    }

    @Test
    void grantPermissionById_onTableWithMissingNamespace_shouldThrowDataInconsistency() {
      Table table = new Table(new TableInfo(UUID.randomUUID(), namespace.getId(), "tbl"));
      when(tableRepository.findById(txContext, table.getInfo().getId()))
          .thenReturn(Optional.of(table));
      when(namespaceRepository.findById(txContext, namespace.getId())).thenReturn(Optional.empty());

      assertAnalyticsError(
          () ->
              permissionUseCase.grantPermissionById(
                  USER_ID,
                  GranteeType.USER,
                  TARGET_USER_ID,
                  Permission.TABLE_READ,
                  table.getInfo().getId()),
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of(
              "table_id",
              table.getInfo().getId().toString(),
              "namespace_id",
              namespace.getId().toString()));
    }

    @Test
    void listPermissions_ofMissingUser_shouldThrowUserNotFound() {
      when(authUserRepository.findByUsername(txContext, "bob")).thenReturn(Optional.empty());

      assertAnalyticsError(
          () -> permissionUseCase.listPermissions(USER_ID, "bob"),
          AnalyticsErrorCode.USER_NOT_FOUND,
          Map.of("username", "bob"));
    }

    @Test
    void listPermissionsForRole_ofMissingRole_shouldThrowRoleNotFound() {
      when(roleRepository.findByName(txContext, "analyst")).thenReturn(Optional.empty());

      assertAnalyticsError(
          () -> permissionUseCase.listPermissionsForRole(USER_ID, "analyst"),
          AnalyticsErrorCode.ROLE_NOT_FOUND,
          Map.of("role_name", "analyst"));
    }

    @Test
    void listPermissionsForRoleById_ofMissingRole_shouldThrowRoleNotFound() {
      when(roleRepository.findById(txContext, ROLE_ID)).thenReturn(Optional.empty());

      assertAnalyticsError(
          () -> permissionUseCase.listPermissionsForRoleById(USER_ID, ROLE_ID),
          AnalyticsErrorCode.ROLE_NOT_FOUND,
          Map.of("role_id", ROLE_ID.toString()));
    }
  }
}
