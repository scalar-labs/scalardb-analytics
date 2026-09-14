/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.model;

import java.util.List;
import lombok.Value;

/**
 * DataSourceNamespaceTable is a value object that holds Table along with Namespace and DataSource
 * that the table belongs to.
 */
@Value
public class DataSourceNamespaceTable {
  DataSource dataSource;
  Namespace namespace;
  Table table;

  public DataSourceNamespaceTableDetail toDetail(List<Column> columns) {
    return new DataSourceNamespaceTableDetail(dataSource, namespace, table.toDetail(columns));
  }

  public DataSourceNamespace toDataSourceNamespace() {
    return new DataSourceNamespace(dataSource, namespace);
  }
}
