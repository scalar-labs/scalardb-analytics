/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.namespace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.grpc.mapper.CommonArbitraries;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class NamespaceMapperPropertyTest {

  private final NamespaceMapper mapper = NamespaceMapper.INSTANCE;

  @Property
  void namespace_shouldRoundTripCorrectly(
      @ForAll("uuid") UUID id,
      @ForAll("uuid") UUID dataSourceId,
      @ForAll("namespaceNames") List<String> names) {
    // Create domain namespace
    Namespace domainNamespace = new Namespace(id, dataSourceId, names);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.namespace.v1.Namespace proto =
        mapper.toProto(domainNamespace);
    Namespace roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getId()).isEqualTo(id);
    assertThat(roundTripped.getDataSourceId()).isEqualTo(dataSourceId);
    assertThat(roundTripped.getNames()).containsExactlyElementsOf(names);
  }

  @Property
  void dataSourceNamespace_shouldRoundTripCorrectly(
      @ForAll("uuid") UUID dataSourceId,
      @ForAll("uuid") UUID catalogId,
      @ForAll("catalogName") String dataSourceName,
      @ForAll("uuid") UUID namespaceId,
      @ForAll("namespaceNames") List<String> names) {
    // Create domain objects
    DataSource dataSource =
        new DataSource(
            dataSourceId,
            catalogId,
            dataSourceName,
            ScalarDbProvider.builder()
                .configs(Collections.singletonMap("scalar.db.storage", "cassandra"))
                .build());
    Namespace namespace = new Namespace(namespaceId, dataSourceId, names);
    DataSourceNamespace domainDsNs = new DataSourceNamespace(dataSource, namespace);

    // Domain -> Proto -> Domain
    com.scalar.db.analytics.grpc.generated.namespace.v1.DataSourceNamespace proto =
        mapper.toProto(domainDsNs);
    DataSourceNamespace roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getDataSource()).isNotNull();
    assertThat(roundTripped.getNamespace()).isNotNull();
    assertThat(roundTripped.getNamespace().getId()).isEqualTo(namespaceId);
    assertThat(roundTripped.getNamespace().getNames()).containsExactlyElementsOf(names);
  }

  @Example
  void toProto_withNullElementInNames_shouldSkipNullElements() {
    // Given
    UUID id = UUID.randomUUID();
    UUID dataSourceId = UUID.randomUUID();
    List<String> namesWithNull = Arrays.asList("schema1", null, "schema2");
    Namespace namespace = new Namespace(id, dataSourceId, namesWithNull);

    // When
    com.scalar.db.analytics.grpc.generated.namespace.v1.Namespace proto = mapper.toProto(namespace);

    // Then
    assertThat(proto).isNotNull();
    assertThat(proto.getNamesList()).containsExactly("schema1", "schema2");
  }

  @Property
  void toDomain_namespace_withInvalidUuidForId_shouldThrowException(
      @ForAll("invalidUuidString") String invalidId,
      @ForAll("uuid") UUID dataSourceId,
      @ForAll("namespaceNames") List<String> names) {
    // Given
    com.scalar.db.analytics.grpc.generated.namespace.v1.Namespace proto =
        com.scalar.db.analytics.grpc.generated.namespace.v1.Namespace.newBuilder()
            .setId(invalidId)
            .setDataSourceId(dataSourceId.toString())
            .addAllNames(names)
            .build();

    // When & Then
    assertThatThrownBy(() -> mapper.toDomain(proto)).isInstanceOf(IllegalArgumentException.class);
  }

  @Property
  void toDomain_namespace_withInvalidUuidForDataSourceId_shouldThrowException(
      @ForAll("uuid") UUID id,
      @ForAll("invalidUuidString") String invalidDataSourceId,
      @ForAll("namespaceNames") List<String> names) {
    // Given
    com.scalar.db.analytics.grpc.generated.namespace.v1.Namespace proto =
        com.scalar.db.analytics.grpc.generated.namespace.v1.Namespace.newBuilder()
            .setId(id.toString())
            .setDataSourceId(invalidDataSourceId)
            .addAllNames(names)
            .build();

    // When & Then
    assertThatThrownBy(() -> mapper.toDomain(proto)).isInstanceOf(IllegalArgumentException.class);
  }

  @Provide
  Arbitrary<UUID> uuid() {
    return CommonArbitraries.uuid();
  }

  @Provide
  Arbitrary<String> catalogName() {
    return CommonArbitraries.catalogName();
  }

  @Provide
  Arbitrary<String> invalidUuidString() {
    return CommonArbitraries.invalidUuidString();
  }

  @Provide
  Arbitrary<List<String>> namespaceNames() {
    return Arbitraries.<List<String>>of(
            Arrays.asList("schema1"),
            Arrays.asList("schema1", "schema2"),
            Arrays.asList("ns1", "ns2", "ns3"),
            Arrays.asList())
        .injectDuplicates(0.1);
  }
}
