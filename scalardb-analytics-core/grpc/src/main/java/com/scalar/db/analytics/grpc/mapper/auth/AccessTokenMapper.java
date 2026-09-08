/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.auth;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordResponse;
import com.scalar.db.analytics.grpc.mapper.annotation.IgnoreProtobufBuilderDefaults;
import com.scalar.db.analytics.grpc.mapper.config.MapStructConfig;
import java.time.Instant;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.factory.Mappers;

/** Mapper for bidirectional conversion between AccessToken and AuthenticateWithPasswordResponse. */
@Mapper(config = MapStructConfig.class)
public interface AccessTokenMapper {

  AccessTokenMapper INSTANCE = Mappers.getMapper(AccessTokenMapper.class);

  @IgnoreProtobufBuilderDefaults
  @Mapping(target = "token", source = "token")
  @Mapping(target = "tokenBytes", ignore = true)
  @Mapping(target = "expiresAt", source = "expiresAt", qualifiedByName = "instantToEpochSeconds")
  @Mapping(target = "userId", source = "userId")
  @Mapping(target = "userIdBytes", ignore = true)
  AuthenticateWithPasswordResponse toResponse(AccessToken accessToken);

  @Mapping(target = "expiresAt", source = "expiresAt", qualifiedByName = "epochSecondsToInstant")
  @Mapping(target = "userId", source = "userId")
  AccessToken fromResponse(AuthenticateWithPasswordResponse response);

  @Named("instantToEpochSeconds")
  default long instantToEpochSeconds(Instant instant) {
    return instant.getEpochSecond();
  }

  @Named("epochSecondsToInstant")
  default Instant epochSecondsToInstant(long epochSeconds) {
    return Instant.ofEpochSecond(epochSeconds);
  }
}
