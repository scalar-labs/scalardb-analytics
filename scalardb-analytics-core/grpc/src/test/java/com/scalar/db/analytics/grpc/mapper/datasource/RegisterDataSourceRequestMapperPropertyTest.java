/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterRequest;
import com.scalar.db.analytics.grpc.mapper.CommonArbitraries;
import com.scalar.db.analytics.grpc.mapper.RequestMapper;
import java.util.Map;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.junit.jupiter.api.Test;

class RegisterDataSourceRequestMapperPropertyTest {

  private final RequestMapper mapper = RequestMapper.INSTANCE;
  private final DataSourceProviderJsonMapper providerMapper = DataSourceProviderJsonMapper.INSTANCE;
  private final DataSourceSchemaJsonMapper schemaMapper = DataSourceSchemaJsonMapper.INSTANCE;

  @Property
  void registerDataSourceRequest_shouldRoundTripCorrectly(
      @ForAll("catalogName") String catalogName,
      @ForAll("dataSourceName") String dataSourceName,
      @ForAll("configs") Map<String, String> configs) {
    ScalarDbProvider provider = new ScalarDbProvider(configs);
    RegisterDataSourceRequest domainRequest =
        new RegisterDataSourceRequest(catalogName, dataSourceName, provider, null);

    RegisterRequest proto = mapper.toProtoRegisterRequest(domainRequest);

    assertThat(proto).isNotNull();
    assertThat(proto.getDataSourceName()).isEqualTo(dataSourceName);
    assertThat(proto.getProviderPayloadJson()).isEqualTo(providerMapper.toJson(provider));
    assertThat(proto.hasSchemaJson()).isFalse();

    RegisterDataSourceRequest roundTripped = mapper.toDomainRegisterDataSourceRequest(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getName()).isEqualTo(dataSourceName);
    assertThat(roundTripped.getProviderType()).isEqualTo(provider.getType());
    assertThat(roundTripped.getProvider()).isInstanceOf(ScalarDbProvider.class);
    ScalarDbProvider roundTrippedProvider = (ScalarDbProvider) roundTripped.getProvider();
    assertThat(roundTrippedProvider.getConfigs()).isEqualTo(configs);
    assertThat(roundTripped.getSchema()).isNull();
  }

  @Property
  void registerDataSourceRequest_withSchema_shouldRoundTripCorrectly(
      @ForAll("catalogName") String catalogName,
      @ForAll("dataSourceName") String dataSourceName,
      @ForAll("region") String region,
      @ForAll("schema") DataSourceSchema schema) {
    DynamoDbProvider provider = DynamoDbProvider.builder().region(region).build();
    RegisterDataSourceRequest domainRequest =
        new RegisterDataSourceRequest(catalogName, dataSourceName, provider, schema);

    RegisterRequest proto = mapper.toProtoRegisterRequest(domainRequest);

    assertThat(proto).isNotNull();
    assertThat(proto.getDataSourceName()).isEqualTo(dataSourceName);
    assertThat(proto.getProviderPayloadJson()).isEqualTo(providerMapper.toJson(provider));
    assertThat(proto.hasSchemaJson()).isTrue();
    assertThat(proto.getSchemaJson()).isEqualTo(schemaMapper.toJson(schema));

    RegisterDataSourceRequest roundTripped = mapper.toDomainRegisterDataSourceRequest(proto);

    assertThat(roundTripped).isNotNull();
    assertThat(roundTripped.getSchema()).isNotNull();
    assertThat(roundTripped.getSchema().getNamespaces()).isEqualTo(schema.getNamespaces());
    assertThat(roundTripped.getProvider()).isInstanceOf(DynamoDbProvider.class);
  }

  @Property
  void null_shouldMapToNull() {
    assertThat(mapper.toProtoRegisterRequest(null)).isNull();
    assertThat(mapper.toDomainRegisterDataSourceRequest(null)).isNull();
  }

  @Test
  void fromJson_withNull_shouldReturnNull() {
    assertThat(schemaMapper.fromJson(null)).isNull();
  }

  @Test
  void fromJson_withBlank_shouldThrowException() {
    assertThatThrownBy(() -> schemaMapper.fromJson(" "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void fromJson_withJsonNullLiteral_shouldThrowException() {
    assertThatThrownBy(() -> schemaMapper.fromJson("null"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Provide
  Arbitrary<String> catalogName() {
    return CommonArbitraries.catalogName();
  }

  @Provide
  Arbitrary<String> dataSourceName() {
    return CommonArbitraries.systemIdentifier(100);
  }

  @Provide
  Arbitrary<Map<String, String>> configs() {
    return net.jqwik.api.Arbitraries.maps(
            net.jqwik.api.Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('.', '-', '_')
                .ofMinLength(1)
                .ofMaxLength(50),
            net.jqwik.api.Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('/', '-', '_', '.', ':')
                .ofMinLength(1)
                .ofMaxLength(255))
        .ofMinSize(1)
        .ofMaxSize(20);
  }

  @Provide
  Arbitrary<String> region() {
    return net.jqwik.api.Arbitraries.strings()
        .withCharRange('a', 'z')
        .ofMinLength(2)
        .ofMaxLength(20);
  }

  @Provide
  Arbitrary<DataSourceSchema> schema() {
    return SchemaArbitraries.dataSourceSchema();
  }
}
