/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.schema;

import com.google.common.collect.Streams;
import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.SkippedColumn;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.api.request.schema.NamespaceSchema;
import com.scalar.db.analytics.api.request.schema.TableSchema;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Value;

/**
 * ResolvedSchema is a container class that holds the result of the schema resolution process. It
 * contains a list of ResolvedSchemaEntry objects, each of which contains a Namespace and a list of
 * TableDetail that belong to the namespace, and the source columns that were left out of the
 * resolved tables.
 */
@Value
public class ResolvedSchema {
  List<ResolvedSchemaEntry> entries;
  List<SkippedColumn> skippedColumns;

  public ResolvedSchema(List<ResolvedSchemaEntry> entries, List<SkippedColumn> skippedColumns) {
    this.entries = entries;
    this.skippedColumns = skippedColumns;
  }

  /**
   * Creates a resolved schema in which no source column was skipped.
   *
   * @param entries the resolved namespaces and their tables
   */
  public ResolvedSchema(List<ResolvedSchemaEntry> entries) {
    this(entries, List.of());
  }

  /**
   * Creates the resolved schema of a data source from the schema supplied with its registration
   * request, for a data source whose schema is not read from the source itself. Every supplied
   * column is kept, so no column is skipped.
   *
   * @param dataSourceId the ID of the data source the namespaces belong to
   * @param schema the supplied schema
   * @return the resolved namespaces and their tables, with new IDs
   */
  public static ResolvedSchema fromSupplied(UUID dataSourceId, DataSourceSchema schema) {
    List<ResolvedSchemaEntry> entries =
        schema.getNamespaces().stream()
            .map(ns -> toEntry(dataSourceId, ns))
            .collect(Collectors.toList());
    return new ResolvedSchema(entries);
  }

  private static ResolvedSchemaEntry toEntry(UUID dataSourceId, NamespaceSchema ns) {
    Namespace namespace = Namespace.create(dataSourceId, ns.getNames());
    List<TableDetail> tables =
        ns.getTables().stream()
            .map(t -> toTableDetail(namespace.getId(), t))
            .collect(Collectors.toList());
    return new ResolvedSchemaEntry(namespace, tables);
  }

  private static TableDetail toTableDetail(UUID namespaceId, TableSchema t) {
    TableInfo info = TableInfo.create(namespaceId, t.getName());
    List<Column> columns =
        Streams.mapWithIndex(t.getColumns().stream(), (c, i) -> toColumn(info.getId(), c, (int) i))
            .collect(Collectors.toList());
    return new TableDetail(info, columns);
  }

  private static Column toColumn(UUID tableId, ColumnSchema c, int index) {
    return Column.create(tableId, c.getName(), c.getType(), index, c.isNullable());
  }

  /**
   * Returns the list of Namespace objects that are resolved.
   *
   * @return the list of Namespace objects
   */
  public List<Namespace> getNamespaces() {
    return entries.stream().map(ResolvedSchemaEntry::getNamespace).collect(Collectors.toList());
  }

  /**
   * Returns the list of TableDetail objects that are resolved.
   *
   * @return the list of TableDetail objects
   */
  public List<TableDetail> getTables() {
    return entries.stream()
        .map(ResolvedSchemaEntry::getTables)
        .flatMap(List::stream)
        .collect(Collectors.toList());
  }
}
