/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.grpc.mapper.CommonArbitraries;
import java.util.Collections;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class DataSourceMapperPropertyTest {

  private final DataSourceMapper mapper = DataSourceMapper.INSTANCE;
  private final DataSourceProviderJsonMapper providerMapper = DataSourceProviderJsonMapper.INSTANCE;

  @Property
  void dataSource_shouldRoundTripCorrectly(
      @ForAll("uuid") UUID id,
      @ForAll("uuid") UUID catalogId,
      @ForAll("dataSourceName") String name,
      @ForAll("provider") DataSourceProvider provider) {
    DataSource domainDataSource = new DataSource(id, catalogId, name, provider);

    com.scalar.db.analytics.grpc.generated.datasource.v1.DataSource proto =
        mapper.toProto(domainDataSource);
    assertThat(proto.getProviderPayloadJson()).isEqualTo(providerMapper.toJson(provider));

    DataSource roundTripped = mapper.toDomain(proto);
    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getId()).isEqualTo(id);
    assertThat(roundTripped.getCatalogId()).isEqualTo(catalogId);
    assertThat(roundTripped.getName()).isEqualTo(name);
    assertThat(roundTripped.getProvider()).isEqualTo(provider);
  }

  @Property
  void toDomain_withInvalidUuidForId_shouldThrowException(
      @ForAll("invalidUuidString") String invalidId,
      @ForAll("uuid") UUID catalogId,
      @ForAll("dataSourceName") String name,
      @ForAll("provider") DataSourceProvider provider) {
    com.scalar.db.analytics.grpc.generated.datasource.v1.DataSource proto =
        com.scalar.db.analytics.grpc.generated.datasource.v1.DataSource.newBuilder()
            .setId(invalidId)
            .setCatalogId(catalogId.toString())
            .setName(name)
            .setProviderPayloadJson(providerMapper.toJson(provider))
            .build();

    assertThatThrownBy(() -> mapper.toDomain(proto)).isInstanceOf(IllegalArgumentException.class);
  }

  @Property
  void toDomain_withInvalidUuidForCatalogId_shouldThrowException(
      @ForAll("uuid") UUID id,
      @ForAll("invalidUuidString") String invalidCatalogId,
      @ForAll("dataSourceName") String name,
      @ForAll("provider") DataSourceProvider provider) {
    com.scalar.db.analytics.grpc.generated.datasource.v1.DataSource proto =
        com.scalar.db.analytics.grpc.generated.datasource.v1.DataSource.newBuilder()
            .setId(id.toString())
            .setCatalogId(invalidCatalogId)
            .setName(name)
            .setProviderPayloadJson(providerMapper.toJson(provider))
            .build();

    assertThatThrownBy(() -> mapper.toDomain(proto)).isInstanceOf(IllegalArgumentException.class);
  }

  @Provide
  Arbitrary<UUID> uuid() {
    return CommonArbitraries.uuid();
  }

  @Provide
  Arbitrary<String> dataSourceName() {
    return CommonArbitraries.catalogName();
  }

  @Provide
  Arbitrary<String> invalidUuidString() {
    return CommonArbitraries.invalidUuidString();
  }

  @Provide
  Arbitrary<DataSourceProvider> provider() {
    return Arbitraries.of(
        ScalarDbProvider.builder()
            .configs(Collections.singletonMap("scalar.db.storage", "cassandra"))
            .build(),
        PostgreSql.builder()
            .host("localhost")
            .port(5432)
            .username("user")
            .password("password")
            .database("db")
            .build());
  }
}
