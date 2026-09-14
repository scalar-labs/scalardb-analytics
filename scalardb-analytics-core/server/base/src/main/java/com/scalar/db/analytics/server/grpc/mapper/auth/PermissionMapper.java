/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc.mapper.auth;

import com.scalar.db.analytics.domain.authz.EffectivePermission;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.PermissionSource;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionType;
import com.scalar.db.analytics.grpc.mapper.UuidMapper;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.mapstruct.factory.Mappers;

/** Mapper for converting between permission-related domain models and protobuf. */
@Mapper(
    config = MapStructConfig.class,
    uses = {UuidMapper.class})
public interface PermissionMapper {

  PermissionMapper INSTANCE = Mappers.getMapper(PermissionMapper.class);

  // GranteeType: proto -> domain
  @ValueMapping(source = "GRANTEE_TYPE_USER", target = "USER")
  @ValueMapping(source = "GRANTEE_TYPE_ROLE", target = "ROLE")
  @ValueMapping(source = "GRANTEE_TYPE_UNSPECIFIED", target = MappingConstants.THROW_EXCEPTION)
  @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.THROW_EXCEPTION)
  GranteeType toDomainGranteeType(com.scalar.db.analytics.grpc.generated.auth.v1.GranteeType proto);

  // PermissionType: proto -> domain
  @ValueMapping(source = "PERMISSION_TYPE_CATALOG_READ", target = "CATALOG_READ")
  @ValueMapping(source = "PERMISSION_TYPE_CATALOG_WRITE", target = "CATALOG_WRITE")
  @ValueMapping(source = "PERMISSION_TYPE_CATALOG_ADMIN", target = "CATALOG_ADMIN")
  @ValueMapping(source = "PERMISSION_TYPE_DATA_SOURCE_READ", target = "DATA_SOURCE_READ")
  @ValueMapping(source = "PERMISSION_TYPE_DATA_SOURCE_ADMIN", target = "DATA_SOURCE_ADMIN")
  @ValueMapping(source = "PERMISSION_TYPE_NAMESPACE_READ", target = "NAMESPACE_READ")
  @ValueMapping(source = "PERMISSION_TYPE_TABLE_READ", target = "TABLE_READ")
  @ValueMapping(source = "PERMISSION_TYPE_UNSPECIFIED", target = MappingConstants.THROW_EXCEPTION)
  @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.THROW_EXCEPTION)
  Permission toDomainPermission(PermissionType proto);

  // PermissionType: domain -> proto
  @ValueMapping(source = "CATALOG_READ", target = "PERMISSION_TYPE_CATALOG_READ")
  @ValueMapping(source = "CATALOG_WRITE", target = "PERMISSION_TYPE_CATALOG_WRITE")
  @ValueMapping(source = "CATALOG_ADMIN", target = "PERMISSION_TYPE_CATALOG_ADMIN")
  @ValueMapping(source = "DATA_SOURCE_READ", target = "PERMISSION_TYPE_DATA_SOURCE_READ")
  @ValueMapping(source = "DATA_SOURCE_ADMIN", target = "PERMISSION_TYPE_DATA_SOURCE_ADMIN")
  @ValueMapping(source = "NAMESPACE_READ", target = "PERMISSION_TYPE_NAMESPACE_READ")
  @ValueMapping(source = "TABLE_READ", target = "PERMISSION_TYPE_TABLE_READ")
  PermissionType toProtoPermissionType(Permission domain);

  // PermissionSource: domain -> proto
  @ValueMapping(source = "DIRECT", target = "PERMISSION_SOURCE_DIRECT")
  @ValueMapping(source = "VIA_ROLE", target = "PERMISSION_SOURCE_VIA_ROLE")
  com.scalar.db.analytics.grpc.generated.auth.v1.PermissionSource toProtoPermissionSource(
      PermissionSource domain);

  // EffectivePermission: domain -> proto
  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "permission", source = "permission")
  @Mapping(target = "resourceId", source = "resourceId", qualifiedByName = "uuidToString")
  @Mapping(target = "resourceIdBytes", ignore = true)
  @Mapping(target = "source", source = "source")
  @Mapping(target = "viaRoleId", source = "viaRoleId", qualifiedByName = "uuidToString")
  @Mapping(target = "viaRoleIdBytes", ignore = true)
  @Mapping(target = "viaRoleNameBytes", ignore = true)
  @Mapping(target = "permissionValue", ignore = true)
  @Mapping(target = "sourceValue", ignore = true)
  // protobuf-java 4.x exposes clearExtension on the Builder; this module resolves protobuf-java 4.x
  // via proto-google-common-protos, while the grpc module stays on 3.x, so the ignore is declared
  // here instead of in the shared IgnoreProtobufBuilderDefaults.
  // TODO(#521): once the authz models are consolidated into the api module and this mapper moves to
  // the grpc module (protobuf-java 3.x), remove this clearExtension ignore.
  @Mapping(target = "clearExtension", ignore = true)
  com.scalar.db.analytics.grpc.generated.auth.v1.EffectivePermission toProto(
      EffectivePermission domain);
}
