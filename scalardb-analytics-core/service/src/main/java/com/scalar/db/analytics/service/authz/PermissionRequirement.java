/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.authz;

import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import java.util.Set;

/**
 * A requirement specifying which permissions satisfy authorization at a given resource level.
 *
 * <p>Multiple requirements are combined with OR semantics: if any single requirement is satisfied,
 * authorization succeeds. Within a requirement, the satisfying permissions are also OR-ed: the user
 * must hold at least one.
 */
public record PermissionRequirement(ResourceRef resource, Set<Permission> satisfyingPermissions) {
  public PermissionRequirement {
    satisfyingPermissions = Set.copyOf(satisfyingPermissions);
  }
}
