/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.UUID;

/** Types of resources that can be protected by access control. */
public enum ResourceType {
  CATALOG,
  DATA_SOURCE,
  NAMESPACE,
  TABLE;

  private final UUID id;

  ResourceType() {
    this.id = UUID.nameUUIDFromBytes(("resource_type:" + name()).getBytes(UTF_8));
  }

  /** Returns the stable, deterministic UUID for this resource type. */
  public UUID id() {
    return id;
  }
}
