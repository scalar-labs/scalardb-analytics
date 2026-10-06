/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.datasource;

import com.scalar.db.analytics.api.model.SkippedColumn;
import com.scalar.db.analytics.grpc.generated.datasource.v1.SkippedColumnWarning;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/** Mapper for converting between domain skipped columns and protobuf warning messages. */
@Mapper(config = MapStructConfig.class)
public interface SkippedColumnMapper {

  SkippedColumnMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(SkippedColumnMapper.class);

  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "namespaceNamesList", ignore = true)
  @Mapping(target = "tableNameBytes", ignore = true)
  @Mapping(target = "columnNameBytes", ignore = true)
  @Mapping(target = "sourceTypeNameBytes", ignore = true)
  @Mapping(target = "reasonBytes", ignore = true)
  SkippedColumnWarning toProto(SkippedColumn skippedColumn);

  @AfterMapping
  default void setNamespaceNames(
      @MappingTarget SkippedColumnWarning.Builder builder, SkippedColumn source) {
    if (source.getNamespaceNames() != null) {
      for (String name : source.getNamespaceNames()) {
        if (name != null) {
          builder.addNamespaceNames(name);
        }
      }
    }
  }

  @Mapping(target = "namespaceNames", source = "namespaceNamesList")
  SkippedColumn toDomain(SkippedColumnWarning warning);
}
