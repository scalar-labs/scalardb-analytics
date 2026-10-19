/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import java.util.List;
import lombok.Value;

/**
 * A source column that was left out of a registered table because its source type is not supported.
 */
@Value
public class SkippedColumn {
  List<String> namespaceNames;
  String tableName;
  String columnName;

  /** The type name reported by the source, for example "json". */
  String sourceTypeName;

  /** Why the column was skipped. */
  String reason;
}
