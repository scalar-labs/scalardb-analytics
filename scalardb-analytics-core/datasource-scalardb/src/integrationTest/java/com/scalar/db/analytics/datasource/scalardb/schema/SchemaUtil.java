/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.datasource.scalardb.schema;

import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.service.datasource.ResolvedSchema;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SchemaUtil {
  public static Map<List<String>, Namespace> getNamespacesMap(ResolvedSchema result) {
    return result.getNamespaces().stream().collect(Collectors.toMap(Namespace::getNames, ns -> ns));
  }

  public static Map<String, TableDetail> getTablesMap(ResolvedSchema result) {
    return result.getTables().stream()
        .collect(Collectors.toMap(t -> t.getInfo().getName(), t -> t));
  }

  public static Map<String, Column> getColumnsMap(TableDetail table) {
    return table.getColumns().stream().collect(Collectors.toMap(Column::getName, c -> c));
  }
}
