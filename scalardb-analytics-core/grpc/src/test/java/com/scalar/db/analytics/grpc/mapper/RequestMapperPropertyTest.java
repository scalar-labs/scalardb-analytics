/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceProviderJsonMapper;
import java.util.Map;
import java.util.Objects;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class RequestMapperPropertyTest {

  private final RequestMapper mapper = RequestMapper.INSTANCE;
  private final DataSourceProviderJsonMapper providerMapper = DataSourceProviderJsonMapper.INSTANCE;

  @Property
  void registerDataSourceRequest_shouldRoundTripCorrectly(
      @ForAll("catalogName") String catalogName,
      @ForAll("dataSourceName") String dataSourceName,
      @ForAll("configs") Map<String, String> configs) {
    ScalarDbProvider provider = ScalarDbProvider.builder().configs(configs).build();
    RegisterDataSourceRequest domainRequest =
        new RegisterDataSourceRequest(catalogName, dataSourceName, provider, null);

    com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterRequest proto =
        Objects.requireNonNull(mapper.toProtoRegisterRequest(domainRequest));
    assertThat(proto.getProviderPayloadJson()).isEqualTo(providerMapper.toJson(provider));
    assertThat(proto.hasSchemaJson()).isFalse();

    RegisterDataSourceRequest roundTripped = mapper.toDomainRegisterDataSourceRequest(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getCatalogName()).isEqualTo(catalogName);
    assertThat(roundTripped.getName()).isEqualTo(dataSourceName);
    assertThat(roundTripped.getProviderType()).isEqualTo(provider.getType());
    assertThat(roundTripped.getProvider()).isInstanceOf(ScalarDbProvider.class);
    assertThat(((ScalarDbProvider) roundTripped.getProvider()).getConfigs()).isEqualTo(configs);
    assertThat(roundTripped.getSchema()).isNull();
  }

  @Provide
  Arbitrary<String> catalogName() {
    return CommonArbitraries.catalogName();
  }

  @Provide
  Arbitrary<String> dataSourceName() {
    return CommonArbitraries.catalogName();
  }

  @Provide
  Arbitrary<Map<String, String>> configs() {
    return Arbitraries.of(
        createConfigMap("cassandra", "localhost"),
        createConfigMap("jdbc", "localhost:3306"),
        createConfigMap("dynamodb", "localhost:8000"));
  }

  private Map<String, String> createConfigMap(String storage, String contactPoints) {
    Map<String, String> map = new java.util.HashMap<>();
    map.put("scalar.db.storage", storage);
    map.put("scalar.db.contact_points", contactPoints);
    return map;
  }
}
