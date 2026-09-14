/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.api.request.schema.NamespaceSchema;
import com.scalar.db.analytics.api.request.schema.TableSchema;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;

final class SchemaArbitraries {

  private SchemaArbitraries() {}

  static Arbitrary<DataSourceSchema> dataSourceSchema() {
    return namespaceSchemas()
        .map(namespaces -> DataSourceSchema.builder().namespaces(namespaces).build());
  }

  private static Arbitrary<List<NamespaceSchema>> namespaceSchemas() {
    return namespaceSchema().list().ofMinSize(1).ofMaxSize(3);
  }

  private static Arbitrary<NamespaceSchema> namespaceSchema() {
    return Combinators.combine(namespaceNameList(), tableSchemas())
        .as((names, tables) -> NamespaceSchema.builder().names(names).tables(tables).build());
  }

  private static Arbitrary<List<String>> namespaceNameList() {
    return identifier().list().ofMinSize(1).ofMaxSize(3);
  }

  private static Arbitrary<List<TableSchema>> tableSchemas() {
    return tableSchema().list().ofMinSize(1).ofMaxSize(3);
  }

  private static Arbitrary<TableSchema> tableSchema() {
    return Combinators.combine(identifier(), columnSchemas())
        .as((name, columns) -> TableSchema.builder().name(name).columns(columns).build());
  }

  private static Arbitrary<List<ColumnSchema>> columnSchemas() {
    return columnSchema().list().ofMinSize(1).ofMaxSize(4);
  }

  private static Arbitrary<ColumnSchema> columnSchema() {
    return Combinators.combine(
            identifier(), dataType(), Arbitraries.of(Boolean.TRUE, Boolean.FALSE))
        .as(
            (name, type, nullable) ->
                ColumnSchema.builder().name(name).type(type).nullable(nullable).build());
  }

  private static Arbitrary<DataType> dataType() {
    return Arbitraries.of(
        DataType.Int.INSTANCE,
        DataType.BigInt.INSTANCE,
        DataType.Text.INSTANCE,
        DataType.Blob.INSTANCE,
        DataType.Decimal.builder().precision(10).scale(2).build());
  }

  private static Arbitrary<String> identifier() {
    return Arbitraries.strings().alpha().ofMinLength(1).ofMaxLength(20);
  }
}
