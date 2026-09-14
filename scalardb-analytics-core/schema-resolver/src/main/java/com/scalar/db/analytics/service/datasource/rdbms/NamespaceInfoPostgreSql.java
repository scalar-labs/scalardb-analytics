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
 * This class provides the namespace information of a PostgreSQL data source.
 *
 * <p>This includes only the schema name because PostgreSQL does not have a catalog name.
 */
@Value
public class NamespaceInfoPostgreSql implements JdbcNamespaceInfo {
  String schema;

  @Override
  public List<String> toNamespaceNames() {
    return Lists.newArrayList(schema);
  }

  @Nullable
  @Override
  public String jdbcCatalog() {
    return null;
  }

  @Override
  public String jdbcSchema() {
    return schema;
  }
}
