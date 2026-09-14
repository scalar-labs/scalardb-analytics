/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListRolesRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListRolesResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RoleServiceGrpc;
import com.scalar.db.analytics.usecase.authz.RoleUseCase;
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
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RoleServiceImplTest {
  private static final UUID USER_ID = UUID.randomUUID();
  private RoleUseCase useCase;
  private RoleServiceGrpc.RoleServiceBlockingStub stub;

  @BeforeEach
  void setUp() throws IOException {
    useCase = mock(RoleUseCase.class);
    RoleServiceImpl service = new RoleServiceImpl(useCase);

    String serverName = InProcessServerBuilder.generateName();
    InProcessServerBuilder.forName(serverName)
        .addService(service)
        .intercept(userIdInterceptor())
        .build()
        .start();

    Channel channel = InProcessChannelBuilder.forName(serverName).build();
    stub = RoleServiceGrpc.newBlockingStub(channel);
  }

  @Test
  void createRole_shouldReturnCreatedRole() {
    String roleName = "analyst";
    Role role = Role.create(roleName);
    when(useCase.createRole(USER_ID, roleName)).thenReturn(role);

    CreateRoleResponse response =
        stub.createRole(CreateRoleRequest.newBuilder().setRoleName(roleName).build());

    assertThat(response.getRole().getName()).isEqualTo(roleName);
    assertThat(response.getRole().getId()).isEqualTo(role.id().toString());
    assertThat(response.getRole().getBuiltIn()).isFalse();
  }

  @Test
  void deleteRole_shouldReturnDeleted() {
    String roleName = "analyst";
    when(useCase.deleteRole(USER_ID, roleName)).thenReturn(true);

    DeleteRoleResponse response =
        stub.deleteRole(DeleteRoleRequest.newBuilder().setRoleName(roleName).build());

    assertThat(response.getDeleted()).isTrue();
  }

  @Test
  void listRoles_shouldReturnAllRoles() {
    Role role1 = Role.create("analyst");
    Role role2 = new Role(UUID.randomUUID(), "SUPERADMIN", true);
    when(useCase.listRoles(USER_ID)).thenReturn(List.of(role1, role2));

    ListRolesResponse response = stub.listRoles(ListRolesRequest.getDefaultInstance());

    assertThat(response.getRolesList()).hasSize(2);
    assertThat(response.getRoles(0).getName()).isEqualTo("analyst");
    assertThat(response.getRoles(1).getBuiltIn()).isTrue();
  }

  @Test
  void grantRole_shouldDelegateToUseCase() {
    String roleName = "editor";
    String username = "alice";

    stub.grantRole(
        GrantRoleRequest.newBuilder().setRoleName(roleName).setUsername(username).build());

    verify(useCase).grantRole(USER_ID, roleName, username);
  }

  @Test
  void revokeRole_shouldDelegateToUseCase() {
    String roleName = "editor";
    String username = "alice";

    stub.revokeRole(
        RevokeRoleRequest.newBuilder().setRoleName(roleName).setUsername(username).build());

    verify(useCase).revokeRole(USER_ID, roleName, username);
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
