/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.authz;

import com.scalar.db.analytics.api.authz.EffectivePermission;
import com.scalar.db.analytics.api.error.AnalyticsException;
import java.util.List;

/**
 * Client for managing permissions in the authorization system.
 *
 * <p>Provides operations to grant and revoke permissions on resources, and to list effective
 * permissions for a user.
 */
public interface PermissionClient {

  /**
   * Grants a permission on a catalog to a user or role by name.
   *
   * @param granteeType the type of grantee ({@code "USER"} or {@code "ROLE"})
   * @param granteeName the username or role name
   * @param permission the permission type (e.g., {@code "CATALOG_READ"})
   * @param catalogName the name of the catalog
   * @throws AnalyticsException if the operation fails
   */
  void grantCatalogPermission(
      String granteeType, String granteeName, String permission, String catalogName);

  /**
   * Grants a permission on a data source to a user or role by name.
   *
   * @param granteeType the type of grantee ({@code "USER"} or {@code "ROLE"})
   * @param granteeName the username or role name
   * @param permission the permission type (e.g., {@code "DATA_SOURCE_READ"})
   * @param catalogName the name of the catalog containing the data source
   * @param dataSourceName the name of the data source
   * @throws AnalyticsException if the operation fails
   */
  void grantDataSourcePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName);

  /**
   * Grants a permission on a namespace to a user or role by name.
   *
   * @param granteeType the type of grantee ({@code "USER"} or {@code "ROLE"})
   * @param granteeName the username or role name
   * @param permission the permission type (e.g., {@code "NAMESPACE_READ"})
   * @param catalogName the name of the catalog
   * @param dataSourceName the name of the data source
   * @param namespaceNames the namespace name parts
   * @throws AnalyticsException if the operation fails
   */
  void grantNamespacePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames);

  /**
   * Grants a permission on a table to a user or role by name.
   *
   * @param granteeType the type of grantee ({@code "USER"} or {@code "ROLE"})
   * @param granteeName the username or role name
   * @param permission the permission type (e.g., {@code "TABLE_READ"})
   * @param catalogName the name of the catalog
   * @param dataSourceName the name of the data source
   * @param namespaceNames the namespace name parts
   * @param tableName the name of the table
   * @throws AnalyticsException if the operation fails
   */
  void grantTablePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName);

  /**
   * Grants a permission on a resource to a user or role by ID.
   *
   * @param granteeType the type of grantee ({@code "USER"} or {@code "ROLE"})
   * @param granteeId the ID of the user or role
   * @param permission the permission type (e.g., {@code "CATALOG_READ"})
   * @param resourceId the ID of the resource
   * @throws AnalyticsException if the operation fails
   */
  void grantPermissionById(
      String granteeType, String granteeId, String permission, String resourceId);

  /**
   * Revokes a permission on a catalog from a user or role by name.
   *
   * @param granteeType the type of grantee ({@code "USER"} or {@code "ROLE"})
   * @param granteeName the username or role name
   * @param permission the permission type (e.g., {@code "CATALOG_READ"})
   * @param catalogName the name of the catalog
   * @throws AnalyticsException if the operation fails
   */
  void revokeCatalogPermission(
      String granteeType, String granteeName, String permission, String catalogName);

  /**
   * Revokes a permission on a data source from a user or role by name.
   *
   * @param granteeType the type of grantee ({@code "USER"} or {@code "ROLE"})
   * @param granteeName the username or role name
   * @param permission the permission type (e.g., {@code "DATA_SOURCE_READ"})
   * @param catalogName the name of the catalog containing the data source
   * @param dataSourceName the name of the data source
   * @throws AnalyticsException if the operation fails
   */
  void revokeDataSourcePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName);

  /**
   * Revokes a permission on a namespace from a user or role by name.
   *
   * @param granteeType the type of grantee ({@code "USER"} or {@code "ROLE"})
   * @param granteeName the username or role name
   * @param permission the permission type (e.g., {@code "NAMESPACE_READ"})
   * @param catalogName the name of the catalog
   * @param dataSourceName the name of the data source
   * @param namespaceNames the namespace name parts
   * @throws AnalyticsException if the operation fails
   */
  void revokeNamespacePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames);

  /**
   * Revokes a permission on a table from a user or role by name.
   *
   * @param granteeType the type of grantee ({@code "USER"} or {@code "ROLE"})
   * @param granteeName the username or role name
   * @param permission the permission type (e.g., {@code "TABLE_READ"})
   * @param catalogName the name of the catalog
   * @param dataSourceName the name of the data source
   * @param namespaceNames the namespace name parts
   * @param tableName the name of the table
   * @throws AnalyticsException if the operation fails
   */
  void revokeTablePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName);

  /**
   * Revokes a permission on a resource from a user or role by ID.
   *
   * @param granteeType the type of grantee ({@code "USER"} or {@code "ROLE"})
   * @param granteeId the ID of the user or role
   * @param permission the permission type (e.g., {@code "CATALOG_READ"})
   * @param resourceId the ID of the resource
   * @throws AnalyticsException if the operation fails
   */
  void revokePermissionById(
      String granteeType, String granteeId, String permission, String resourceId);

  /**
   * Lists all effective permissions for a user by username.
   *
   * @param username the username
   * @return the list of effective permissions (including direct and via-role grants)
   * @throws AnalyticsException if the operation fails
   */
  List<EffectivePermission> listPermissions(String username);

  /**
   * Lists all effective permissions for a user by ID.
   *
   * @param userId the ID of the user
   * @return the list of effective permissions (including direct and via-role grants)
   * @throws AnalyticsException if the operation fails
   */
  List<EffectivePermission> listPermissionsById(String userId);

  /**
   * Lists permissions granted to a role by name.
   *
   * @param roleName the role name
   * @return the list of permissions granted directly to the role
   * @throws AnalyticsException if the operation fails
   */
  List<EffectivePermission> listPermissionsForRole(String roleName);

  /**
   * Lists permissions granted to a role by ID.
   *
   * @param roleId the role ID
   * @return the list of permissions granted directly to the role
   * @throws AnalyticsException if the operation fails
   */
  List<EffectivePermission> listPermissionsForRoleById(String roleId);
}
