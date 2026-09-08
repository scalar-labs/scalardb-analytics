/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceMapper;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceProviderJsonMapper;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceSchemaJsonMapper;
import java.util.Objects;
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

  // RegisterDataSourceRequest mappings (manual to support JSON payload)
  default @Nullable RegisterDataSourceRequest toDomainRegisterDataSourceRequest(
      com.scalar.db.analytics.grpc.generated.datasource.v1.@Nullable RegisterRequest request) {
    if (request == null) {
      return null;
    }

    DataSourceProvider provider =
        Objects.requireNonNull(
            DataSourceProviderJsonMapper.INSTANCE.fromJson(request.getProviderPayloadJson()),
            "provider payload must not be empty");
    String schemaJson = request.hasSchemaJson() ? request.getSchemaJson() : null;

    return new RegisterDataSourceRequest(
        request.getCatalogName(),
        request.getDataSourceName(),
        provider,
        DataSourceSchemaJsonMapper.INSTANCE.fromJson(schemaJson));
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
