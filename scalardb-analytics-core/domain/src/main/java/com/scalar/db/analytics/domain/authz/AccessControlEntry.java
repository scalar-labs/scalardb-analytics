/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import java.util.UUID;

/**
 * An entry granting a specific permission on a resource to a grantee (user or role).
 *
 * <p>The resource type is derivable from the permission (e.g., CATALOG_READ implies CATALOG).
 *
 * @param granteeType whether the grantee is a user or role
 * @param granteeId the ID of the user or role receiving the grant
 * @param resourceId the ID of the specific resource
 * @param permission the permission granted
 */
public record AccessControlEntry(
    GranteeType granteeType, UUID granteeId, UUID resourceId, Permission permission) {

  public static AccessControlEntry create(
      GranteeType granteeType, UUID granteeId, UUID resourceId, Permission permission) {
    return new AccessControlEntry(granteeType, granteeId, resourceId, permission);
  }
}
