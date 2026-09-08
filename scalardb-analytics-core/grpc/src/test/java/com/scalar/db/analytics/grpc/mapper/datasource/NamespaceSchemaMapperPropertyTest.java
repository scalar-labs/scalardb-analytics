/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.api.request.schema.NamespaceSchema;
import com.scalar.db.analytics.api.request.schema.TableSchema;
import java.util.Arrays;
import java.util.List;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class NamespaceSchemaMapperPropertyTest {

  private final NamespaceSchemaMapper mapper = NamespaceSchemaMapper.INSTANCE;

  @Property
  void namespaceSchema_shouldRoundTripCorrectly(
      @ForAll("namespaceNames") List<String> names,
      @ForAll("tableSchemas") List<TableSchema> tables) {
    // Create domain namespace schema
    NamespaceSchema domainSchema = new NamespaceSchema(names, tables);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.datasource.v1.NamespaceSchema proto =
        mapper.toProto(domainSchema);
    NamespaceSchema roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getNames()).containsExactlyElementsOf(names);
    assertThat(roundTripped.getTables()).hasSize(tables.size());
  }

  @Example
  void toProto_withNullElementInNames_shouldSkipNullElements() {
    // Given
    List<String> namesWithNull = Arrays.asList("schema1", null, "schema2");
    NamespaceSchema schema = new NamespaceSchema(namesWithNull, Arrays.asList());

    // When
    com.scalar.db.analytics.grpc.generated.datasource.v1.NamespaceSchema proto =
        mapper.toProto(schema);

    // Then
    assertThat(proto).isNotNull();
    assertThat(proto.getNamesList()).containsExactly("schema1", "schema2");
  }

  @Example
  void toProto_withNullElementInTables_shouldSkipNullElements() {
    // Given
    TableSchema table1 = new TableSchema("table1", Arrays.asList());
    List<TableSchema> tablesWithNull = Arrays.asList(table1, null);
    NamespaceSchema schema = new NamespaceSchema(Arrays.asList("schema1"), tablesWithNull);

    // When
    com.scalar.db.analytics.grpc.generated.datasource.v1.NamespaceSchema proto =
        mapper.toProto(schema);

    // Then
    assertThat(proto).isNotNull();
    assertThat(proto.getTablesList()).hasSize(1);
  }

  @Provide
  Arbitrary<List<String>> namespaceNames() {
    return Arbitraries.of(
        Arrays.asList("schema1"), Arrays.asList("schema1", "schema2"), Arrays.asList());
  }

  @Provide
  Arbitrary<List<TableSchema>> tableSchemas() {
    return Arbitraries.of(
        Arrays.asList(
            new TableSchema(
                "table1", Arrays.asList(new ColumnSchema("col1", DataType.Int.INSTANCE, false)))),
        Arrays.asList(
            new TableSchema("table1", Arrays.asList()), new TableSchema("table2", Arrays.asList())),
        Arrays.asList());
  }
}
