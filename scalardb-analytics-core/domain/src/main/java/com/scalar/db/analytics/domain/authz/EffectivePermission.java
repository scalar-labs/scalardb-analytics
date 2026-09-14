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
 */
public record EffectivePermission(
    Permission permission,
    UUID resourceId,
    PermissionSource source,
    @Nullable UUID viaRoleId,
    @Nullable String viaRoleName) {}
