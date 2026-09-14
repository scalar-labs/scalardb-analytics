/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repository.authz;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.Permission;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Resolves between Permission enum values and their deterministic UUIDs.
 *
 * <p>Permission UUIDs are derived from enum names and are stable across environments. This
 * component provides bidirectional lookup without requiring database access.
 */
@Component
public class PermissionIdResolver {

  private static final Map<UUID, Permission> ID_TO_PERMISSION = new HashMap<>();

  static {
    for (Permission permission : Permission.values()) {
      ID_TO_PERMISSION.put(permission.id(), permission);
    }
  }

  /** Resolves a Permission enum to its deterministic UUID. */
  public UUID resolveId(Permission permission) {
    return permission.id();
  }

  /** Resolves a UUID to its Permission enum. */
  public Permission resolvePermission(UUID permissionId) {
    Permission permission = ID_TO_PERMISSION.get(permissionId);
    if (permission == null) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of("field_name", "permission_id", "entity_name", permissionId.toString()));
    }
    return permission;
  }
}
