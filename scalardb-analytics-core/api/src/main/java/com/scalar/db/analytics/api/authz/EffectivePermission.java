/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.authz;

import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * An effective permission resolved for a user, indicating which permission applies on which
 * resource and whether it was granted directly or via a role.
 */
@Value
public class EffectivePermission {
  String permission;
  String resourceId;
  String source;
  @Nullable String viaRoleId;
  @Nullable String viaRoleName;
}
