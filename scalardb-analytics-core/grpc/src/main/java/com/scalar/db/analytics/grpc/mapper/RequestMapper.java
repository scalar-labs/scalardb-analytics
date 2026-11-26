/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.grpc.generated.catalog.v1.InitializeCatalogRequest;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceMapper;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceProviderJsonMapper;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceSchemaJsonMapper;
import java.util.ArrayList;
import java.util.List;
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
   * credentials. A codec failure that does not come from the payload, such as a bug in a provider
   * codec, is rethrown as is and reported as an internal error.
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
    return RegisterRequestDecoder.decode(request, RegisterRequestErrors.Context.of(request));
  }

  /**
   * Decodes the data sources of a catalog initialization request received by the server.
   *
   * <p>Each entry is decoded as in {@link
   * #toDomainRegisterDataSourceRequest(com.scalar.db.analytics.grpc.generated.datasource.v1.RegisterRequest)},
   * except that each data source is registered in the catalog of the initialization request (the
   * {@code catalog_name} of each entry is ignored), and the metadata of a decoding error carries
   * that catalog name and {@code data_source_index}, the 0-based index of the failed entry in
   * {@code data_sources}.
   *
   * @param request the protobuf request
   * @return the domain requests, in the order of {@code data_sources}
   * @throws AnalyticsException if the provider or schema JSON of an entry is missing or invalid
   */
  default List<RegisterDataSourceRequest> toDomainRegisterDataSourceRequests(
      InitializeCatalogRequest request) {
    List<RegisterDataSourceRequest> result = new ArrayList<>(request.getDataSourcesCount());
    for (int i = 0; i < request.getDataSourcesCount(); i++) {
      result.add(
          RegisterRequestDecoder.decode(
              request.getDataSources(i), RegisterRequestErrors.Context.of(request, i)));
    }
    return result;
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
