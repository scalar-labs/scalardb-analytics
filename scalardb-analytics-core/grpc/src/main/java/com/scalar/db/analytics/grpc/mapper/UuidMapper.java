/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.mapstruct.Mapper;
import org.mapstruct.Named;

/** Mapper for UUID conversions between String and UUID. */
@Mapper
public interface UuidMapper {

  UuidMapper INSTANCE = org.mapstruct.factory.Mappers.getMapper(UuidMapper.class);

  @Named("uuidToString")
  default @Nullable String toString(@Nullable UUID uuid) {
    return uuid != null ? uuid.toString() : null;
  }

  @Named("stringToUuid")
  default @Nullable UUID toUuid(@Nullable String string) {
    return string != null ? UUID.fromString(string) : null;
  }
}
