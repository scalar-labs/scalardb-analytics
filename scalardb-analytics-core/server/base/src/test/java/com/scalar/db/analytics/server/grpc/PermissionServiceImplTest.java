/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.domain.authz.EffectivePermission;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.PermissionSource;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantCatalogPermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantDataSourcePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionType;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeCatalogPermissionRequest;
import com.scalar.db.analytics.usecase.authz.PermissionUseCase;
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

class PermissionServiceImplTest {
  private static final UUID USER_ID = UUID.randomUUID();
  private PermissionUseCase useCase;
  private PermissionServiceGrpc.PermissionServiceBlockingStub stub;

  @BeforeEach
  void setUp() throws IOException {
    useCase = mock(PermissionUseCase.class);
    PermissionServiceImpl service = new PermissionServiceImpl(useCase);

    String serverName = InProcessServerBuilder.generateName();
    InProcessServerBuilder.forName(serverName)
        .addService(service)
        .intercept(userIdInterceptor())
        .build()
        .start();

    Channel channel = InProcessChannelBuilder.forName(serverName).build();
    stub = PermissionServiceGrpc.newBlockingStub(channel);
  }

  @Test
  void grantCatalogPermission_shouldDelegateToUseCase() {
    String granteeName = "alice";

    stub.grantCatalogPermission(
        GrantCatalogPermissionRequest.newBuilder()
            .setGranteeType(
                com.scalar.db.analytics.grpc.generated.auth.v1.GranteeType.GRANTEE_TYPE_USER)
            .setGranteeName(granteeName)
            .setPermission(PermissionType.PERMISSION_TYPE_CATALOG_READ)
            .setCatalogName("prod")
            .build());

    verify(useCase)
        .grantCatalogPermission(
            USER_ID, GranteeType.USER, granteeName, Permission.CATALOG_READ, "prod");
  }

  @Test
  void grantDataSourcePermission_shouldDelegateToUseCase() {
    String granteeName = "alice";

    stub.grantDataSourcePermission(
        GrantDataSourcePermissionRequest.newBuilder()
            .setGranteeType(
                com.scalar.db.analytics.grpc.generated.auth.v1.GranteeType.GRANTEE_TYPE_USER)
            .setGranteeName(granteeName)
            .setPermission(PermissionType.PERMISSION_TYPE_DATA_SOURCE_ADMIN)
            .setCatalogName("prod")
            .setDataSourceName("ds1")
            .build());

    verify(useCase)
        .grantDataSourcePermission(
            USER_ID, GranteeType.USER, granteeName, Permission.DATA_SOURCE_ADMIN, "prod", "ds1");
  }

  @Test
  void revokeCatalogPermission_shouldDelegateToUseCase() {
    String granteeName = "viewer";

    stub.revokeCatalogPermission(
        RevokeCatalogPermissionRequest.newBuilder()
            .setGranteeType(
                com.scalar.db.analytics.grpc.generated.auth.v1.GranteeType.GRANTEE_TYPE_ROLE)
            .setGranteeName(granteeName)
            .setPermission(PermissionType.PERMISSION_TYPE_DATA_SOURCE_ADMIN)
            .setCatalogName("prod")
            .build());

    verify(useCase)
        .revokeCatalogPermission(
            USER_ID, GranteeType.ROLE, granteeName, Permission.DATA_SOURCE_ADMIN, "prod");
  }

  @Test
  void listPermissions_shouldReturnMappedResults() {
    String username = "alice";
    UUID catalogId = UUID.randomUUID();
    UUID roleId = UUID.randomUUID();

    List<EffectivePermission> permissions =
        List.of(
            new EffectivePermission(
                Permission.CATALOG_READ, catalogId, PermissionSource.DIRECT, null, null),
            new EffectivePermission(
                Permission.DATA_SOURCE_READ,
                catalogId,
                PermissionSource.VIA_ROLE,
                roleId,
                "viewer"));
    when(useCase.listPermissions(USER_ID, username)).thenReturn(permissions);

    ListPermissionsResponse response =
        stub.listPermissions(ListPermissionsRequest.newBuilder().setUsername(username).build());

    assertThat(response.getPermissionsList()).hasSize(2);

    var direct = response.getPermissions(0);
    assertThat(direct.getPermission()).isEqualTo(PermissionType.PERMISSION_TYPE_CATALOG_READ);
    assertThat(direct.getResourceId()).isEqualTo(catalogId.toString());
    assertThat(direct.getSource())
        .isEqualTo(
            com.scalar.db.analytics.grpc.generated.auth.v1.PermissionSource
                .PERMISSION_SOURCE_DIRECT);

    var viaRole = response.getPermissions(1);
    assertThat(viaRole.getPermission()).isEqualTo(PermissionType.PERMISSION_TYPE_DATA_SOURCE_READ);
    assertThat(viaRole.getSource())
        .isEqualTo(
            com.scalar.db.analytics.grpc.generated.auth.v1.PermissionSource
                .PERMISSION_SOURCE_VIA_ROLE);
    assertThat(viaRole.getViaRoleId()).isEqualTo(roleId.toString());
    assertThat(viaRole.getViaRoleName()).isEqualTo("viewer");
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
