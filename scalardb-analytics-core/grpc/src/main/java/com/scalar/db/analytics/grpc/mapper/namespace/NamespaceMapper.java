/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.namespace;

import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.grpc.mapper.UuidMapper;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import com.scalar.db.analytics.grpc.mapper.datasource.DataSourceMapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper for converting between domain namespace models and protobuf messages.
 *
 * <p>This mapper handles the conversion of:
 *
 * <ul>
 *   <li>Namespace - A namespace within a data source
 *   <li>DataSourceNamespace - A namespace with its associated data source
 * </ul>
 */
@Mapper(
    config = MapStructConfig.class,
    uses = {UuidMapper.class, DataSourceMapper.class})
public interface NamespaceMapper {

  NamespaceMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(NamespaceMapper.class);

  // Namespace mappings
  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "dataSourceId", source = "dataSourceId", qualifiedByName = "uuidToString")
  @Mapping(target = "idBytes", ignore = true)
  @Mapping(target = "dataSourceIdBytes", ignore = true)
  @Mapping(target = "namesList", ignore = true)
  com.scalar.db.analytics.grpc.generated.namespace.v1.Namespace toProto(Namespace namespace);

  @AfterMapping
  default void setNames(
      @MappingTarget com.scalar.db.analytics.grpc.generated.namespace.v1.Namespace.Builder builder,
      Namespace source) {
    if (source.getNames() != null) {
      // Add null check for each name element
      for (String name : source.getNames()) {
        if (name != null) {
          builder.addNames(name);
        }
      }
    }
  }

  @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
  @Mapping(target = "dataSourceId", source = "dataSourceId", qualifiedByName = "stringToUuid")
  @Mapping(target = "names", source = "namesList")
  Namespace toDomain(com.scalar.db.analytics.grpc.generated.namespace.v1.Namespace namespace);

  // DataSourceNamespace mappings
  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "mergeDataSource", ignore = true)
  @Mapping(target = "mergeNamespace", ignore = true)
  com.scalar.db.analytics.grpc.generated.namespace.v1.DataSourceNamespace toProto(
      DataSourceNamespace dataSourceNamespace);

  DataSourceNamespace toDomain(
      com.scalar.db.analytics.grpc.generated.namespace.v1.DataSourceNamespace dataSourceNamespace);
}
