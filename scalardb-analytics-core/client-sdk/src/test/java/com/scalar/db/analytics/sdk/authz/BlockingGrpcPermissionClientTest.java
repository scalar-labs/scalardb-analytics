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

import com.scalar.db.analytics.api.authz.EffectivePermission;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantCatalogPermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantCatalogPermissionResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantDataSourcePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantDataSourcePermissionResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.GranteeType;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionType;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeCatalogPermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeCatalogPermissionResponse;
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
class BlockingGrpcPermissionClientTest {

  private static final String USERNAME = "alice";
  private static final String ROLE_NAME = "viewer";
  private static final String CATALOG_NAME = "prod";
  private static final String DATA_SOURCE_NAME = "ds1";

  @Mock private PermissionServiceGrpc.PermissionServiceBlockingStub stub;

  private BlockingGrpcPermissionClient client;

  @BeforeEach
  void setUp() {
    client = new BlockingGrpcPermissionClient(stub);
  }

  @Nested
  class GrantCatalogPermission {
    @Test
    void shouldSucceed_WhenGrantedToUser() {
      GrantCatalogPermissionRequest expectedRequest =
          GrantCatalogPermissionRequest.newBuilder()
              .setGranteeType(GranteeType.GRANTEE_TYPE_USER)
              .setGranteeName(USERNAME)
              .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_READ)
              .setCatalogName(CATALOG_NAME)
              .build();
      when(stub.grantCatalogPermission(expectedRequest))
          .thenReturn(GrantCatalogPermissionResponse.getDefaultInstance());

      client.grantCatalogPermission("USER", USERNAME, "CATALOG_READ", CATALOG_NAME);

      ArgumentCaptor<GrantCatalogPermissionRequest> captor =
          ArgumentCaptor.forClass(GrantCatalogPermissionRequest.class);
      verify(stub).grantCatalogPermission(captor.capture());
      assertThat(captor.getValue().getGranteeName()).isEqualTo(USERNAME);
      assertThat(captor.getValue().getCatalogName()).isEqualTo(CATALOG_NAME);
    }

    @Test
    void shouldSucceed_WhenGrantedToRole() {
      GrantCatalogPermissionRequest expectedRequest =
          GrantCatalogPermissionRequest.newBuilder()
              .setGranteeType(GranteeType.GRANTEE_TYPE_ROLE)
              .setGranteeName(ROLE_NAME)
              .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_ADMIN)
              .setCatalogName(CATALOG_NAME)
              .build();
      when(stub.grantCatalogPermission(expectedRequest))
          .thenReturn(GrantCatalogPermissionResponse.getDefaultInstance());

      client.grantCatalogPermission("ROLE", ROLE_NAME, "CATALOG_ADMIN", CATALOG_NAME);

