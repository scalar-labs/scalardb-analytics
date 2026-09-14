/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordResponse;
import com.scalar.db.analytics.usecase.auth.AuthenticationUseCase;
import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AuthServiceImplTest {
  private AuthenticationUseCase useCase;
  private AuthServiceGrpc.AuthServiceBlockingStub stub;

  @BeforeEach
  void setUp() throws IOException {
    useCase = mock(AuthenticationUseCase.class);
    AuthServiceImpl service = new AuthServiceImpl(useCase);

    String serverName = InProcessServerBuilder.generateName();
    InProcessServerBuilder.forName(serverName)
        .addService(service)
        .intercept(new ExceptionHandlingInterceptor())
        .build()
        .start();

    Channel channel = InProcessChannelBuilder.forName(serverName).build();
    stub = AuthServiceGrpc.newBlockingStub(channel);
  }

  @Test
  void authenticateWithPassword_shouldReturnTokenOnSuccess() throws Exception {
    // Arrange
    String username = "alice";
    String password = "secret123";
    String token = "generated_token_123";
    String userId = "550e8400-e29b-41d4-a716-446655440000";
    Instant expiresAt = Instant.now().plusSeconds(86400);

    AccessToken accessToken = new AccessToken(token, expiresAt, userId);

    when(useCase.authenticateWithPassword(any())).thenReturn(accessToken);

    AuthenticateWithPasswordRequest request =
        AuthenticateWithPasswordRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build();

    // Act
    AuthenticateWithPasswordResponse response = stub.authenticateWithPassword(request);

    // Assert
    assertThat(response.getToken()).isEqualTo(token);
    assertThat(response.getExpiresAt()).isEqualTo(expiresAt.getEpochSecond());
    assertThat(response.getUserId()).isEqualTo(userId);
  }

  @Test
  void authenticateWithPassword_shouldReturnUnauthenticatedOnFailure() throws Exception {
    // Arrange
    String username = "alice";
    String password = "wrongpassword";

    when(useCase.authenticateWithPassword(any()))
        .thenThrow(new AnalyticsException(AnalyticsErrorCode.AUTHENTICATION_FAILED));

    AuthenticateWithPasswordRequest request =
        AuthenticateWithPasswordRequest.newBuilder()
            .setUsername(username)
            .setPassword(password)
            .build();

    // Act & Assert
    assertThatThrownBy(() -> stub.authenticateWithPassword(request))
        .isInstanceOf(StatusRuntimeException.class)
        .satisfies(
            e -> {
              StatusRuntimeException se = (StatusRuntimeException) e;
              assertThat(se.getStatus().getCode()).isEqualTo(Status.Code.UNAUTHENTICATED);
            });
  }
}
