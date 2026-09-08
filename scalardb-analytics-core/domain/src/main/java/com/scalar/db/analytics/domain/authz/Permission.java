/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.UUID;

/**
 * Resource-type-specific permissions for access control.
 *
 * <p>Each permission encodes both a resource type and an operation level. There is no ordinal-based
 * inclusion hierarchy between permissions.
 *
 * <p>Each permission has a stable, deterministic UUID derived from its name. This UUID is used as
 * the primary key in the permissions master table and referenced by access control entries.
 */
public enum Permission {
  CATALOG_READ(ResourceType.CATALOG),
  CATALOG_WRITE(ResourceType.CATALOG),
  CATALOG_ADMIN(ResourceType.CATALOG),
  DATA_SOURCE_READ(ResourceType.DATA_SOURCE),
  DATA_SOURCE_ADMIN(ResourceType.DATA_SOURCE),
  NAMESPACE_READ(ResourceType.NAMESPACE),
  TABLE_READ(ResourceType.TABLE);

  private final ResourceType resourceType;
  private final UUID id;

  Permission(ResourceType resourceType) {
    this.resourceType = resourceType;
    this.id = UUID.nameUUIDFromBytes(("permission:" + name()).getBytes(UTF_8));
  }

  /** Returns the resource type associated with this permission. */
  public ResourceType resourceType() {
    return resourceType;
  }

  /** Returns the stable, deterministic UUID for this permission. */
  public UUID id() {
    return id;
  }
}
