/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import java.util.List;
import lombok.Value;

/** The result of registering a data source. */
@Value
public class RegisterDataSourceResult {
  DataSource dataSource;

  /** The source columns that were left out of the registered tables. Empty if none were. */
  List<SkippedColumn> skippedColumns;
}
