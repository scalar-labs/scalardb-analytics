/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateInternalBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteInternalBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteInternalBackendUserResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.InternalUserDirectoryServiceGrpc;
import com.scalar.db.analytics.sdk.exception.GrpcExceptionMapper;
import io.grpc.Channel;

/**
 * Default implementation of {@link InternalUserDirectoryClient}.
 *
 * <p>This class is for internal SDK use only and should not be instantiated directly by SDK users.
 */
public class BlockingGrpcInternalUserDirectoryClient implements InternalUserDirectoryClient {
  private final InternalUserDirectoryServiceGrpc.InternalUserDirectoryServiceBlockingStub stub;

  public BlockingGrpcInternalUserDirectoryClient(Channel channel) {
    this.stub = InternalUserDirectoryServiceGrpc.newBlockingStub(channel);
  }

  @VisibleForTesting
  BlockingGrpcInternalUserDirectoryClient(
      InternalUserDirectoryServiceGrpc.InternalUserDirectoryServiceBlockingStub stub) {
    this.stub = stub;
  }

  @Override
  public void createInternalBackendUser(String username, String password) {
    try {
      CreateInternalBackendUserRequest request =
          CreateInternalBackendUserRequest.newBuilder()
              .setUsername(username)
              .setPassword(password)
              .build();
      stub.createInternalBackendUser(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "createInternalBackendUser", ImmutableMap.of("username", username));
    }
  }

  @Override
  public boolean deleteInternalBackendUser(String username, boolean cascade) {
    try {
      DeleteInternalBackendUserRequest request =
          DeleteInternalBackendUserRequest.newBuilder()
              .setUsername(username)
              .setCascade(cascade)
              .build();
      DeleteInternalBackendUserResponse response = stub.deleteInternalBackendUser(request);
      return response.getDeleted();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "deleteInternalBackendUser", ImmutableMap.of("username", username));
    }
  }
}
