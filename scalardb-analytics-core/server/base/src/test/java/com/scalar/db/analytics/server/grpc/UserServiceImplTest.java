/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.scalar.db.analytics.grpc.generated.auth.v1.ListUsersRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListUsersResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.PasswordBackend;
import com.scalar.db.analytics.grpc.generated.auth.v1.UnlinkBackendUserRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.UserServiceGrpc;
import com.scalar.db.analytics.usecase.auth.UserUseCase;
import io.grpc.Channel;
import io.grpc.Context;
import io.grpc.Contexts;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserServiceImplTest {
  private static final UUID USER_ID = UUID.randomUUID();
  private UserUseCase useCase;
  private UserServiceGrpc.UserServiceBlockingStub stub;

  @BeforeEach
  void setUp() throws IOException {
    useCase = mock(UserUseCase.class);
    UserServiceImpl service = new UserServiceImpl(useCase);

    String serverName = InProcessServerBuilder.generateName();
    InProcessServerBuilder.forName(serverName)
        .addService(service)
        .intercept(userIdInterceptor())
        .intercept(new ExceptionHandlingInterceptor())
        .build()
        .start();

    Channel channel = InProcessChannelBuilder.forName(serverName).build();
    stub = UserServiceGrpc.newBlockingStub(channel);
  }

  @Test
  void listUsers_shouldReturnUsersOnSuccess() {
    UUID userId1 = UUID.randomUUID();
    UUID userId2 = UUID.randomUUID();
    List<UserInfo> users =
        List.of(
            new UserInfo(userId1.toString(), "alice", PasswordBackendType.INTERNAL, "alice"),
            new UserInfo(userId2.toString(), "bob", PasswordBackendType.SCALARDB_CLUSTER, "bob"));

    when(useCase.listUsers(USER_ID)).thenReturn(users);

    ListUsersRequest request = ListUsersRequest.newBuilder().build();
    ListUsersResponse response = stub.listUsers(request);

    assertThat(response.getUsersCount()).isEqualTo(2);
    assertThat(response.getUsers(0).getUserId()).isEqualTo(userId1.toString());
    assertThat(response.getUsers(0).getBackendUserId()).isEqualTo("alice");
    assertThat(response.getUsers(0).getBackend())
        .isEqualTo(PasswordBackend.PASSWORD_BACKEND_INTERNAL);
    assertThat(response.getUsers(0).getUsername()).isEqualTo("alice");
    assertThat(response.getUsers(1).getUserId()).isEqualTo(userId2.toString());
    assertThat(response.getUsers(1).getBackendUserId()).isEqualTo("bob");
    assertThat(response.getUsers(1).getBackend())
        .isEqualTo(PasswordBackend.PASSWORD_BACKEND_SCALARDB_CLUSTER);
    assertThat(response.getUsers(1).getUsername()).isEqualTo("bob");
  }

  @Test
  void listUsers_shouldMapBackendlessPrincipalToProtoPlaceholders() {
    UUID carolId = UUID.randomUUID();
    when(useCase.listUsers(USER_ID))
        .thenReturn(List.of(new UserInfo(carolId.toString(), null, null, "carol")));

    ListUsersResponse response = stub.listUsers(ListUsersRequest.newBuilder().build());

    assertThat(response.getUsersCount()).isEqualTo(1);
    assertThat(response.getUsers(0).getUserId()).isEqualTo(carolId.toString());
    assertThat(response.getUsers(0).getUsername()).isEqualTo("carol");
    // A principal without a backend identity is encoded with proto3 default-zero placeholders.
    assertThat(response.getUsers(0).getBackendUserId()).isEmpty();
    assertThat(response.getUsers(0).getBackend())
        .isEqualTo(PasswordBackend.PASSWORD_BACKEND_UNSPECIFIED);
  }

  @Test
  void listUsers_shouldReturnEmptyListWhenNoUsers() {
    when(useCase.listUsers(USER_ID)).thenReturn(Collections.emptyList());

    ListUsersRequest request = ListUsersRequest.newBuilder().build();
    ListUsersResponse response = stub.listUsers(request);

    assertThat(response.getUsersCount()).isZero();
  }

  @Test
  void createUser_shouldReturnNewUserId() {
    UUID newId = UUID.randomUUID();
    when(useCase.createUser(USER_ID, "alice")).thenReturn(new AuthUser(newId, "alice"));

    CreateUserResponse response =
        stub.createUser(CreateUserRequest.newBuilder().setUsername("alice").build());

    assertThat(response.getUserId()).isEqualTo(newId.toString());
    verify(useCase).createUser(USER_ID, "alice");
  }

  @Test
  void deleteUser_shouldReturnDeletedFlagAndForwardCascade() {
    when(useCase.deleteUser(USER_ID, "alice", true)).thenReturn(true);

    DeleteUserResponse response =
        stub.deleteUser(
            DeleteUserRequest.newBuilder().setUsername("alice").setCascade(true).build());

    assertThat(response.getDeleted()).isTrue();
    verify(useCase).deleteUser(USER_ID, "alice", true);
  }

  @Test
  void deleteUserById_shouldReturnDeletedFlagAndForwardCascade() {
    UUID targetId = UUID.randomUUID();
    when(useCase.deleteUserById(USER_ID, targetId, false)).thenReturn(true);

    DeleteUserByIdResponse response =
        stub.deleteUserById(
            DeleteUserByIdRequest.newBuilder()
                .setUserId(targetId.toString())
                .setCascade(false)
                .build());

    assertThat(response.getDeleted()).isTrue();
    verify(useCase).deleteUserById(USER_ID, targetId, false);
  }

  @Test
  void createUserWithBackendUser_shouldReturnNewUserId() {
    UUID newId = UUID.randomUUID();
    when(useCase.createUserWithBackendUser(USER_ID, "alice", "alice-internal", "pw"))
        .thenReturn(new AuthUser(newId, "alice"));

    CreateUserWithBackendUserResponse response =
        stub.createUserWithBackendUser(
            CreateUserWithBackendUserRequest.newBuilder()
                .setUsername("alice")
                .setBackendUsername("alice-internal")
                .setPassword("pw")
                .build());

    assertThat(response.getUserId()).isEqualTo(newId.toString());
    verify(useCase).createUserWithBackendUser(USER_ID, "alice", "alice-internal", "pw");
  }

  @Test
  void linkBackendUser_shouldInvokeUseCaseWithMappedBackend() {
    stub.linkBackendUser(
        LinkBackendUserRequest.newBuilder()
            .setUsername("alice")
            .setBackendUsername("alice-internal")
            .setBackend(PasswordBackend.PASSWORD_BACKEND_INTERNAL)
            .build());

    verify(useCase)
        .linkBackendUser(USER_ID, "alice", "alice-internal", PasswordBackendType.INTERNAL);
  }

  @Test
  void unlinkBackendUser_shouldInvokeUseCaseWithMappedBackend() {
    stub.unlinkBackendUser(
        UnlinkBackendUserRequest.newBuilder()
            .setUsername("alice")
            .setBackendUsername("alice-internal")
            .setBackend(PasswordBackend.PASSWORD_BACKEND_SCALARDB_CLUSTER)
            .build());

    verify(useCase)
        .unlinkBackendUser(
            USER_ID, "alice", "alice-internal", PasswordBackendType.SCALARDB_CLUSTER);
  }

  @Test
  void describeUserById_shouldReturnUserDetailOnSuccess() {
    UUID targetUserId = UUID.randomUUID();
    UserDetail detail =
        new UserDetail(
            targetUserId.toString(),
            "alice",
            PasswordBackendType.INTERNAL,
            Arrays.asList("admin"),
            "alice");
    when(useCase.describeUserById(USER_ID, targetUserId)).thenReturn(Optional.of(detail));

    DescribeUserByIdRequest request =
        DescribeUserByIdRequest.newBuilder().setUserId(targetUserId.toString()).build();
    DescribeUserByIdResponse response = stub.describeUserById(request);

    assertThat(response.hasUser()).isTrue();
    assertThat(response.getUser().getBackendUserId()).isEqualTo("alice");
    assertThat(response.getUser().getBackend())
        .isEqualTo(PasswordBackend.PASSWORD_BACKEND_INTERNAL);
    assertThat(response.getUser().getRolesList()).containsExactly("admin");
    assertThat(response.getUser().getUsername()).isEqualTo("alice");
  }

  @Test
  void describeUserById_shouldReturnEmptyWhenUserNotFound() {
    UUID targetUserId = UUID.randomUUID();
    when(useCase.describeUserById(USER_ID, targetUserId)).thenReturn(Optional.empty());

    DescribeUserByIdRequest request =
        DescribeUserByIdRequest.newBuilder().setUserId(targetUserId.toString()).build();
    DescribeUserByIdResponse response = stub.describeUserById(request);

    assertThat(response.hasUser()).isFalse();
  }

  @Test
  void describeUserByName_shouldReturnUserDetailOnSuccess() {
    UUID targetUserId = UUID.randomUUID();
    UserDetail detail =
        new UserDetail(
            targetUserId.toString(),
            "alice",
            PasswordBackendType.INTERNAL,
            Arrays.asList("admin"),
            "alice");
    when(useCase.describeUserByName(USER_ID, "alice")).thenReturn(Optional.of(detail));

    DescribeUserByNameRequest request =
        DescribeUserByNameRequest.newBuilder().setUsername("alice").build();
    DescribeUserByNameResponse response = stub.describeUserByName(request);

    assertThat(response.hasUser()).isTrue();
    assertThat(response.getUser().getUserId()).isEqualTo(targetUserId.toString());
    assertThat(response.getUser().getBackendUserId()).isEqualTo("alice");
    assertThat(response.getUser().getBackend())
        .isEqualTo(PasswordBackend.PASSWORD_BACKEND_INTERNAL);
    assertThat(response.getUser().getRolesList()).containsExactly("admin");
    assertThat(response.getUser().getUsername()).isEqualTo("alice");
  }

  @Test
  void describeUserByName_shouldReturnEmptyWhenUserNotFound() {
    when(useCase.describeUserByName(USER_ID, "nonexistent")).thenReturn(Optional.empty());

    DescribeUserByNameRequest request =
        DescribeUserByNameRequest.newBuilder().setUsername("nonexistent").build();
    DescribeUserByNameResponse response = stub.describeUserByName(request);

    assertThat(response.hasUser()).isFalse();
  }

  private static ServerInterceptor userIdInterceptor() {
    return new ServerInterceptor() {
      @Override
      public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
          ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        Context ctx =
            Context.current().withValue(AuthenticationInterceptor.AUTHENTICATED_USER_ID, USER_ID);
        return Contexts.interceptCall(ctx, call, headers, next);
      }
    };
  }
}
