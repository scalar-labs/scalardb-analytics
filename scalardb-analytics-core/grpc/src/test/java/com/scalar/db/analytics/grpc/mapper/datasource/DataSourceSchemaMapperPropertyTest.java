/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
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

class DataSourceSchemaMapperPropertyTest {

  private final DataSourceSchemaMapper mapper = DataSourceSchemaMapper.INSTANCE;

  @Property
  void dataSourceSchema_shouldRoundTripCorrectly(
      @ForAll("namespaceSchemas") List<NamespaceSchema> namespaces) {
    // Create domain data source schema
    DataSourceSchema domainSchema = new DataSourceSchema(namespaces);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.datasource.v1.DataSourceSchema proto =
        mapper.toProto(domainSchema);
    DataSourceSchema roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getNamespaces()).hasSize(namespaces.size());
  }

  @Example
  void toProto_withNullElementInNamespaces_shouldSkipNullElements() {
    // Given
    NamespaceSchema ns1 = new NamespaceSchema(Arrays.asList("schema1"), Arrays.asList());
    List<NamespaceSchema> namespacesWithNull = Arrays.asList(ns1, null);
    DataSourceSchema schema = new DataSourceSchema(namespacesWithNull);

    // When
    com.scalar.db.analytics.grpc.generated.datasource.v1.DataSourceSchema proto =
        mapper.toProto(schema);

    // Then
    assertThat(proto).isNotNull();
    assertThat(proto.getNamespacesList()).hasSize(1);
  }

  @Provide
  Arbitrary<List<NamespaceSchema>> namespaceSchemas() {
    return Arbitraries.of(
        Arrays.asList(
            new NamespaceSchema(
                Arrays.asList("schema1"),
                Arrays.asList(
                    new TableSchema(
                        "table1",
                        Arrays.asList(new ColumnSchema("col1", DataType.Int.INSTANCE, false)))))),
        Arrays.asList(
            new NamespaceSchema(Arrays.asList("schema1"), Arrays.asList()),
            new NamespaceSchema(Arrays.asList("schema2"), Arrays.asList())),
        Arrays.asList());
  }
}
