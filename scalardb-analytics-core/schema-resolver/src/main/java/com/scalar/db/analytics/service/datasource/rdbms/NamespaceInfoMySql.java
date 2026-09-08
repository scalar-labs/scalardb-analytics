/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource.rdbms;

import com.google.common.collect.Lists;
import java.util.List;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * This class provides the namespace information of a MySQL data source. This includes only the
 * database name because MySQL uses the database as the catalog name and does not have a schema.
 */
@Value
public class NamespaceInfoMySql implements JdbcNamespaceInfo {
  String database;

  @Override
  public List<String> toNamespaceNames() {
    return Lists.newArrayList(database);
  }

  @Override
  public String jdbcCatalog() {
    return database;
  }

  @Nullable
  @Override
  public String jdbcSchema() {
    return null;
  }
}
