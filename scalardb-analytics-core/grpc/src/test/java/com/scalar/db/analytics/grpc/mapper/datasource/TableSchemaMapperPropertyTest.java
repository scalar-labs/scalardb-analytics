/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.api.request.schema.TableSchema;
import com.scalar.db.analytics.grpc.mapper.CommonArbitraries;
import java.util.Arrays;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class TableSchemaMapperPropertyTest {

  private final TableSchemaMapper mapper = TableSchemaMapper.INSTANCE;

  @Property
  void tableSchema_shouldRoundTripCorrectly(
      @ForAll("tableName") String name, @ForAll("columnSchemas") List<ColumnSchema> columns) {
    // Create domain table schema
    TableSchema domainSchema = new TableSchema(name, columns);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.datasource.v1.TableSchema proto =
        mapper.toProto(domainSchema);
    TableSchema roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getName()).isEqualTo(name);
    assertThat(roundTripped.getColumns()).hasSize(columns.size());
  }

  @Example
  void toProto_withNullElementInColumns_shouldSkipNullElements() {
    // Given
    ColumnSchema col1 = new ColumnSchema("col1", DataType.Int.INSTANCE, false);
    List<ColumnSchema> columnsWithNull = Arrays.asList(col1, null);
    TableSchema schema = new TableSchema("table1", columnsWithNull);

    // When
    com.scalar.db.analytics.grpc.generated.datasource.v1.TableSchema proto = mapper.toProto(schema);

    // Then
    assertThat(proto).isNotNull();
    assertThat(proto.getColumnsList()).hasSize(1);
  }

  @Provide
  Arbitrary<String> tableName() {
    return CommonArbitraries.tableName();
  }

  @Provide
  Arbitrary<List<ColumnSchema>> columnSchemas() {
    return Arbitraries.of(
        Arrays.asList(new ColumnSchema("col1", DataType.Int.INSTANCE, false)),
        Arrays.asList(
            new ColumnSchema("col1", DataType.Int.INSTANCE, false),
            new ColumnSchema("col2", DataType.Text.INSTANCE, true)),
        Arrays.asList());
  }
}
