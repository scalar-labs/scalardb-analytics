/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.authz;

import lombok.Value;

/** A named role that can be assigned to users and granted permissions on resources. */
@Value
public class Role {
  String id;
  String name;
  boolean builtIn;
}
