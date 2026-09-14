/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.grpc.mapper.UuidMapper;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/** Mapper for converting between domain data source models and protobuf messages. */
@Mapper(
    config = MapStructConfig.class,
    uses = {UuidMapper.class, DataSourceProviderJsonMapper.class})
public interface DataSourceMapper {

  DataSourceMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(DataSourceMapper.class);

  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "catalogId", source = "catalogId", qualifiedByName = "uuidToString")
  @Mapping(target = "name", source = "name")
  @Mapping(target = "idBytes", ignore = true)
  @Mapping(target = "catalogIdBytes", ignore = true)
  @Mapping(target = "nameBytes", ignore = true)
  @Mapping(target = "providerPayloadJsonBytes", ignore = true)
  @Mapping(target = "providerPayloadJson", source = "provider", qualifiedByName = "providerToJson")
  com.scalar.db.analytics.grpc.generated.datasource.v1.DataSource toProto(DataSource dataSource);

  @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
  @Mapping(target = "catalogId", source = "catalogId", qualifiedByName = "stringToUuid")
  @Mapping(target = "name", source = "name")
  @Mapping(
      target = "provider",
      source = "providerPayloadJson",
      qualifiedByName = "providerFromJson")
  DataSource toDomain(com.scalar.db.analytics.grpc.generated.datasource.v1.DataSource dataSource);
}