      ArgumentCaptor<GrantCatalogPermissionRequest> captor =
          ArgumentCaptor.forClass(GrantCatalogPermissionRequest.class);
      verify(stub).grantCatalogPermission(captor.capture());
      assertThat(captor.getValue().getGranteeName()).isEqualTo(ROLE_NAME);
      assertThat(captor.getValue().getCatalogName()).isEqualTo(CATALOG_NAME);
    }

    @Test
    void shouldThrowAnalyticsException_WhenInvalidGranteeType() {
      assertThatThrownBy(
              () ->
                  client.grantCatalogPermission("INVALID", USERNAME, "CATALOG_READ", CATALOG_NAME))
          .isInstanceOf(AnalyticsException.class)
          .extracting(e -> ((AnalyticsException) e).getErrorCode())
          .isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT);
    }

    @Test
    void shouldThrowAnalyticsException_WhenInvalidPermissionType() {
      assertThatThrownBy(
              () -> client.grantCatalogPermission("USER", USERNAME, "INVALID_PERM", CATALOG_NAME))
          .isInstanceOf(AnalyticsException.class)
          .extracting(e -> ((AnalyticsException) e).getErrorCode())
          .isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT);
    }

    @Test
    void shouldThrowAnalyticsException_WhenGrpcCallFails() {
      when(stub.grantCatalogPermission(any()))
          .thenThrow(new StatusRuntimeException(io.grpc.Status.PERMISSION_DENIED));

      assertThatThrownBy(
              () -> client.grantCatalogPermission("USER", USERNAME, "CATALOG_READ", CATALOG_NAME))
          .isInstanceOf(AnalyticsException.class);
    }
  }

  @Nested
  class GrantDataSourcePermission {
    @Test
    void shouldSucceed_WhenGrantedToUser() {
      GrantDataSourcePermissionRequest expectedRequest =
          GrantDataSourcePermissionRequest.newBuilder()
              .setGranteeType(GranteeType.GRANTEE_TYPE_USER)
              .setGranteeName(USERNAME)
              .setPermission(PermissionType.PERMISSION_TYPE_DATA_SOURCE_ADMIN)
              .setCatalogName(CATALOG_NAME)
              .setDataSourceName(DATA_SOURCE_NAME)
              .build();
      when(stub.grantDataSourcePermission(expectedRequest))
          .thenReturn(GrantDataSourcePermissionResponse.getDefaultInstance());

      client.grantDataSourcePermission(
          "USER", USERNAME, "DATA_SOURCE_ADMIN", CATALOG_NAME, DATA_SOURCE_NAME);

      ArgumentCaptor<GrantDataSourcePermissionRequest> captor =
          ArgumentCaptor.forClass(GrantDataSourcePermissionRequest.class);
      verify(stub).grantDataSourcePermission(captor.capture());
      assertThat(captor.getValue().getDataSourceName()).isEqualTo(DATA_SOURCE_NAME);
    }
  }

  @Nested
  class RevokeCatalogPermission {
    @Test
    void shouldSucceed_WhenRevoked() {
      RevokeCatalogPermissionRequest expectedRequest =
          RevokeCatalogPermissionRequest.newBuilder()
              .setGranteeType(GranteeType.GRANTEE_TYPE_USER)
              .setGranteeName(USERNAME)
              .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_READ)
              .setCatalogName(CATALOG_NAME)
              .build();
      when(stub.revokeCatalogPermission(expectedRequest))
          .thenReturn(RevokeCatalogPermissionResponse.getDefaultInstance());

      client.revokeCatalogPermission("USER", USERNAME, "CATALOG_READ", CATALOG_NAME);

      ArgumentCaptor<RevokeCatalogPermissionRequest> captor =
          ArgumentCaptor.forClass(RevokeCatalogPermissionRequest.class);
      verify(stub).revokeCatalogPermission(captor.capture());
      assertThat(captor.getValue().getGranteeName()).isEqualTo(USERNAME);
      assertThat(captor.getValue().getCatalogName()).isEqualTo(CATALOG_NAME);
    }

    @Test
    void shouldThrowAnalyticsException_WhenGrpcCallFails() {
      when(stub.revokeCatalogPermission(any()))
          .thenThrow(new StatusRuntimeException(io.grpc.Status.PERMISSION_DENIED));

      assertThatThrownBy(
              () -> client.revokeCatalogPermission("USER", USERNAME, "CATALOG_READ", CATALOG_NAME))
          .isInstanceOf(AnalyticsException.class);
    }
  }

  @Nested
  class ListPermissions {
    @Test
    void shouldReturnPermissions_WhenSuccessful() {
      String viaRoleId = UUID.randomUUID().toString();
      String resourceId = UUID.randomUUID().toString();
      com.scalar.db.analytics.grpc.generated.auth.v1.EffectivePermission directPerm =
          com.scalar.db.analytics.grpc.generated.auth.v1.EffectivePermission.newBuilder()
              .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_READ)
              .setResourceId(resourceId)
              .setSource(
                  com.scalar.db.analytics.grpc.generated.auth.v1.PermissionSource
                      .PERMISSION_SOURCE_DIRECT)
              .build();
      com.scalar.db.analytics.grpc.generated.auth.v1.EffectivePermission viaRolePerm =
          com.scalar.db.analytics.grpc.generated.auth.v1.EffectivePermission.newBuilder()
              .setPermission(PermissionType.PERMISSION_TYPE_TABLE_READ)
              .setResourceId(resourceId)
              .setSource(
                  com.scalar.db.analytics.grpc.generated.auth.v1.PermissionSource
                      .PERMISSION_SOURCE_VIA_ROLE)
              .setViaRoleId(viaRoleId)
              .setViaRoleName("test-role")
              .build();
      ListPermissionsResponse response =
          ListPermissionsResponse.newBuilder()
              .addPermissions(directPerm)
              .addPermissions(viaRolePerm)
              .build();
      when(stub.listPermissions(ListPermissionsRequest.newBuilder().setUsername(USERNAME).build()))
          .thenReturn(response);

      List<EffectivePermission> permissions = client.listPermissions(USERNAME);

      assertThat(permissions).hasSize(2);
      assertThat(permissions.get(0).getPermission()).isEqualTo("CATALOG_READ");
      assertThat(permissions.get(0).getSource()).isEqualTo("DIRECT");
      assertThat(permissions.get(0).getViaRoleId()).isNull();
      assertThat(permissions.get(1).getPermission()).isEqualTo("TABLE_READ");
      assertThat(permissions.get(1).getSource()).isEqualTo("VIA_ROLE");
      assertThat(permissions.get(1).getViaRoleId()).isEqualTo(viaRoleId);
      assertThat(permissions.get(1).getViaRoleName()).isEqualTo("test-role");
    }

    @Test
    void shouldReturnEmptyList_WhenNoPermissions() {
      ListPermissionsResponse response = ListPermissionsResponse.newBuilder().build();
      when(stub.listPermissions(any())).thenReturn(response);

      List<EffectivePermission> permissions = client.listPermissions(USERNAME);

      assertThat(permissions).isEmpty();
    }

    @Test
    void shouldThrowAnalyticsException_WhenGrpcCallFails() {
      when(stub.listPermissions(any()))
          .thenThrow(new StatusRuntimeException(io.grpc.Status.INTERNAL));

      assertThatThrownBy(() -> client.listPermissions(USERNAME))
          .isInstanceOf(AnalyticsException.class);
    }
  }
}
