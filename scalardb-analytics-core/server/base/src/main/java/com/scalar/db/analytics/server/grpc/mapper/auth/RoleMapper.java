/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc.mapper.auth;

import com.scalar.db.analytics.grpc.generated.auth.v1.Role;
import com.scalar.db.analytics.grpc.mapper.UuidMapper;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

/** Mapper for converting between Role domain model and protobuf. */
@Mapper(
    config = MapStructConfig.class,
    uses = {UuidMapper.class})
public interface RoleMapper {

  RoleMapper INSTANCE = Mappers.getMapper(RoleMapper.class);

  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
  @Mapping(target = "idBytes", ignore = true)
  @Mapping(target = "nameBytes", ignore = true)
  // protobuf-java 4.x exposes clearExtension on the Builder; this module resolves protobuf-java 4.x
  // via proto-google-common-protos, while the grpc module stays on 3.x, so the ignore is declared
  // here instead of in the shared IgnoreProtobufBuilderDefaults.
  // TODO(#521): once the authz models are consolidated into the api module and this mapper moves to
  // the grpc module (protobuf-java 3.x), remove this clearExtension ignore.
  @Mapping(target = "clearExtension", ignore = true)
  Role toProto(com.scalar.db.analytics.domain.authz.Role role);
}
