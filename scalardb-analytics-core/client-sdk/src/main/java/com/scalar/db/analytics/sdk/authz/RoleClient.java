/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.authz;

import com.scalar.db.analytics.api.authz.Role;
import com.scalar.db.analytics.api.error.AnalyticsException;
import java.util.List;

/**
 * Client for managing roles in the authorization system.
 *
 * <p>Provides operations to create, delete, and list roles, as well as grant and revoke role
 * assignments to users.
 */
public interface RoleClient {

  /**
   * Creates a new role with the given name.
   *
   * @param roleName the name for the new role
   * @return the created role
   * @throws AnalyticsException if creation fails
   */
  Role createRole(String roleName);

  /**
   * Deletes a role by name.
   *
   * @param roleName the name of the role to delete
   * @return {@code true} if the role was found and deleted, {@code false} if not found
   * @throws AnalyticsException if deletion fails
   */
  boolean deleteRole(String roleName);

  /**
   * Deletes a role by ID.
   *
   * @param roleId the ID of the role to delete
   * @return {@code true} if the role was found and deleted, {@code false} if not found
   * @throws AnalyticsException if deletion fails
   */
  boolean deleteRoleById(String roleId);

  /**
   * Lists all roles.
   *
   * @return the list of all roles
   * @throws AnalyticsException if the operation fails
   */
  List<Role> listRoles();

  /**
   * Assigns a role to a user by name.
   *
   * @param roleName the name of the role to assign
   * @param username the username of the user to assign the role to
   * @throws AnalyticsException if the operation fails
   */
  void grantRole(String roleName, String username);

  /**
   * Assigns a role to a user by ID.
   *
   * @param roleId the ID of the role to assign
   * @param userId the ID of the user to assign the role to
   * @throws AnalyticsException if the operation fails
   */
  void grantRoleById(String roleId, String userId);

  /**
   * Removes a role assignment from a user by name.
   *
   * @param roleName the name of the role to remove
   * @param username the username of the user to remove the role from
   * @throws AnalyticsException if the operation fails
   */
  void revokeRole(String roleName, String username);

  /**
   * Removes a role assignment from a user by ID.
   *
   * @param roleId the ID of the role to remove
   * @param userId the ID of the user to remove the role from
   * @throws AnalyticsException if the operation fails
   */
  void revokeRoleById(String roleId, String userId);
}
