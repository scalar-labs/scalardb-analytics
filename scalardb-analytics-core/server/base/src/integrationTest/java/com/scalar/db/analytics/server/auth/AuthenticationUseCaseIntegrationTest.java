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

class AuthenticationUseCaseIntegrationTest extends UseCaseIntegrationTestBase {

  @Autowired private TokenValidationUseCase tokenValidationUseCase;

  @Test
  void authenticateWithValidCredentials_shouldReturnToken() {
    AccessToken token =
        authenticationUseCase.authenticateWithPassword(
            new PasswordCredential(ADMIN_USERNAME, ADMIN_PASSWORD));

    assertThat(token.getToken()).isNotEmpty();
    assertThat(token.getUserId()).isNotNull();
    assertThat(token.getExpiresAt()).isNotNull();
  }

  @Test
  void authenticateWithWrongPassword_shouldThrow() {
    assertThatThrownBy(
            () ->
                authenticationUseCase.authenticateWithPassword(
                    new PasswordCredential(ADMIN_USERNAME, "wrong")))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.AUTHENTICATION_FAILED));
  }

  @Test
  void authenticateWithNonExistentUser_shouldThrow() {
    assertThatThrownBy(
            () ->
                authenticationUseCase.authenticateWithPassword(
                    new PasswordCredential("nonexistent", "password")))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.AUTHENTICATION_FAILED));
  }

  @Test
  void reAuthentication_shouldInvalidateOldToken() {
    // 1:1 token policy - re-auth replaces old token
    AccessToken firstToken =
        authenticationUseCase.authenticateWithPassword(
            new PasswordCredential(ADMIN_USERNAME, ADMIN_PASSWORD));

    AccessToken secondToken =
        authenticationUseCase.authenticateWithPassword(
            new PasswordCredential(ADMIN_USERNAME, ADMIN_PASSWORD));

    // New token should be different
    assertThat(secondToken.getToken()).isNotEqualTo(firstToken.getToken());

    // Old token should be invalid
    assertThatThrownBy(
            () ->
                tokenValidationUseCase.validateToken(
                    UUID.fromString(firstToken.getUserId()), firstToken.getToken()))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.TOKEN_INVALID));

    // New token should be valid
    var validatedUserId =
        tokenValidationUseCase.validateToken(
            UUID.fromString(secondToken.getUserId()), secondToken.getToken());
    assertThat(validatedUserId).isEqualTo(UUID.fromString(secondToken.getUserId()));
  }
}
