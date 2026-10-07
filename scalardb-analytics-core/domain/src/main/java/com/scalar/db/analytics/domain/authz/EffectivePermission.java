/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import java.util.UUID;
import org.jspecify.annotations.Nullable;

/**
 * An effective permission resolved for a user, indicating which permission applies on which
 * resource and whether it was granted directly or via a role.
 *
 * @param permission the permission type
 * @param resourceId the resource the permission applies to
 * @param source whether the permission is direct or via a role
 * @param viaRoleId the role ID if source is VIA_ROLE, null otherwise
 * @param viaRoleName the role name if source is VIA_ROLE, null otherwise
 * @param resourceNames the names that identify the resource, or null if they were not resolved
 */
public record EffectivePermission(
    Permission permission,
    UUID resourceId,
    PermissionSource source,
    @Nullable UUID viaRoleId,
    @Nullable String viaRoleName,
    @Nullable ResourceNames resourceNames) {

  /** Creates an effective permission whose resource names are not resolved. */
  public EffectivePermission(
      Permission permission,
      UUID resourceId,
      PermissionSource source,
      @Nullable UUID viaRoleId,
      @Nullable String viaRoleName) {
    this(permission, resourceId, source, viaRoleId, viaRoleName, null);
  }

  /** Returns a copy of this permission with the given resource names. */
  public EffectivePermission withResourceNames(@Nullable ResourceNames names) {
    return new EffectivePermission(permission, resourceId, source, viaRoleId, viaRoleName, names);
  }
}
