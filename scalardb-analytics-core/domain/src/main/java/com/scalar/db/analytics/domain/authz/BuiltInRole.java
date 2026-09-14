/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.UUID;

/** Constants for built-in roles that are seeded at startup. */
public final class BuiltInRole {

  public static final String SUPERADMIN_NAME = "SUPERADMIN";

  public static final UUID SUPERADMIN_ID =
      UUID.nameUUIDFromBytes(("built_in_role:" + SUPERADMIN_NAME).getBytes(UTF_8));

  private BuiltInRole() {}

  /** Returns a new {@link Role} instance representing the SUPERADMIN built-in role. */
  public static Role superadmin() {
    return new Role(SUPERADMIN_ID, SUPERADMIN_NAME, true);
  }
}
