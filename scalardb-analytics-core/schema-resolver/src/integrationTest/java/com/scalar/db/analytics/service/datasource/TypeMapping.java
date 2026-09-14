/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource;

import com.scalar.db.analytics.api.model.DataType;
import lombok.Value;

@Value
public class TypeMapping {
  String columnName;
  DataType dataType;

  public String getDisplayName() {
    return String.format("%s should be mapped to %s", columnName, dataType);
  }
}
