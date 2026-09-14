/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.api.request.schema.TableSchema;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for converting between domain TableSchema model and protobuf messages.
 *
 * <p>TableSchema represents the schema for a table with its columns.
 */
@Mapper(
    config = MapStructConfig.class,
    uses = {ColumnSchemaMapper.class})
public interface TableSchemaMapper {

  TableSchemaMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(TableSchemaMapper.class);

  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "nameBytes", ignore = true)
  @Mapping(target = "removeColumns", ignore = true)
  @Mapping(target = "columnsOrBuilderList", ignore = true)
  @Mapping(target = "columnsBuilderList", ignore = true)
  @Mapping(target = "columnsList", ignore = true)
  com.scalar.db.analytics.grpc.generated.datasource.v1.TableSchema toProto(TableSchema tableSchema);

  @AfterMapping
  default void setColumns(
      @MappingTarget
          com.scalar.db.analytics.grpc.generated.datasource.v1.TableSchema.Builder builder,
      TableSchema source) {
    if (source.getColumns() != null) {
      // Add null check for each column element
      for (ColumnSchema column : source.getColumns()) {
        if (column != null) {
          builder.addColumns(ColumnSchemaMapper.INSTANCE.toProto(column));
        }
      }
    }
  }

  @Mapping(target = "columns", source = "columnsList")
  TableSchema toDomain(
      com.scalar.db.analytics.grpc.generated.datasource.v1.TableSchema tableSchema);
}
