/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import com.scalar.db.analytics.api.request.schema.NamespaceSchema;
import com.scalar.db.analytics.api.request.schema.TableSchema;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for converting between domain NamespaceSchema model and protobuf messages.
 *
 * <p>NamespaceSchema represents the schema for a namespace containing its tables.
 */
@Mapper(
    config = MapStructConfig.class,
    uses = {TableSchemaMapper.class})
public interface NamespaceSchemaMapper {

  NamespaceSchemaMapper INSTANCE =
      org.mapstruct.factory.Mappers.getMapper(NamespaceSchemaMapper.class);

  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "namesList", ignore = true)
  @Mapping(target = "tablesList", ignore = true)
  @Mapping(target = "removeTables", ignore = true)
  @Mapping(target = "tablesOrBuilderList", ignore = true)
  @Mapping(target = "tablesBuilderList", ignore = true)
  com.scalar.db.analytics.grpc.generated.datasource.v1.NamespaceSchema toProto(
      NamespaceSchema namespaceSchema);

  @AfterMapping
  default void setNamesAndTables(
      @MappingTarget
          com.scalar.db.analytics.grpc.generated.datasource.v1.NamespaceSchema.Builder builder,
      NamespaceSchema source) {
    if (source.getNames() != null) {
      // Add null check for each name element
      for (String name : source.getNames()) {
        if (name != null) {
          builder.addNames(name);
        }
      }
    }
    if (source.getTables() != null) {
      // Add null check for each table element
      for (TableSchema table : source.getTables()) {
        if (table != null) {
          builder.addTables(TableSchemaMapper.INSTANCE.toProto(table));
        }
      }
    }
  }

  @Mapping(target = "names", source = "namesList")
  @Mapping(target = "tables", source = "tablesList")
  NamespaceSchema toDomain(
      com.scalar.db.analytics.grpc.generated.datasource.v1.NamespaceSchema namespaceSchema);
}
