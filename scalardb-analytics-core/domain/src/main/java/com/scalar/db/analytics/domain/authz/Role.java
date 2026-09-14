/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import java.util.Objects;
import java.util.UUID;

/** A named role that can be assigned to users and granted permissions on resources. */
public record Role(UUID id, String name, boolean builtIn) {

  public Role {
    Objects.requireNonNull(id, "id must not be null");
    Objects.requireNonNull(name, "name must not be null");
    if (name.isBlank()) {
      throw new IllegalArgumentException("name must not be blank");
    }
  }

  public static Role create(String name) {
    return new Role(UUID.randomUUID(), name, false);
  }
}
