/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.server.support.UseCaseIntegrationTestBase;
import com.scalar.db.analytics.usecase.auth.TokenValidationUseCase;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TokenValidationUseCaseIntegrationTest extends UseCaseIntegrationTestBase {

  @Autowired private TokenValidationUseCase tokenValidationUseCase;

  @Test
  void validateValidToken_shouldReturnUserId() {
    AccessToken token =
        authenticationUseCase.authenticateWithPassword(
            new PasswordCredential(ADMIN_USERNAME, ADMIN_PASSWORD));

    UUID validatedUserId =
        tokenValidationUseCase.validateToken(UUID.fromString(token.getUserId()), token.getToken());

    assertThat(validatedUserId).isEqualTo(UUID.fromString(token.getUserId()));
  }

  @Test
  void validateNonExistentToken_shouldThrow() {
    AccessToken token =
        authenticationUseCase.authenticateWithPassword(
            new PasswordCredential(ADMIN_USERNAME, ADMIN_PASSWORD));

    assertThatThrownBy(
            () ->
                tokenValidationUseCase.validateToken(
                    UUID.fromString(token.getUserId()), "non-existent-token"))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.TOKEN_INVALID));
  }

  @Test
  void validateTokenWithWrongUserId_shouldThrow() {
    AccessToken token =
        authenticationUseCase.authenticateWithPassword(
            new PasswordCredential(ADMIN_USERNAME, ADMIN_PASSWORD));

    assertThatThrownBy(
            () -> tokenValidationUseCase.validateToken(UUID.randomUUID(), token.getToken()))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.TOKEN_INVALID));
  }
}
