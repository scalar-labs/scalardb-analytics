/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceMapper;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceProviderJsonMapper;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceSchemaJsonMapper;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;

/**
 * Mapper for converting between domain request models and protobuf request messages.
 *
 * <p>This mapper handles the conversion of various request objects used in the gRPC service layer.
 */
@Mapper(
    config = MapStructConfig.class,
    uses = {DataSourceMapper.class})
public interface RequestMapper {

  RequestMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(RequestMapper.class);

  /**
   * Decodes a data source registration request received by the server.
   *
   * <p>The provider and schema JSON come from the caller, so a payload that does not decode fails
   * with {@code INVALID_ARGUMENT}, carrying {@code field} ({@code provider_payload_json} or {@code
   * schema_json}), {@code reason} ({@code required} or {@code invalid}), {@code catalog_name}, and
   * {@code data_source_name} in its metadata. The codec failure is kept as the cause for server
   * logs but not sent to the caller, since its message can echo the payload, which may hold
   * credentials.
   *
   * @param request the protobuf request
   * @return the domain request
   * @throws AnalyticsException if the provider or schema JSON is missing or invalid
   */
  default @Nullable RegisterDataSourceRequest toDomainRegisterDataSourceRequest(
      com.scalar.db.analytics.grpc.generated.datasource.v1.@Nullable RegisterRequest request) {
    if (request == null) {
      return null;
    }

    DataSourceProvider provider;
    try {
      provider = DataSourceProviderJsonMapper.INSTANCE.fromJson(request.getProviderPayloadJson());
    } catch (IllegalArgumentException e) {
      throw RegisterRequestErrors.invalidField(
          request, RegisterRequestErrors.PROVIDER_PAYLOAD_JSON, RegisterRequestErrors.INVALID, e);
    }
    if (provider == null) {
      throw RegisterRequestErrors.invalidField(
          request,
          RegisterRequestErrors.PROVIDER_PAYLOAD_JSON,
          RegisterRequestErrors.REQUIRED,
          null);
    }

    DataSourceSchema schema;
    try {
      schema =
          DataSourceSchemaJsonMapper.INSTANCE.fromJson(
              request.hasSchemaJson() ? request.getSchemaJson() : null);
    } catch (IllegalArgumentException e) {
      throw RegisterRequestErrors.invalidField(
          request, RegisterRequestErrors.SCHEMA_JSON, RegisterRequestErrors.INVALID, e);
    }

    return new RegisterDataSourceRequest(
        request.getCatalogName(), request.getDataSourceName(), provider, schema);
  }

  default com.scalar.db.analytics.grpc.generated.datasource.v1.@Nullable RegisterRequest
      toProtoRegisterRequest(@Nullable RegisterDataSourceRequest request) {
    if (request == null) {
      return null;
    }

    com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterRequest.Builder builder =
        com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterRequest.newBuilder();

    builder.setCatalogName(request.getCatalogName());
    builder.setDataSourceName(request.getName());
    builder.setProviderPayloadJson(
        DataSourceProviderJsonMapper.INSTANCE.toJson(request.getProvider()));
    if (request.getSchema() != null) {
      builder.setSchemaJson(DataSourceSchemaJsonMapper.INSTANCE.toJson(request.getSchema()));
    }

    return builder.build();
  }
}
