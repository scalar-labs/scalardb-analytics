/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.datasource;

import com.google.common.collect.Streams;
import com.scalar.db.analytics.api.model.Column;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.api.request.schema.NamespaceSchema;
import com.scalar.db.analytics.api.request.schema.TableSchema;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * A no-op implementation of {@link SchemaResolver} that returns the schema converted from the given
 * simplified schema, that is {@link DataSourceSchema}. This implementation is intended to be used
 * for data sources where we cannot resolve the schema automatically.
 */
public class NoOpSchemaResolver implements SchemaResolver {
  private final UUID dataSourceId;
  private final DataSourceSchema schema;

  public NoOpSchemaResolver(UUID dataSourceId, DataSourceSchema schema) {
    this.dataSourceId = dataSourceId;
    this.schema = schema;
  }

  @Override
  public ResolvedSchema resolveSchema() throws SchemaResolverException {
    return convertSchema(schema);
  }

  private ResolvedSchema convertSchema(DataSourceSchema schema) {
    List<ResolvedSchemaEntry> entries =
        schema.getNamespaces().stream()
            .map(this::convertToResolvedSchemaEntry)
            .collect(Collectors.toList());
    return new ResolvedSchema(entries);
  }

  private ResolvedSchemaEntry convertToResolvedSchemaEntry(NamespaceSchema ns) {
    Namespace namespace = convertToNamespace(ns);
    List<TableDetail> tables =
        ns.getTables().stream()
            .map(t -> convertToTableDetail(namespace.getId(), t))
            .collect(Collectors.toList());
    return new ResolvedSchemaEntry(namespace, tables);
  }

  private Namespace convertToNamespace(NamespaceSchema ns) {
    return Namespace.create(dataSourceId, ns.getNames());
  }

  private TableDetail convertToTableDetail(UUID namespaceId, TableSchema t) {
    TableInfo info = TableInfo.create(namespaceId, t.getName());
    List<Column> columns =
        Streams.mapWithIndex(
                t.getColumns().stream(), (c, i) -> convertToColumn(info.getId(), c, (int) i))
            .collect(Collectors.toList());
    return new TableDetail(info, columns);
  }

  private Column convertToColumn(UUID tableId, ColumnSchema c, int index) {
    return Column.create(tableId, c.getName(), c.getType(), index, c.isNullable());
  }
}
