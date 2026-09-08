/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.authz.Role;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListRolesRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListRolesResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.RoleServiceGrpc;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BlockingGrpcRoleClientTest {

  private static final String ROLE_ID = UUID.randomUUID().toString();
  private static final String ROLE_NAME = "editor";
  private static final String USERNAME = "alice";

  @Mock private RoleServiceGrpc.RoleServiceBlockingStub stub;

  private BlockingGrpcRoleClient client;

  @BeforeEach
  void setUp() {
    client = new BlockingGrpcRoleClient(stub);
  }

  @Nested
  class CreateRole {
    @Test
    void shouldReturnRole_WhenSuccessful() {
      CreateRoleRequest expectedRequest =
          CreateRoleRequest.newBuilder().setRoleName(ROLE_NAME).build();
      com.scalar.db.analytics.grpc.generated.auth.v1.Role protoRole =
          com.scalar.db.analytics.grpc.generated.auth.v1.Role.newBuilder()
              .setId(ROLE_ID)
              .setName(ROLE_NAME)
              .setBuiltIn(false)
              .build();
      CreateRoleResponse response = CreateRoleResponse.newBuilder().setRole(protoRole).build();
      when(stub.createRole(expectedRequest)).thenReturn(response);

      Role role = client.createRole(ROLE_NAME);

      assertThat(role.getId()).isEqualTo(ROLE_ID);
      assertThat(role.getName()).isEqualTo(ROLE_NAME);
      assertThat(role.isBuiltIn()).isFalse();
      verify(stub).createRole(expectedRequest);
    }

    @Test
    void shouldThrowAnalyticsException_WhenGrpcCallFails() {
      when(stub.createRole(any()))
          .thenThrow(new StatusRuntimeException(io.grpc.Status.ALREADY_EXISTS));

      assertThatThrownBy(() -> client.createRole(ROLE_NAME))
          .isInstanceOf(AnalyticsException.class)
          .extracting(e -> ((AnalyticsException) e).getErrorCode())
          .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
    }
  }

  @Nested
  class DeleteRole {
    @Test
    void shouldReturnTrue_WhenDeleted() {
      DeleteRoleRequest expectedRequest =
          DeleteRoleRequest.newBuilder().setRoleName(ROLE_NAME).build();
      DeleteRoleResponse response = DeleteRoleResponse.newBuilder().setDeleted(true).build();
      when(stub.deleteRole(expectedRequest)).thenReturn(response);

      boolean result = client.deleteRole(ROLE_NAME);

      assertThat(result).isTrue();
      ArgumentCaptor<DeleteRoleRequest> captor = ArgumentCaptor.forClass(DeleteRoleRequest.class);
      verify(stub).deleteRole(captor.capture());
      assertThat(captor.getValue().getRoleName()).isEqualTo(ROLE_NAME);
    }

    @Test
    void shouldReturnFalse_WhenNotFound() {
      DeleteRoleRequest expectedRequest =
          DeleteRoleRequest.newBuilder().setRoleName(ROLE_NAME).build();
      DeleteRoleResponse response = DeleteRoleResponse.newBuilder().setDeleted(false).build();
      when(stub.deleteRole(expectedRequest)).thenReturn(response);

      boolean result = client.deleteRole(ROLE_NAME);

      assertThat(result).isFalse();
    }

    @Test
    void shouldThrowAnalyticsException_WhenGrpcCallFails() {
      when(stub.deleteRole(any())).thenThrow(new StatusRuntimeException(io.grpc.Status.INTERNAL));

      assertThatThrownBy(() -> client.deleteRole(ROLE_NAME))
          .isInstanceOf(AnalyticsException.class)
          .extracting(e -> ((AnalyticsException) e).getErrorCode())
          .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
    }
  }

  @Nested
  class ListRoles {
    @Test
    void shouldReturnRoles_WhenSuccessful() {
      String roleId2 = UUID.randomUUID().toString();
      com.scalar.db.analytics.grpc.generated.auth.v1.Role protoRole1 =
          com.scalar.db.analytics.grpc.generated.auth.v1.Role.newBuilder()
              .setId(ROLE_ID)
              .setName(ROLE_NAME)
              .setBuiltIn(false)
              .build();
      com.scalar.db.analytics.grpc.generated.auth.v1.Role protoRole2 =
          com.scalar.db.analytics.grpc.generated.auth.v1.Role.newBuilder()
              .setId(roleId2)
              .setName("SUPERADMIN")
              .setBuiltIn(true)
              .build();
      ListRolesResponse response =
          ListRolesResponse.newBuilder().addRoles(protoRole1).addRoles(protoRole2).build();
      when(stub.listRoles(ListRolesRequest.newBuilder().build())).thenReturn(response);

      List<Role> roles = client.listRoles();

      assertThat(roles).hasSize(2);
      assertThat(roles.get(0).getName()).isEqualTo(ROLE_NAME);
      assertThat(roles.get(1).getName()).isEqualTo("SUPERADMIN");
      assertThat(roles.get(1).isBuiltIn()).isTrue();
    }

    @Test
    void shouldThrowAnalyticsException_WhenGrpcCallFails() {
      when(stub.listRoles(any())).thenThrow(new StatusRuntimeException(io.grpc.Status.INTERNAL));

      assertThatThrownBy(() -> client.listRoles())
          .isInstanceOf(AnalyticsException.class)
          .extracting(e -> ((AnalyticsException) e).getErrorCode())
          .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
    }
  }

  @Nested
  class GrantRole {
    @Test
    void shouldSucceed_WhenGranted() {
      GrantRoleRequest expectedRequest =
          GrantRoleRequest.newBuilder().setRoleName(ROLE_NAME).setUsername(USERNAME).build();
      when(stub.grantRole(expectedRequest)).thenReturn(GrantRoleResponse.newBuilder().build());

      client.grantRole(ROLE_NAME, USERNAME);

      ArgumentCaptor<GrantRoleRequest> captor = ArgumentCaptor.forClass(GrantRoleRequest.class);
      verify(stub).grantRole(captor.capture());
      assertThat(captor.getValue().getRoleName()).isEqualTo(ROLE_NAME);
      assertThat(captor.getValue().getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void shouldThrowAnalyticsException_WhenGrpcCallFails() {
      when(stub.grantRole(any()))
          .thenThrow(new StatusRuntimeException(io.grpc.Status.PERMISSION_DENIED));

      assertThatThrownBy(() -> client.grantRole(ROLE_NAME, USERNAME))
          .isInstanceOf(AnalyticsException.class)
          .extracting(e -> ((AnalyticsException) e).getErrorCode())
          .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
    }
  }

  @Nested
  class RevokeRole {
    @Test
    void shouldSucceed_WhenRevoked() {
      RevokeRoleRequest expectedRequest =
          RevokeRoleRequest.newBuilder().setRoleName(ROLE_NAME).setUsername(USERNAME).build();
      when(stub.revokeRole(expectedRequest)).thenReturn(RevokeRoleResponse.newBuilder().build());

      client.revokeRole(ROLE_NAME, USERNAME);

      ArgumentCaptor<RevokeRoleRequest> captor = ArgumentCaptor.forClass(RevokeRoleRequest.class);
      verify(stub).revokeRole(captor.capture());
      assertThat(captor.getValue().getRoleName()).isEqualTo(ROLE_NAME);
      assertThat(captor.getValue().getUsername()).isEqualTo(USERNAME);
    }

    @Test
    void shouldThrowAnalyticsException_WhenGrpcCallFails() {
      when(stub.revokeRole(any()))
          .thenThrow(new StatusRuntimeException(io.grpc.Status.PERMISSION_DENIED));

      assertThatThrownBy(() -> client.revokeRole(ROLE_NAME, USERNAME))
          .isInstanceOf(AnalyticsException.class)
          .extracting(e -> ((AnalyticsException) e).getErrorCode())
          .isEqualTo(AnalyticsErrorCode.CLIENT_INTERNAL_ERROR);
    }
  }
}
