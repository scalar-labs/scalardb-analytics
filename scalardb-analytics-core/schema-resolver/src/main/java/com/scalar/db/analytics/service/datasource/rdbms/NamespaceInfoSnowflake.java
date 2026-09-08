/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Value;

/** This class provides the namespace information of a Snowflake data source. */
@Value
public class NamespaceInfoSnowflake implements JdbcNamespaceInfo {
  String database;
  String schema;

  @Override
  public List<String> toNamespaceNames() {
    return Lists.newArrayList(database, schema);
  }

  @Override
  public String jdbcCatalog() {
    return database;
  }

  @Override
  public String jdbcSchema() {
    return schema;
  }
}
