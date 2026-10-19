/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository;

import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceNames;
import com.scalar.db.analytics.domain.authz.Role;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** Reads permissions together with the names of their resources and the roles they come from. */
public interface PermissionQueryService<T extends RepositoryTransactionContext> {

  /**
   * Returns the permissions granted directly to a grantee, each with the names of its resource, in
   * the order in which the grantee's access control entries are stored. A permission whose resource
   * can no longer be found is returned without names.
   */
  List<GrantedPermission> listByGrantee(T ctx, GranteeType granteeType, UUID granteeId);

  /** Returns the roles assigned to a user. */
  List<Role> listRolesByUserId(T ctx, UUID userId);

  /**
   * A permission granted on a resource.
   *
   * @param permission the permission granted
   * @param resourceId the resource the permission applies to
   * @param resourceNames the names that identify the resource, or null if it cannot be found
   */
  record GrantedPermission(
      Permission permission, UUID resourceId, @Nullable ResourceNames resourceNames) {}
}
