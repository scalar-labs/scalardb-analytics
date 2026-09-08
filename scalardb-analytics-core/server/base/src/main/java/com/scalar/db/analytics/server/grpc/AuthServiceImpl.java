/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthServiceGrpc.AuthServiceImplBase;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.AuthenticateWithPasswordResponse;
import com.scalar.db.analytics.grpc.mapper.auth.AccessTokenMapper;
import com.scalar.db.analytics.usecase.auth.AuthenticationUseCase;
import io.grpc.stub.StreamObserver;

public class AuthServiceImpl extends AuthServiceImplBase {
  private final AuthenticationUseCase authenticationUseCase;

  public AuthServiceImpl(AuthenticationUseCase authenticationUseCase) {
    this.authenticationUseCase = authenticationUseCase;
  }

  @Override
  public void authenticateWithPassword(
      AuthenticateWithPasswordRequest request,
      StreamObserver<AuthenticateWithPasswordResponse> responseObserver) {
    PasswordCredential credential =
        new PasswordCredential(request.getUsername(), request.getPassword());

    AccessToken accessToken = authenticationUseCase.authenticateWithPassword(credential);

    AuthenticateWithPasswordResponse response = AccessTokenMapper.INSTANCE.toResponse(accessToken);

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }
}
