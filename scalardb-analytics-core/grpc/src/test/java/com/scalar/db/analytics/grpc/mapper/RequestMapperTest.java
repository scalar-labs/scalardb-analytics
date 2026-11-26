/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.entry;
import static org.assertj.core.api.Assertions.tuple;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.scalar.db.analytics.api.codec.provider.ProviderCodecException;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.InitializeCatalogRequest;
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
    // Jackson wraps the builder's rejection, which is how it is told apart from a codec failure.
    assertThat(e.getCause())
        .isInstanceOf(ProviderCodecException.class)
        .hasCauseInstanceOf(JsonProcessingException.class);
  }

  @Test
  void isRejectedInput_withJacksonCause_shouldReturnTrue() {
    ProviderCodecException e =
        new ProviderCodecException("invalid", new JsonMappingException(null, "bad value"));

    assertThat(RegisterRequestErrors.isRejectedInput(e)).isTrue();
  }

  @Test
  void isRejectedInput_withCodecFailure_shouldReturnFalse() {
    // A codec bug surfaces as a ProviderCodecException wrapping something other than a Jackson
    // binding failure, and must be reported as an internal error rather than INVALID_ARGUMENT.
    assertThat(
            RegisterRequestErrors.isRejectedInput(
                new ProviderCodecException("failed", new NullPointerException())))
        .isFalse();
    assertThat(RegisterRequestErrors.isRejectedInput(new ProviderCodecException("failed")))
        .isFalse();
  }

  @Test
  void decode_withProviderCodecFailure_shouldRethrowAsIs() {
    // Stands in for a provider whose JSON decodes, but whose codec fails while doing so.
    ProviderCodecException failure =
        new ProviderCodecException("failed", new NullPointerException());
    RegisterRequest request = request().build();

    Throwable thrown =
        catchThrowable(
            () ->
                RegisterRequestDecoder.decode(
                    request,
                    RegisterRequestErrors.Context.of(request),
                    json -> {
                      throw failure;
                    }));

    assertThat(thrown).isSameAs(failure);
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

  @Test
  void toDomainRegisterDataSourceRequests_shouldDecodeEveryEntryInOrderIntoTheOuterCatalog() {
    InitializeCatalogRequest request =
        InitializeCatalogRequest.newBuilder()
            .setCatalogName("new-cat")
            .addDataSources(request().clearCatalogName().setDataSourceName("first"))
            .addDataSources(request().setCatalogName("other-cat").setDataSourceName("second"))
            .build();

    assertThat(mapper.toDomainRegisterDataSourceRequests(request))
        .extracting(RegisterDataSourceRequest::getCatalogName, RegisterDataSourceRequest::getName)
        .containsExactly(tuple("new-cat", "first"), tuple("new-cat", "second"));
  }

  @Test
  void toDomainRegisterDataSourceRequests_withInvalidEntry_shouldNameOuterCatalogAndEntry() {
    InitializeCatalogRequest request =
        InitializeCatalogRequest.newBuilder()
            .setCatalogName("new-cat")
            .addDataSources(request())
            .addDataSources(
                request()
                    .clearCatalogName()
                    .setDataSourceName("broken")
                    .setProviderPayloadJson("{\"type\":\"dynamodb\"}"))
            .build();

    AnalyticsException e =
        catchThrowableOfType(
            AnalyticsException.class, () -> mapper.toDomainRegisterDataSourceRequests(request));

    assertThat(e).isNotNull();
    assertThat(e.getErrorCode()).isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT);
    assertThat(e.getMetadata())
        .containsOnly(
            entry("catalog_name", "new-cat"),
            entry("data_source_name", "broken"),
            entry("data_source_index", "1"),
            entry("field", "provider_payload_json"),
            entry("reason", "invalid"));
  }

  @Test
  void toDomainRegisterDataSourceRequests_withMalformedSchemaInFirstEntry_shouldReportIndexZero() {
    InitializeCatalogRequest request =
        InitializeCatalogRequest.newBuilder()
            .setCatalogName("cat")
            .addDataSources(request().setSchemaJson("{not json"))
            .build();

    AnalyticsException e =
        catchThrowableOfType(
            AnalyticsException.class, () -> mapper.toDomainRegisterDataSourceRequests(request));

    assertThat(e).isNotNull();
    assertThat(e.getMetadata())
        .contains(entry("data_source_index", "0"), entry("field", "schema_json"));
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
