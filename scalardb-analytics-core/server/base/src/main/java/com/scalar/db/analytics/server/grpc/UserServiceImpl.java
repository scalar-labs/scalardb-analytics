/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.api.auth.AuthUser;
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
import com.scalar.db.analytics.grpc.generated.auth.v1.LinkBackendUserResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListUsersRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListUsersResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.PasswordBackend;
import com.scalar.db.analytics.grpc.generated.auth.v1.UnlinkBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.UnlinkBackendUserResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.UserServiceGrpc.UserServiceImplBase;
import com.scalar.db.analytics.grpc.mapper.auth.PasswordBackendMapper;
import com.scalar.db.analytics.usecase.auth.UserUseCase;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class UserServiceImpl extends UserServiceImplBase {

  private final UserUseCase userUseCase;

  public UserServiceImpl(UserUseCase userUseCase) {
    this.userUseCase = userUseCase;
  }

  // ---------------- Read ----------------

  @Override
  public void listUsers(
      ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    List<UserInfo> users = userUseCase.listUsers(userId);

    ListUsersResponse.Builder builder = ListUsersResponse.newBuilder();
    builder.addAllUsers(
        users.stream().map(UserServiceImpl::toProtoUserInfo).collect(Collectors.toList()));

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void describeUserById(
      DescribeUserByIdRequest request, StreamObserver<DescribeUserByIdResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    UUID targetUserId = UUID.fromString(request.getUserId());
    Optional<UserDetail> detail = userUseCase.describeUserById(userId, targetUserId);

    DescribeUserByIdResponse.Builder responseBuilder = DescribeUserByIdResponse.newBuilder();
    detail.ifPresent(d -> responseBuilder.setUser(toProtoUserDetail(d)));

    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void describeUserByName(
      DescribeUserByNameRequest request,
      StreamObserver<DescribeUserByNameResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    Optional<UserDetail> detail = userUseCase.describeUserByName(userId, request.getUsername());

    DescribeUserByNameResponse.Builder responseBuilder = DescribeUserByNameResponse.newBuilder();
    detail.ifPresent(d -> responseBuilder.setUser(toProtoUserDetail(d)));

    responseObserver.onNext(responseBuilder.build());
    responseObserver.onCompleted();
  }

  // ---------------- Write: principal ----------------

  @Override
  public void createUser(
      CreateUserRequest request, StreamObserver<CreateUserResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    AuthUser user = userUseCase.createUser(userId, request.getUsername());
    responseObserver.onNext(
        CreateUserResponse.newBuilder().setUserId(user.getUserId().toString()).build());
    responseObserver.onCompleted();
  }

  @Override
  public void deleteUser(
      DeleteUserRequest request, StreamObserver<DeleteUserResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    boolean deleted = userUseCase.deleteUser(userId, request.getUsername(), request.getCascade());
    responseObserver.onNext(DeleteUserResponse.newBuilder().setDeleted(deleted).build());
    responseObserver.onCompleted();
  }

  @Override
  public void deleteUserById(
      DeleteUserByIdRequest request, StreamObserver<DeleteUserByIdResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    UUID targetUserId = UUID.fromString(request.getUserId());
    boolean deleted = userUseCase.deleteUserById(userId, targetUserId, request.getCascade());
    responseObserver.onNext(DeleteUserByIdResponse.newBuilder().setDeleted(deleted).build());
    responseObserver.onCompleted();
  }

  @Override
  public void createUserWithBackendUser(
      CreateUserWithBackendUserRequest request,
      StreamObserver<CreateUserWithBackendUserResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    AuthUser user =
        userUseCase.createUserWithBackendUser(
            userId, request.getUsername(), request.getBackendUsername(), request.getPassword());
    responseObserver.onNext(
        CreateUserWithBackendUserResponse.newBuilder()
            .setUserId(user.getUserId().toString())
            .build());
    responseObserver.onCompleted();
  }

  // ---------------- Write: link ----------------

  @Override
  public void linkBackendUser(
      LinkBackendUserRequest request, StreamObserver<LinkBackendUserResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    PasswordBackendType backend = PasswordBackendMapper.INSTANCE.toDomain(request.getBackend());
    userUseCase.linkBackendUser(
        userId, request.getUsername(), request.getBackendUsername(), backend);
    responseObserver.onNext(LinkBackendUserResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  @Override
  public void unlinkBackendUser(
      UnlinkBackendUserRequest request,
      StreamObserver<UnlinkBackendUserResponse> responseObserver) {
    UUID userId = AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
    PasswordBackendType backend = PasswordBackendMapper.INSTANCE.toDomain(request.getBackend());
    userUseCase.unlinkBackendUser(
        userId, request.getUsername(), request.getBackendUsername(), backend);
    responseObserver.onNext(UnlinkBackendUserResponse.newBuilder().build());
    responseObserver.onCompleted();
  }

  // ---------------- Helpers ----------------

  // A principal without any backend identity is represented at the api boundary by both
  // {@link UserDetail#getBackendUserId()} and {@link UserDetail#getBackend()} being null;
  // on the wire those map to the proto3 default-zero placeholders (empty string and
  // PASSWORD_BACKEND_UNSPECIFIED) so the client can recover the same absence semantics.
  private static com.scalar.db.analytics.grpc.generated.auth.v1.UserInfo toProtoUserInfo(
      UserInfo u) {
    PasswordBackend protoBackend =
        u.getBackend() == null
            ? PasswordBackend.PASSWORD_BACKEND_UNSPECIFIED
            : PasswordBackendMapper.INSTANCE.toProto(u.getBackend());
    String backendUserId = u.getBackendUserId() == null ? "" : u.getBackendUserId();
    return com.scalar.db.analytics.grpc.generated.auth.v1.UserInfo.newBuilder()
        .setUserId(u.getUserId())
        .setBackendUserId(backendUserId)
        .setBackend(protoBackend)
        .setUsername(u.getUsername())
        .build();
  }

  private static com.scalar.db.analytics.grpc.generated.auth.v1.UserDetail toProtoUserDetail(
      UserDetail d) {
    PasswordBackend protoBackend =
        d.getBackend() == null
            ? PasswordBackend.PASSWORD_BACKEND_UNSPECIFIED
            : PasswordBackendMapper.INSTANCE.toProto(d.getBackend());
    String backendUserId = d.getBackendUserId() == null ? "" : d.getBackendUserId();
    return com.scalar.db.analytics.grpc.generated.auth.v1.UserDetail.newBuilder()
        .setUserId(d.getUserId())
        .setBackendUserId(backendUserId)
        .setBackend(protoBackend)
        .addAllRoles(d.getRoles())
        .setUsername(d.getUsername())
        .build();
  }
}
