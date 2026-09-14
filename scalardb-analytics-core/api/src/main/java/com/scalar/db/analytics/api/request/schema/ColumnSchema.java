/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.request.schema;

import com.scalar.db.analytics.api.model.DataType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class ColumnSchema {
  String name;
  DataType type;
  @Builder.Default boolean nullable = true;
}
