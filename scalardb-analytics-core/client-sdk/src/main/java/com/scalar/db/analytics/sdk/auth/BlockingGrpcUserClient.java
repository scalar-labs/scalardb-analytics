/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.auth.UserDetail;
import com.scalar.db.analytics.api.auth.UserInfo;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateUserResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateUserWithBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateUserWithBackendUserResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteUserByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteUserByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteUserResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DescribeUserByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DescribeUserByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DescribeUserByNameRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DescribeUserByNameResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.LinkBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListUsersRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListUsersResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.PasswordBackend;
import com.scalar.db.analytics.grpc.generated.auth.v1.UnlinkBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.UserServiceGrpc;
import com.scalar.db.analytics.grpc.mapper.auth.PasswordBackendMapper;
import com.scalar.db.analytics.sdk.exception.GrpcExceptionMapper;
import io.grpc.Channel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;

/**
 * Default implementation of {@link UserClient}.
 *
 * <p>This class is for internal SDK use only and should not be instantiated directly by SDK users.
 */
public class BlockingGrpcUserClient implements UserClient {
  private final UserServiceGrpc.UserServiceBlockingStub stub;

  public BlockingGrpcUserClient(Channel channel) {
    this.stub = UserServiceGrpc.newBlockingStub(channel);
  }

  @VisibleForTesting
  BlockingGrpcUserClient(UserServiceGrpc.UserServiceBlockingStub stub) {
    this.stub = stub;
  }

  // ---------------- Read ----------------

  @Override
  public List<UserInfo> listUsers() {
    try {
      ListUsersResponse response = stub.listUsers(ListUsersRequest.newBuilder().build());
      return response.getUsersList().stream()
          .map(BlockingGrpcUserClient::toUserInfo)
          .collect(Collectors.toList());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(e, "listUsers");
    }
  }

  @Override
  public Optional<UserDetail> describeUserById(UUID userId) {
    try {
      DescribeUserByIdResponse response =
          stub.describeUserById(
              DescribeUserByIdRequest.newBuilder().setUserId(userId.toString()).build());
      return toUserDetail(response.hasUser() ? response.getUser() : null);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "describeUserById", ImmutableMap.of("userId", userId.toString()));
    }
  }

  @Override
  public Optional<UserDetail> describeUserByName(String username) {
    try {
      DescribeUserByNameResponse response =
          stub.describeUserByName(
              DescribeUserByNameRequest.newBuilder().setUsername(username).build());
      return toUserDetail(response.hasUser() ? response.getUser() : null);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "describeUserByName", ImmutableMap.of("username", username));
    }
  }

  // ---------------- Write: principal ----------------

  @Override
  public String createUser(String username) {
    try {
      CreateUserResponse response =
          stub.createUser(CreateUserRequest.newBuilder().setUsername(username).build());
      return response.getUserId();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "createUser", ImmutableMap.of("username", username));
    }
  }

  @Override
  public boolean deleteUser(String username, boolean cascade) {
    try {
      DeleteUserResponse response =
          stub.deleteUser(
              DeleteUserRequest.newBuilder().setUsername(username).setCascade(cascade).build());
      return response.getDeleted();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "deleteUser", ImmutableMap.of("username", username));
    }
  }

  @Override
  public boolean deleteUserById(UUID userId, boolean cascade) {
    try {
      DeleteUserByIdResponse response =
          stub.deleteUserById(
              DeleteUserByIdRequest.newBuilder()
                  .setUserId(userId.toString())
                  .setCascade(cascade)
                  .build());
      return response.getDeleted();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "deleteUserById", ImmutableMap.of("userId", userId.toString()));
    }
  }

  @Override
  public String createUserWithBackendUser(
      String username, String backendUsername, String password) {
    try {
      CreateUserWithBackendUserResponse response =
          stub.createUserWithBackendUser(
              CreateUserWithBackendUserRequest.newBuilder()
                  .setUsername(username)
                  .setBackendUsername(backendUsername)
                  .setPassword(password)
                  .build());
      return response.getUserId();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "createUserWithBackendUser",
          ImmutableMap.of("username", username, "backendUsername", backendUsername));
    }
  }

  // ---------------- Write: link ----------------

  @Override
  public void linkBackendUser(
      String username, String backendUsername, PasswordBackendType backend) {
    try {
      stub.linkBackendUser(
          LinkBackendUserRequest.newBuilder()
              .setUsername(username)
              .setBackendUsername(backendUsername)
              .setBackend(PasswordBackendMapper.INSTANCE.toProto(backend))
              .build());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "linkBackendUser",
          ImmutableMap.of("username", username, "backendUsername", backendUsername));
    }
  }

  @Override
  public void unlinkBackendUser(
      String username, String backendUsername, PasswordBackendType backend) {
    try {
      stub.unlinkBackendUser(
          UnlinkBackendUserRequest.newBuilder()
              .setUsername(username)
              .setBackendUsername(backendUsername)
              .setBackend(PasswordBackendMapper.INSTANCE.toProto(backend))
              .build());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "unlinkBackendUser",
          ImmutableMap.of("username", username, "backendUsername", backendUsername));
    }
  }

  // ---------------- Helpers ----------------

  private static UserInfo toUserInfo(
      com.scalar.db.analytics.grpc.generated.auth.v1.UserInfo proto) {
    // proto3 default-zero placeholders (UNSPECIFIED for backend, "" for backend_user_id) represent
    // a principal without any backend identity; map them back to null on the api boundary.
    PasswordBackendType backend =
        proto.getBackend() == PasswordBackend.PASSWORD_BACKEND_UNSPECIFIED
            ? null
            : PasswordBackendMapper.INSTANCE.toDomain(proto.getBackend());
    String backendUserId = proto.getBackendUserId().isEmpty() ? null : proto.getBackendUserId();
    return new UserInfo(proto.getUserId(), backendUserId, backend, proto.getUsername());
  }

  private static Optional<UserDetail> toUserDetail(
      com.scalar.db.analytics.grpc.generated.auth.v1.@Nullable UserDetail proto) {
    if (proto == null) {
      return Optional.empty();
    }
    // proto3 default-zero placeholders (UNSPECIFIED for backend, "" for backend_user_id) represent
    // a principal without any backend identity; map them back to null on the api boundary.
    PasswordBackendType backend =
        proto.getBackend() == PasswordBackend.PASSWORD_BACKEND_UNSPECIFIED
            ? null
            : PasswordBackendMapper.INSTANCE.toDomain(proto.getBackend());
    String backendUserId = proto.getBackendUserId().isEmpty() ? null : proto.getBackendUserId();
    return Optional.of(
        new UserDetail(
            proto.getUserId(), backendUserId, backend, proto.getRolesList(), proto.getUsername()));
  }
}
