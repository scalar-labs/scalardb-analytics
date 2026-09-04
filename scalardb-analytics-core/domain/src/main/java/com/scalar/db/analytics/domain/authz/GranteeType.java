/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

/** Identifies the type of entity receiving an access control grant. */
public enum GranteeType {
  USER,
  ROLE
}
