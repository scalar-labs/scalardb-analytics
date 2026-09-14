/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.grpc.generated.catalog.v1.Catalog;
import com.scalar.db.analytics.grpc.mapper.CommonArbitraries;
import java.util.UUID;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class CatalogMapperPropertyTest {

  private final CatalogMapper mapper = CatalogMapper.INSTANCE;

  @Property
  void catalog_shouldRoundTripCorrectly(
      @ForAll("uuid") UUID id, @ForAll("catalogName") String name) {
    // Create domain catalog
    com.scalar.db.analytics.api.model.Catalog domainCatalog =
        com.scalar.db.analytics.api.model.Catalog.of(id, name);

    // Domain -> Proto -> Domain
    Catalog proto = mapper.toProto(domainCatalog);
    com.scalar.db.analytics.api.model.Catalog roundTripped = mapper.toDomain(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getId()).isEqualTo(id);
    assertThat(roundTripped.getName()).isEqualTo(name);
  }

  @Property
  void toProto_shouldMapFieldsCorrectly(
      @ForAll("uuid") UUID id, @ForAll("catalogName") String name) {
    // Given
    com.scalar.db.analytics.api.model.Catalog domainCatalog =
        com.scalar.db.analytics.api.model.Catalog.of(id, name);

    // When
    Catalog protoCatalog = mapper.toProto(domainCatalog);

    // Then
    assertThat(protoCatalog).isNotNull();
    assertThat(protoCatalog.getId()).isEqualTo(id.toString());
    assertThat(protoCatalog.getName()).isEqualTo(name);
  }

  @Property
  void toDomain_shouldMapFieldsCorrectly(
      @ForAll("uuid") UUID id, @ForAll("catalogName") String name) {
    // Given
    Catalog protoCatalog = Catalog.newBuilder().setId(id.toString()).setName(name).build();

    // When
    com.scalar.db.analytics.api.model.Catalog domainCatalog = mapper.toDomain(protoCatalog);

    // Then
    assertThat(domainCatalog).isNotNull();
    assertThat(domainCatalog.getId()).isEqualTo(id);
    assertThat(domainCatalog.getName()).isEqualTo(name);
  }

  @Property
  void invalidUuidString_shouldThrowException(
      @ForAll("invalidUuidString") String invalidId, @ForAll("catalogName") String name) {
    // Given
    Catalog protoCatalog = Catalog.newBuilder().setId(invalidId).setName(name).build();

    // When & Then
    assertThatThrownBy(() -> mapper.toDomain(protoCatalog))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Property
  void emptyId_shouldThrowException(@ForAll("catalogName") String name) {
    // Given
    Catalog protoCatalog = Catalog.newBuilder().setId("").setName(name).build();

    // When & Then
    assertThatThrownBy(() -> mapper.toDomain(protoCatalog))
        .isInstanceOf(IllegalArgumentException.class);
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
}
