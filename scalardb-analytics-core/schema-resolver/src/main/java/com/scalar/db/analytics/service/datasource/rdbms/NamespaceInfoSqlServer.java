/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Value;

/** This class provides the namespace information of an SQL Server data source. */
@Value
public class NamespaceInfoSqlServer implements JdbcNamespaceInfo {
  String catalog;
  String schema;

  @Override
  public List<String> toNamespaceNames() {
    return Lists.newArrayList(catalog, schema);
  }

  @Override
  public String jdbcCatalog() {
    return catalog;
  }

  @Override
  public String jdbcSchema() {
    return schema;
  }
}
