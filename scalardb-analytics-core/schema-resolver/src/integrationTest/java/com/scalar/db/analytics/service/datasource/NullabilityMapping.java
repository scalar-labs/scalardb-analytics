/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource;

import lombok.Value;

@Value
public class NullabilityMapping {
  String columnName;
  boolean nullable;

  public String getDisplayName() {
    return String.format("%s should be %s", columnName, nullable ? "nullable" : "not nullable");
  }
}
