/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import java.io.Serializable;
import java.util.List;
import lombok.Value;

@Value
public class TableDetail implements Serializable {
  private static final long serialVersionUID = 1L;

  TableInfo info;
  List<Column> columns;

  public Table toTable() {
    return new Table(info);
  }
}
