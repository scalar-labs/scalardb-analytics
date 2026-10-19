/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.authz;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.EffectivePermission;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.PermissionSource;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.repository.CatalogRepository;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.PermissionQueryService;
import com.scalar.db.analytics.repository.PermissionQueryService.GrantedPermission;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public class PermissionUseCaseImpl<T extends RepositoryTransactionContext>
    implements PermissionUseCase {

  private final AccessControlEntryRepository<T> aceRepository;
  private final RoleRepository<T> roleRepository;
  private final CatalogRepository<T> catalogRepository;
  private final DataSourceRepository<T> dataSourceRepository;
  private final NamespaceRepository<T> namespaceRepository;
  private final TableRepository<T> tableRepository;
  private final AuthUserRepository<T> authUserRepository;
  private final PermissionQueryService<T> permissionQueryService;
  private final RepositoryTransactionManager<T> txManager;
  private final AuthorizationService authorizationService;

  public PermissionUseCaseImpl(
      AccessControlEntryRepository<T> aceRepository,
      RoleRepository<T> roleRepository,
      CatalogRepository<T> catalogRepository,
      DataSourceRepository<T> dataSourceRepository,
      NamespaceRepository<T> namespaceRepository,
      TableRepository<T> tableRepository,
      AuthUserRepository<T> authUserRepository,
      PermissionQueryService<T> permissionQueryService,
      RepositoryTransactionManager<T> txManager,
      AuthorizationService authorizationService) {
    this.aceRepository = aceRepository;
    this.roleRepository = roleRepository;
    this.catalogRepository = catalogRepository;
    this.dataSourceRepository = dataSourceRepository;
    this.namespaceRepository = namespaceRepository;
    this.tableRepository = tableRepository;
    this.authUserRepository = authUserRepository;
    this.permissionQueryService = permissionQueryService;
    this.txManager = txManager;
    this.authorizationService = authorizationService;
  }

  // --- Grant by name (per resource type) ---

  @Override
  public void grantCatalogPermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName) {
    txManager.withTransaction(
        ctx -> {
          UUID granteeId = resolveGranteeId(ctx, granteeType, granteeName);
          UUID resourceId = resolveCatalogResourceId(ctx, catalogName);
          doGrantPermission(ctx, userId, granteeType, granteeId, permission, resourceId);
          return null;
        });
  }

  @Override
  public void grantDataSourcePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName) {
    txManager.withTransaction(
        ctx -> {
          UUID granteeId = resolveGranteeId(ctx, granteeType, granteeName);
          UUID resourceId = resolveDataSourceResourceId(ctx, catalogName, dataSourceName);
          doGrantPermission(ctx, userId, granteeType, granteeId, permission, resourceId);
          return null;
        });
  }

  @Override
  public void grantNamespacePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames) {
    txManager.withTransaction(
        ctx -> {
          UUID granteeId = resolveGranteeId(ctx, granteeType, granteeName);
          UUID resourceId =
              resolveNamespaceResourceId(ctx, catalogName, dataSourceName, namespaceNames);
          doGrantPermission(ctx, userId, granteeType, granteeId, permission, resourceId);
          return null;
        });
  }

  @Override
  public void grantTablePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName) {
    txManager.withTransaction(
        ctx -> {
          UUID granteeId = resolveGranteeId(ctx, granteeType, granteeName);
          UUID resourceId =
              resolveTableResourceId(ctx, catalogName, dataSourceName, namespaceNames, tableName);
          doGrantPermission(ctx, userId, granteeType, granteeId, permission, resourceId);
          return null;
        });
  }

  @Override
  public void grantPermissionById(
      UUID userId,
      GranteeType granteeType,
      UUID granteeId,
      Permission permission,
      UUID resourceId) {
    txManager.withTransaction(
        ctx -> {
          doGrantPermission(ctx, userId, granteeType, granteeId, permission, resourceId);
          return null;
        });
  }

  // --- Revoke by name (per resource type) ---

  @Override
  public void revokeCatalogPermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName) {
    txManager.withTransaction(
        ctx -> {
          UUID granteeId = resolveGranteeId(ctx, granteeType, granteeName);
          UUID resourceId = resolveCatalogResourceId(ctx, catalogName);
          doRevokePermission(ctx, userId, granteeType, granteeId, permission, resourceId);
          return null;
        });
  }

  @Override
  public void revokeDataSourcePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName) {
    txManager.withTransaction(
        ctx -> {
          UUID granteeId = resolveGranteeId(ctx, granteeType, granteeName);
          UUID resourceId = resolveDataSourceResourceId(ctx, catalogName, dataSourceName);
          doRevokePermission(ctx, userId, granteeType, granteeId, permission, resourceId);
          return null;
        });
  }

  @Override
  public void revokeNamespacePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames) {
    txManager.withTransaction(
        ctx -> {
          UUID granteeId = resolveGranteeId(ctx, granteeType, granteeName);
          UUID resourceId =
              resolveNamespaceResourceId(ctx, catalogName, dataSourceName, namespaceNames);
          doRevokePermission(ctx, userId, granteeType, granteeId, permission, resourceId);
          return null;
        });
  }

  @Override
  public void revokeTablePermission(
      UUID userId,
      GranteeType granteeType,
      String granteeName,
      Permission permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName) {
    txManager.withTransaction(
        ctx -> {
          UUID granteeId = resolveGranteeId(ctx, granteeType, granteeName);
          UUID resourceId =
              resolveTableResourceId(ctx, catalogName, dataSourceName, namespaceNames, tableName);
          doRevokePermission(ctx, userId, granteeType, granteeId, permission, resourceId);
          return null;
        });
  }

  @Override
  public void revokePermissionById(
      UUID userId,
      GranteeType granteeType,
      UUID granteeId,
      Permission permission,
      UUID resourceId) {
    txManager.withTransaction(
        ctx -> {
          doRevokePermission(ctx, userId, granteeType, granteeId, permission, resourceId);
          return null;
        });
  }

  // --- List ---

  @Override
  public List<EffectivePermission> listPermissions(UUID userId, String username) {
    return txManager.withTransaction(
        ctx -> {
          UUID targetUserId =
              authUserRepository
                  .findByUsername(ctx, username)
                  .orElseThrow(() -> new IllegalArgumentException("User not found: " + username))
                  .getUserId();
          return collectPermissionsForUser(ctx, userId, targetUserId);
        });
  }

  @Override
  public List<EffectivePermission> listPermissionsById(UUID userId, UUID targetUserId) {
    return txManager.withTransaction(ctx -> collectPermissionsForUser(ctx, userId, targetUserId));
  }

  @Override
  public List<EffectivePermission> listPermissionsForRole(UUID userId, String roleName) {
    return txManager.withTransaction(
        ctx -> {
          Role role =
              roleRepository
                  .findByName(ctx, roleName)
                  .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
          return collectPermissionsForRole(ctx, userId, role.id());
        });
  }

  @Override
  public List<EffectivePermission> listPermissionsForRoleById(UUID userId, UUID roleId) {
    return txManager.withTransaction(
        ctx -> {
          // Validate the role exists; throws if not. The return value is intentionally
          // discarded (ACEs are looked up by role ID below).
          @SuppressWarnings("unused")
          Role unused =
              roleRepository
                  .findById(ctx, roleId)
                  .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleId));
          return collectPermissionsForRole(ctx, userId, roleId);
        });
  }

  // --- Internal helpers ---

  private void doGrantPermission(
      T ctx,
      UUID userId,
      GranteeType granteeType,
      UUID granteeId,
      Permission permission,
      UUID resourceId) {
    DataSource dataSource = resolveOwningDataSource(ctx, permission, resourceId);
    UUID catalogId = dataSource != null ? dataSource.getCatalogId() : resourceId;
    if (!authorizationService.authorizePermissionManagement(userId, catalogId)) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED, Map.of("user_id", userId.toString()));
    }
    // An ACE on a resource under ACL delegation would be stored but never evaluated, so the
    // grant would silently have no effect. Reject it instead.
    if (dataSource != null
        && authorizationService.isAclDelegated(
            new ResourceRef(
                permission.resourceType(), resourceId, dataSource.getProvider().getType()))) {
      throw new AnalyticsException(
          AnalyticsErrorCode.RESOURCE_UNDER_ACL_DELEGATION,
          Map.of(
              "permission", permission.name(),
              "resource_id", resourceId.toString(),
              "data_source_name", dataSource.getName()));
    }
    aceRepository.create(
        ctx, AccessControlEntry.create(granteeType, granteeId, resourceId, permission));
  }

  private void doRevokePermission(
      T ctx,
      UUID userId,
      GranteeType granteeType,
      UUID granteeId,
      Permission permission,
      UUID resourceId) {
    UUID catalogId = resolveCatalogId(ctx, permission, resourceId);
    if (!authorizationService.authorizePermissionManagement(userId, catalogId)) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED, Map.of("user_id", userId.toString()));
    }
    aceRepository.delete(ctx, granteeType, granteeId, resourceId, permission);
  }

  /**
   * Returns a role's directly-granted ACEs as effective permissions, each with the names of its
   * resource. Authorization (SUPERADMIN) is verified before fetching. The caller owns the
   * transaction context.
   */
  private List<EffectivePermission> collectPermissionsForRole(T ctx, UUID userId, UUID roleId) {
    if (!authorizationService.authorizeSuperAdmin(userId)) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED, Map.of("user_id", userId.toString()));
    }
    List<EffectivePermission> result = new ArrayList<>();
    for (GrantedPermission granted :
        permissionQueryService.listByGrantee(ctx, GranteeType.ROLE, roleId)) {
      result.add(toEffectivePermission(granted, null));
    }
    return result;
  }

  /**
   * Returns a user's effective permissions (direct ACEs + ACEs via role assignments), each with the
   * names of its resource. Authorization (self-query or SUPERADMIN) is verified before fetching.
   * The caller owns the transaction context.
   */
  private List<EffectivePermission> collectPermissionsForUser(
      T ctx, UUID userId, UUID targetUserId) {
    if (!userId.equals(targetUserId) && !authorizationService.authorizeSuperAdmin(userId)) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED, Map.of("user_id", userId.toString()));
    }
    List<EffectivePermission> result = new ArrayList<>();

    for (GrantedPermission granted :
        permissionQueryService.listByGrantee(ctx, GranteeType.USER, targetUserId)) {
      result.add(toEffectivePermission(granted, null));
    }

    // The permissions of each assigned role are read with separate queries per role. They cannot
    // be joined to the role assignments: ScalarDB SQL requires a join predicate to cover all the
    // primary-key columns or a secondary-index column of the right table, and the primary key of
    // the access control entries is (grantee_id, permission_id, resource_id, grantee_type), with no
    // index on grantee_id.
    for (Role role : permissionQueryService.listRolesByUserId(ctx, targetUserId)) {
      for (GrantedPermission granted :
          permissionQueryService.listByGrantee(ctx, GranteeType.ROLE, role.id())) {
        result.add(toEffectivePermission(granted, role));
      }
    }
    return result;
  }

  /**
   * Returns a granted permission as an effective permission: granted via the role if one is given,
   * and directly otherwise.
   */
  private static EffectivePermission toEffectivePermission(
      GrantedPermission granted, @Nullable Role viaRole) {
    return new EffectivePermission(
        granted.permission(),
        granted.resourceId(),
        viaRole == null ? PermissionSource.DIRECT : PermissionSource.VIA_ROLE,
        viaRole == null ? null : viaRole.id(),
        viaRole == null ? null : viaRole.name(),
        granted.resourceNames());
  }

  // --- Name resolution helpers ---

  private UUID resolveGranteeId(T ctx, GranteeType granteeType, String granteeName) {
    return switch (granteeType) {
      case USER ->
          authUserRepository
              .findByUsername(ctx, granteeName)
              .orElseThrow(() -> new IllegalArgumentException("User not found: " + granteeName))
              .getUserId();
      case ROLE ->
          roleRepository
              .findByName(ctx, granteeName)
              .orElseThrow(() -> new IllegalArgumentException("Role not found: " + granteeName))
              .id();
    };
  }

  private UUID resolveCatalogResourceId(T ctx, String catalogName) {
    return catalogRepository
        .findByName(ctx, catalogName)
        .orElseThrow(() -> new IllegalArgumentException("Catalog not found: " + catalogName))
        .getId();
  }

  private UUID resolveDataSourceResourceId(T ctx, String catalogName, String dataSourceName) {
    var catalog =
        catalogRepository
            .findByName(ctx, catalogName)
            .orElseThrow(() -> new IllegalArgumentException("Catalog not found: " + catalogName));
    return dataSourceRepository
        .findByCatalogIdAndName(ctx, catalog.getId(), dataSourceName)
        .orElseThrow(() -> new IllegalArgumentException("DataSource not found: " + dataSourceName))
        .getId();
  }

  private UUID resolveNamespaceResourceId(
      T ctx, String catalogName, String dataSourceName, List<String> namespaceNames) {
    var catalog =
        catalogRepository
            .findByName(ctx, catalogName)
            .orElseThrow(() -> new IllegalArgumentException("Catalog not found: " + catalogName));
    var dataSource =
        dataSourceRepository
            .findByCatalogIdAndName(ctx, catalog.getId(), dataSourceName)
            .orElseThrow(
                () -> new IllegalArgumentException("DataSource not found: " + dataSourceName));
    return namespaceRepository
        .findByDataSourceIdAndNames(ctx, dataSource.getId(), namespaceNames)
        .orElseThrow(() -> new IllegalArgumentException("Namespace not found: " + namespaceNames))
        .getId();
  }

  private UUID resolveTableResourceId(
      T ctx,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName) {
    var catalog =
        catalogRepository
            .findByName(ctx, catalogName)
            .orElseThrow(() -> new IllegalArgumentException("Catalog not found: " + catalogName));
    var dataSource =
        dataSourceRepository
            .findByCatalogIdAndName(ctx, catalog.getId(), dataSourceName)
            .orElseThrow(
                () -> new IllegalArgumentException("DataSource not found: " + dataSourceName));
    var namespace =
        namespaceRepository
            .findByDataSourceIdAndNames(ctx, dataSource.getId(), namespaceNames)
            .orElseThrow(
                () -> new IllegalArgumentException("Namespace not found: " + namespaceNames));
    return tableRepository
        .findByNamespaceIdAndName(ctx, namespace.getId(), tableName)
        .orElseThrow(() -> new IllegalArgumentException("Table not found: " + tableName))
        .getInfo()
        .getId();
  }

  private UUID resolveCatalogId(T ctx, Permission permission, UUID resourceId) {
    DataSource dataSource = resolveOwningDataSource(ctx, permission, resourceId);
    return dataSource != null ? dataSource.getCatalogId() : resourceId;
  }

  /**
   * Returns the data source that the resource of the permission belongs to, or the resource itself
   * for a data source permission. Returns null for a catalog permission, whose resource belongs to
   * no data source.
   */
  private @Nullable DataSource resolveOwningDataSource(
      T ctx, Permission permission, UUID resourceId) {
    return switch (permission.resourceType()) {
      case CATALOG -> null;
      case DATA_SOURCE ->
          dataSourceRepository
              .findById(ctx, resourceId)
              .orElseThrow(
                  () -> new IllegalArgumentException("DataSource not found: " + resourceId));
      case NAMESPACE -> {
        var namespace =
            namespaceRepository
                .findById(ctx, resourceId)
                .orElseThrow(
                    () -> new IllegalArgumentException("Namespace not found: " + resourceId));
        yield dataSourceRepository
            .findById(ctx, namespace.getDataSourceId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "DataSource not found: " + namespace.getDataSourceId()));
      }
      case TABLE -> {
        var table =
            tableRepository
                .findById(ctx, resourceId)
                .orElseThrow(() -> new IllegalArgumentException("Table not found: " + resourceId));
        var namespace =
            namespaceRepository
                .findById(ctx, table.getInfo().getNamespaceId())
                .orElseThrow(
                    () ->
                        new IllegalArgumentException(
                            "Namespace not found: " + table.getInfo().getNamespaceId()));
        yield dataSourceRepository
            .findById(ctx, namespace.getDataSourceId())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "DataSource not found: " + namespace.getDataSourceId()));
      }
    };
  }
}
