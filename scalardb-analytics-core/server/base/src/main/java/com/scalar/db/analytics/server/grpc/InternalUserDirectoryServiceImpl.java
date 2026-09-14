/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.grpc.generated.auth.v1.CreateInternalBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateInternalBackendUserResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteInternalBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteInternalBackendUserResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.InternalUserDirectoryServiceGrpc.InternalUserDirectoryServiceImplBase;
import com.scalar.db.analytics.usecase.auth.internal.InternalUserDirectoryUseCase;
import io.grpc.stub.StreamObserver;
import java.util.UUID;

public class InternalUserDirectoryServiceImpl extends InternalUserDirectoryServiceImplBase {

  private final InternalUserDirectoryUseCase internalUserDirectoryUseCase;

  public InternalUserDirectoryServiceImpl(
      InternalUserDirectoryUseCase internalUserDirectoryUseCase) {
    this.internalUserDirectoryUseCase = internalUserDirectoryUseCase;
  }

  @Override
  public void createInternalBackendUser(
      CreateInternalBackendUserRequest request,
      StreamObserver<CreateInternalBackendUserResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    internalUserDirectoryUseCase.createInternalBackendUser(
        userId, request.getUsername(), request.getPassword());

    responseObserver.onNext(CreateInternalBackendUserResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void deleteInternalBackendUser(
      DeleteInternalBackendUserRequest request,
      StreamObserver<DeleteInternalBackendUserResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    boolean deleted =
        internalUserDirectoryUseCase.deleteInternalBackendUser(
            userId, request.getUsername(), request.getCascade());

    responseObserver.onNext(
        DeleteInternalBackendUserResponse.newBuilder().setDeleted(deleted).build());
    responseObserver.onCompleted();
  }
}
