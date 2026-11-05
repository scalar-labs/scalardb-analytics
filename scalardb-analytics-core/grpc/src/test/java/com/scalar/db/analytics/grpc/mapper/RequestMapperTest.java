/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.entry;

import com.scalar.db.analytics.api.codec.provider.ProviderCodecException;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterRequest;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceProviderJsonMapper;
import org.junit.jupiter.api.Test;

class RequestMapperTest {

  private final RequestMapper mapper = RequestMapper.INSTANCE;

  @Test
  void toDomainRegisterDataSourceRequest_withEmptyProvider_shouldThrowRequired() {
    AnalyticsException e = decodeFailure(request().clearProviderPayloadJson().build());

    assertInvalidField(e, "provider_payload_json", "required");
    assertThat(e.getCause()).isNull();
  }

  @Test
  void toDomainRegisterDataSourceRequest_withBlankProvider_shouldThrowRequired() {
    AnalyticsException e = decodeFailure(request().setProviderPayloadJson("  ").build());

    assertInvalidField(e, "provider_payload_json", "required");
  }

  @Test
  void toDomainRegisterDataSourceRequest_withMalformedProviderJson_shouldThrowInvalid() {
    AnalyticsException e = decodeFailure(request().setProviderPayloadJson("{").build());

    assertInvalidField(e, "provider_payload_json", "invalid");
    assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void toDomainRegisterDataSourceRequest_withUnknownProviderType_shouldThrowInvalid() {
    AnalyticsException e =
        decodeFailure(request().setProviderPayloadJson("{\"type\":\"unknown\"}").build());

    assertInvalidField(e, "provider_payload_json", "invalid");
  }

  @Test
  void toDomainRegisterDataSourceRequest_withProviderRejectedByItsBuilder_shouldThrowInvalid() {
    // DynamoDbProvider.Builder requires a region or an endpoint.
    AnalyticsException e =
        decodeFailure(request().setProviderPayloadJson("{\"type\":\"dynamodb\"}").build());

    assertInvalidField(e, "provider_payload_json", "invalid");
    assertThat(e.getCause()).isInstanceOf(ProviderCodecException.class);
  }

  @Test
  void toDomainRegisterDataSourceRequest_withProviderFieldOfWrongType_shouldThrowInvalid() {
    AnalyticsException e =
        decodeFailure(
            request()
                .setProviderPayloadJson("{\"type\":\"dynamodb\",\"region\":{\"a\":1}}")
                .build());

    assertInvalidField(e, "provider_payload_json", "invalid");
  }

  @Test
  void toDomainRegisterDataSourceRequest_shouldNotEchoThePayload() {
    String secret = "s3cr3t-value";
    AnalyticsException e =
        decodeFailure(request().setProviderPayloadJson("[\"" + secret + "\"]").build());

    assertInvalidField(e, "provider_payload_json", "invalid");
    assertThat(e.getMessage()).doesNotContain(secret);
    assertThat(e.getMetadata().values()).noneMatch(v -> v.contains(secret));
  }

  @Test
  void toDomainRegisterDataSourceRequest_withEmptySchema_shouldThrowInvalid() {
    AnalyticsException e = decodeFailure(request().setSchemaJson("").build());

    assertInvalidField(e, "schema_json", "invalid");
  }

  @Test
  void toDomainRegisterDataSourceRequest_withUnknownColumnType_shouldThrowInvalid() {
    String schema =
        "{\"namespaces\":[{\"names\":[\"ns\"],\"tables\":[{\"name\":\"t\",\"columns\":"
            + "[{\"name\":\"c\",\"type\":\"no_such_type\"}]}]}]}";
    AnalyticsException e = decodeFailure(request().setSchemaJson(schema).build());

    assertInvalidField(e, "schema_json", "invalid");
  }

  private AnalyticsException decodeFailure(RegisterRequest request) {
    AnalyticsException e =
        catchThrowableOfType(
            AnalyticsException.class, () -> mapper.toDomainRegisterDataSourceRequest(request));
    assertThat(e).isNotNull();
    return e;
  }

  private static void assertInvalidField(AnalyticsException e, String field, String reason) {
    assertThat(e.getErrorCode()).isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT);
    assertThat(e.getMetadata())
        .containsOnly(
            entry("catalog_name", "cat"),
            entry("data_source_name", "ds"),
            entry("field", field),
            entry("reason", reason));
  }

  private static RegisterRequest.Builder request() {
    return RegisterRequest.newBuilder()
        .setCatalogName("cat")
        .setDataSourceName("ds")
        .setProviderPayloadJson(
            DataSourceProviderJsonMapper.INSTANCE.toJson(
                DynamoDbProvider.builder().region("us-east-1").build()));
  }
}
