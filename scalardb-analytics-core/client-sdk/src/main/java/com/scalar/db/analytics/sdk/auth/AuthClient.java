/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordResponse;
import com.scalar.db.analytics.sdk.exception.GrpcExceptionMapper;
import io.grpc.Channel;
import java.time.Instant;

/**
 * Client for the AuthService gRPC service.
 *
 * <p>Handles password-based authentication and converts the gRPC response into a domain {@link
 * AccessToken}.
 */
public class AuthClient {
  private final AuthServiceGrpc.AuthServiceBlockingStub stub;

  public AuthClient(Channel channel) {
    this.stub = AuthServiceGrpc.newBlockingStub(channel);
  }

  @VisibleForTesting
  AuthClient(AuthServiceGrpc.AuthServiceBlockingStub stub) {
    this.stub = stub;
  }

  /**
   * Authenticates with the given password credential and returns an access token.
   *
   * @param credential the username and password to authenticate with
   * @return the access token containing the token string, expiration time, and user ID
   * @throws AnalyticsException if authentication fails
   */
  AccessToken authenticate(PasswordCredential credential) {
    try {
      AuthenticateWithPasswordRequest request =
          AuthenticateWithPasswordRequest.newBuilder()
              .setUsername(credential.getUsername())
              .setPassword(credential.getPassword())
              .build();
      AuthenticateWithPasswordResponse response = stub.authenticateWithPassword(request);
      return new AccessToken(
          response.getToken(),
          Instant.ofEpochSecond(response.getExpiresAt()),
          response.getUserId());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(e, "authenticate");
    }
  }
}
