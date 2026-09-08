/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import com.scalar.db.analytics.grpc.mapper.datatype.DataTypeJsonMapper;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * Mapper for converting between domain ColumnSchema model and protobuf messages.
 *
 * <p>ColumnSchema represents the schema for a column with type information.
 */
@Mapper(
    config = MapStructConfig.class,
    uses = {DataTypeJsonMapper.class})
public interface ColumnSchemaMapper {

  ColumnSchemaMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(ColumnSchemaMapper.class);

  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "type", source = "type", qualifiedByName = "dataTypeToJson")
  @Mapping(target = "nameBytes", ignore = true)
  @Mapping(target = "typeBytes", ignore = true)
  com.scalar.db.analytics.grpc.generated.datasource.v1.ColumnSchema toProto(
      ColumnSchema columnSchema);

  @Mapping(target = "type", source = "type", qualifiedByName = "jsonToDataType")
  ColumnSchema toDomain(
      com.scalar.db.analytics.grpc.generated.datasource.v1.ColumnSchema columnSchema);
}
