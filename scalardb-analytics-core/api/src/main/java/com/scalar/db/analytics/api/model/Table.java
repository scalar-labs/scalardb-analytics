/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import java.util.List;
import lombok.Value;

@Value
public class Table {
  TableInfo info;

  public TableDetail toDetail(List<Column> columns) {
    return new TableDetail(info, columns);
  }
}
