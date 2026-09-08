/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.catalog;

import com.scalar.db.analytics.grpc.generated.catalog.v1.Catalog;
import com.scalar.db.analytics.grpc.mapper.UuidMapper;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** Mapper for converting between Catalog domain model and protobuf. */
@Mapper(
    config = MapStructConfig.class,
    uses = {UuidMapper.class})
public interface CatalogMapper {

  CatalogMapper INSTANCE = Mappers.getMapper(CatalogMapper.class);

  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "idBytes", ignore = true)
  @Mapping(target = "nameBytes", ignore = true)
  Catalog toProto(com.scalar.db.analytics.api.model.Catalog catalog);

  default com.scalar.db.analytics.api.model.@Nullable Catalog toDomain(@Nullable Catalog proto) {
    if (proto == null) {
      return null;
    }
    UUID id = UUID.fromString(proto.getId());
    return com.scalar.db.analytics.api.model.Catalog.of(id, proto.getName());
  }
}
