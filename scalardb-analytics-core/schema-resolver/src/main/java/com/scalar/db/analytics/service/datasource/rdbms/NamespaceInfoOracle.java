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
 * This class provides the namespace information of an Oracle data source. This includes only the
 * schema name because Oracle does not have a catalog name.
 */
@Value
public class NamespaceInfoOracle implements JdbcNamespaceInfo {
  String schema;

  @Override
  public List<String> toNamespaceNames() {
    return Lists.newArrayList(schema);
  }

  @Override
  @Nullable
  public String jdbcCatalog() {
    return null;
  }

  @Override
  public String jdbcSchema() {
    return schema;
  }
}
