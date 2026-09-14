/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.auth;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.grpc.generated.auth.v1.PasswordBackend;
import org.mapstruct.Mapper;
import org.mapstruct.MappingConstants;
import org.mapstruct.ValueMapping;
import org.mapstruct.factory.Mappers;

/**
 * Maps between the api-layer {@link PasswordBackendType} and the wire-format {@link
 * PasswordBackend} proto enum.
 *
 * <p>{@link PasswordBackend#PASSWORD_BACKEND_UNSPECIFIED} (the proto3 default-zero placeholder) and
 * {@code UNRECOGNIZED} (a value the client sent that this build does not know) both throw {@link
 * IllegalArgumentException} on {@link #toDomain}: callers must always specify a concrete backend.
 */
@Mapper
public interface PasswordBackendMapper {

  PasswordBackendMapper INSTANCE = Mappers.getMapper(PasswordBackendMapper.class);

  @ValueMapping(source = "PASSWORD_BACKEND_INTERNAL", target = "INTERNAL")
  @ValueMapping(source = "PASSWORD_BACKEND_SCALARDB_CLUSTER", target = "SCALARDB_CLUSTER")
  @ValueMapping(source = "PASSWORD_BACKEND_UNSPECIFIED", target = MappingConstants.THROW_EXCEPTION)
  @ValueMapping(source = "UNRECOGNIZED", target = MappingConstants.THROW_EXCEPTION)
  PasswordBackendType toDomain(PasswordBackend proto);

  @ValueMapping(source = "INTERNAL", target = "PASSWORD_BACKEND_INTERNAL")
  @ValueMapping(source = "SCALARDB_CLUSTER", target = "PASSWORD_BACKEND_SCALARDB_CLUSTER")
  PasswordBackend toProto(PasswordBackendType domain);
}
