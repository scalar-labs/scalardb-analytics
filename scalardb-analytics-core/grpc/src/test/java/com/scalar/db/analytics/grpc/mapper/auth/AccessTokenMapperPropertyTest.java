/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordResponse;
import java.time.Instant;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Combinators;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class AccessTokenMapperPropertyTest {

  private final AccessTokenMapper mapper = AccessTokenMapper.INSTANCE;

  @Property
  void toResponse_shouldMapAllFieldsCorrectly(@ForAll("accessToken") AccessToken accessToken) {
    AuthenticateWithPasswordResponse response = mapper.toResponse(accessToken);

    assertThat(response.getToken()).isEqualTo(accessToken.getToken());
    assertThat(response.getExpiresAt()).isEqualTo(accessToken.getExpiresAt().getEpochSecond());
    assertThat(response.getUserId()).isEqualTo(accessToken.getUserId());
  }

  @Property
  void fromResponse_shouldMapAllFieldsCorrectly(
      @ForAll("authenticateWithPasswordResponse") AuthenticateWithPasswordResponse response) {
    AccessToken accessToken = mapper.fromResponse(response);

    assertThat(accessToken.getToken()).isEqualTo(response.getToken());
    assertThat(accessToken.getExpiresAt())
        .isEqualTo(Instant.ofEpochSecond(response.getExpiresAt()));
    assertThat(accessToken.getUserId()).isEqualTo(response.getUserId());
  }

  @Property
  void roundTrip_toResponseAndBack(@ForAll("accessToken") AccessToken accessToken) {
    AccessToken result = mapper.fromResponse(mapper.toResponse(accessToken));

    assertThat(result.getToken()).isEqualTo(accessToken.getToken());
    assertThat(result.getExpiresAt()).isEqualTo(accessToken.getExpiresAt());
    assertThat(result.getUserId()).isEqualTo(accessToken.getUserId());
  }

  @Provide
  Arbitrary<AccessToken> accessToken() {
    return Combinators.combine(tokenString(), expiresAt(), userIdString()).as(AccessToken::new);
  }

  @Provide
  Arbitrary<AuthenticateWithPasswordResponse> authenticateWithPasswordResponse() {
    return Combinators.combine(tokenString(), expiresAtEpochSeconds(), userIdString())
        .as(
            (token, expiresAt, userId) ->
                AuthenticateWithPasswordResponse.newBuilder()
                    .setToken(token)
                    .setExpiresAt(expiresAt)
                    .setUserId(userId)
                    .build());
  }

  private Arbitrary<String> tokenString() {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('-', '_')
        .ofMinLength(1)
        .ofMaxLength(256);
  }

  private Arbitrary<Instant> expiresAt() {
    return expiresAtEpochSeconds().map(Instant::ofEpochSecond);
  }

  private Arbitrary<Long> expiresAtEpochSeconds() {
    return Arbitraries.longs()
        .between(Instant.now().getEpochSecond(), Instant.now().getEpochSecond() + 86400);
  }

  private Arbitrary<String> userIdString() {
    return Arbitraries.create(() -> UUID.randomUUID().toString());
  }
}
