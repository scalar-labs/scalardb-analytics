/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

/** Source of an effective permission: directly granted or inherited via a role. */
public enum PermissionSource {
  DIRECT,
  VIA_ROLE
}
