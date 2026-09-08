/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.api.request.schema.NamespaceSchema;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for converting between domain DataSourceSchema model and protobuf messages.
 *
 * <p>DataSourceSchema represents the complete schema for a data source, containing all namespaces
 * and their tables.
 */
@Mapper(
    config = MapStructConfig.class,
    uses = {NamespaceSchemaMapper.class, TableSchemaMapper.class, ColumnSchemaMapper.class})
public interface DataSourceSchemaMapper {

  DataSourceSchemaMapper INSTANCE =
      org.mapstruct.factory.Mappers.getMapper(DataSourceSchemaMapper.class);

  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "namespacesList", ignore = true)
  @Mapping(target = "removeNamespaces", ignore = true)
  @Mapping(target = "namespacesOrBuilderList", ignore = true)
  @Mapping(target = "namespacesBuilderList", ignore = true)
  com.scalar.db.analytics.grpc.generated.datasource.v1.DataSourceSchema toProto(
      DataSourceSchema dataSourceSchema);

  @AfterMapping
  default void setNamespaces(
      @MappingTarget
          com.scalar.db.analytics.grpc.generated.datasource.v1.DataSourceSchema.Builder builder,
      DataSourceSchema source) {
    if (source.getNamespaces() != null) {
      // Add null check for each namespace element
      for (NamespaceSchema namespace : source.getNamespaces()) {
        if (namespace != null) {
          builder.addNamespaces(NamespaceSchemaMapper.INSTANCE.toProto(namespace));
        }
      }
    }
  }

  @Mapping(target = "namespaces", source = "namespacesList")
  DataSourceSchema toDomain(
      com.scalar.db.analytics.grpc.generated.datasource.v1.DataSourceSchema dataSourceSchema);
}
