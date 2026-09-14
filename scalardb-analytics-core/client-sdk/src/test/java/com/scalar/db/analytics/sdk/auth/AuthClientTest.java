/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordResponse;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthClientTest {

  @Mock private AuthServiceGrpc.AuthServiceBlockingStub stub;
  private AuthClient authClient;

  @BeforeEach
  void setUp() {
    authClient = new AuthClient(stub);
  }

  @Test
  void authenticate_ShouldReturnAccessToken_WhenSuccessful() {
    // Arrange
    long expiresAt = Instant.now().plusSeconds(3600).getEpochSecond();
    AuthenticateWithPasswordResponse response =
        AuthenticateWithPasswordResponse.newBuilder()
            .setToken("test-token")
            .setExpiresAt(expiresAt)
            .setUserId("user-123")
            .build();
    when(stub.authenticateWithPassword(any())).thenReturn(response);

    PasswordCredential credential = new PasswordCredential("user", "pass");

    // Act
    AccessToken token = authClient.authenticate(credential);

    // Assert
    assertThat(token.getToken()).isEqualTo("test-token");
    assertThat(token.getExpiresAt()).isEqualTo(Instant.ofEpochSecond(expiresAt));
    assertThat(token.getUserId()).isEqualTo("user-123");
  }

  @Test
  void authenticate_ShouldThrowAnalyticsException_WhenTransportUnavailable() {
    // Arrange: a detail-less UNAVAILABLE is mapped to the client transport code.
    when(stub.authenticateWithPassword(any()))
        .thenThrow(new StatusRuntimeException(io.grpc.Status.UNAVAILABLE));

    PasswordCredential credential = new PasswordCredential("user", "pass");

    // Act & Assert
    assertThatThrownBy(() -> authClient.authenticate(credential))
        .isInstanceOf(AnalyticsException.class)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.SERVER_UNREACHABLE);
  }

  @Test
  void authenticate_ShouldThrowAnalyticsException_WhenUnexpectedErrorOccurs() {
    // Arrange
    when(stub.authenticateWithPassword(any())).thenThrow(new RuntimeException("unexpected"));

    PasswordCredential credential = new PasswordCredential("user", "pass");

    // Act & Assert
    assertThatThrownBy(() -> authClient.authenticate(credential))
        .isInstanceOf(AnalyticsException.class)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
  }
}
