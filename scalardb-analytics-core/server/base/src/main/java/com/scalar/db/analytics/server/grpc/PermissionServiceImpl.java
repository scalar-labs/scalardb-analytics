/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.domain.authz.EffectivePermission;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantCatalogPermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantCatalogPermissionResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantDataSourcePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantDataSourcePermissionResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantNamespacePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantNamespacePermissionResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantPermissionByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantPermissionByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantTablePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantTablePermissionResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsForRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsForRoleByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsForRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsForRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionServiceGrpc.PermissionServiceImplBase;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeCatalogPermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeCatalogPermissionResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeDataSourcePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeDataSourcePermissionResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeNamespacePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeNamespacePermissionResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokePermissionByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokePermissionByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeTablePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeTablePermissionResponse;
import com.scalar.db.analytics.server.grpc.mapper.auth.PermissionMapper;
import com.scalar.db.analytics.usecase.authz.PermissionUseCase;
import java.util.List;
import java.util.UUID;

public class PermissionServiceImpl extends PermissionServiceImplBase {
  private final PermissionUseCase permissionUseCase;

  public PermissionServiceImpl(PermissionUseCase permissionUseCase) {
    this.permissionUseCase = permissionUseCase;
  }

  @Override
  public void grantCatalogPermission(
      GrantCatalogPermissionRequest request,
      io.grpc.stub.StreamObserver<GrantCatalogPermissionResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    GranteeType granteeType =
        PermissionMapper.INSTANCE.toDomainGranteeType(request.getGranteeType());
    Permission permission = PermissionMapper.INSTANCE.toDomainPermission(request.getPermission());
    permissionUseCase.grantCatalogPermission(
        userId, granteeType, request.getGranteeName(), permission, request.getCatalogName());
    responseObserver.onNext(GrantCatalogPermissionResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void grantDataSourcePermission(
      GrantDataSourcePermissionRequest request,
      io.grpc.stub.StreamObserver<GrantDataSourcePermissionResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    GranteeType granteeType =
        PermissionMapper.INSTANCE.toDomainGranteeType(request.getGranteeType());
    Permission permission = PermissionMapper.INSTANCE.toDomainPermission(request.getPermission());
    permissionUseCase.grantDataSourcePermission(
        userId,
        granteeType,
        request.getGranteeName(),
        permission,
        request.getCatalogName(),
        request.getDataSourceName());
    responseObserver.onNext(GrantDataSourcePermissionResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void grantNamespacePermission(
      GrantNamespacePermissionRequest request,
      io.grpc.stub.StreamObserver<GrantNamespacePermissionResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    GranteeType granteeType =
        PermissionMapper.INSTANCE.toDomainGranteeType(request.getGranteeType());
    Permission permission = PermissionMapper.INSTANCE.toDomainPermission(request.getPermission());
    permissionUseCase.grantNamespacePermission(
        userId,
        granteeType,
        request.getGranteeName(),
        permission,
        request.getCatalogName(),
        request.getDataSourceName(),
        request.getNamespaceNamesList());
    responseObserver.onNext(GrantNamespacePermissionResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void grantTablePermission(
      GrantTablePermissionRequest request,
      io.grpc.stub.StreamObserver<GrantTablePermissionResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    GranteeType granteeType =
        PermissionMapper.INSTANCE.toDomainGranteeType(request.getGranteeType());
    Permission permission = PermissionMapper.INSTANCE.toDomainPermission(request.getPermission());
    permissionUseCase.grantTablePermission(
        userId,
        granteeType,
        request.getGranteeName(),
        permission,
        request.getCatalogName(),
        request.getDataSourceName(),
        request.getNamespaceNamesList(),
        request.getTableName());
    responseObserver.onNext(GrantTablePermissionResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void grantPermissionById(
      GrantPermissionByIdRequest request,
      io.grpc.stub.StreamObserver<GrantPermissionByIdResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    GranteeType granteeType =
        PermissionMapper.INSTANCE.toDomainGranteeType(request.getGranteeType());
    Permission permission = PermissionMapper.INSTANCE.toDomainPermission(request.getPermission());

    permissionUseCase.grantPermissionById(
        userId,
        granteeType,
        UUID.fromString(request.getGranteeId()),
        permission,
        UUID.fromString(request.getResourceId()));

    responseObserver.onNext(GrantPermissionByIdResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void revokeCatalogPermission(
      RevokeCatalogPermissionRequest request,
      io.grpc.stub.StreamObserver<RevokeCatalogPermissionResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    GranteeType granteeType =
        PermissionMapper.INSTANCE.toDomainGranteeType(request.getGranteeType());
    Permission permission = PermissionMapper.INSTANCE.toDomainPermission(request.getPermission());
    permissionUseCase.revokeCatalogPermission(
        userId, granteeType, request.getGranteeName(), permission, request.getCatalogName());
    responseObserver.onNext(RevokeCatalogPermissionResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void revokeDataSourcePermission(
      RevokeDataSourcePermissionRequest request,
      io.grpc.stub.StreamObserver<RevokeDataSourcePermissionResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    GranteeType granteeType =
        PermissionMapper.INSTANCE.toDomainGranteeType(request.getGranteeType());
    Permission permission = PermissionMapper.INSTANCE.toDomainPermission(request.getPermission());
    permissionUseCase.revokeDataSourcePermission(
        userId,
        granteeType,
        request.getGranteeName(),
        permission,
        request.getCatalogName(),
        request.getDataSourceName());
    responseObserver.onNext(RevokeDataSourcePermissionResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void revokeNamespacePermission(
      RevokeNamespacePermissionRequest request,
      io.grpc.stub.StreamObserver<RevokeNamespacePermissionResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    GranteeType granteeType =
        PermissionMapper.INSTANCE.toDomainGranteeType(request.getGranteeType());
    Permission permission = PermissionMapper.INSTANCE.toDomainPermission(request.getPermission());
    permissionUseCase.revokeNamespacePermission(
        userId,
        granteeType,
        request.getGranteeName(),
        permission,
        request.getCatalogName(),
        request.getDataSourceName(),
        request.getNamespaceNamesList());
    responseObserver.onNext(RevokeNamespacePermissionResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void revokeTablePermission(
      RevokeTablePermissionRequest request,
      io.grpc.stub.StreamObserver<RevokeTablePermissionResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    GranteeType granteeType =
        PermissionMapper.INSTANCE.toDomainGranteeType(request.getGranteeType());
    Permission permission = PermissionMapper.INSTANCE.toDomainPermission(request.getPermission());
    permissionUseCase.revokeTablePermission(
        userId,
        granteeType,
        request.getGranteeName(),
        permission,
        request.getCatalogName(),
        request.getDataSourceName(),
        request.getNamespaceNamesList(),
        request.getTableName());
    responseObserver.onNext(RevokeTablePermissionResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void revokePermissionById(
      RevokePermissionByIdRequest request,
      io.grpc.stub.StreamObserver<RevokePermissionByIdResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    GranteeType granteeType =
        PermissionMapper.INSTANCE.toDomainGranteeType(request.getGranteeType());
    Permission permission = PermissionMapper.INSTANCE.toDomainPermission(request.getPermission());

    permissionUseCase.revokePermissionById(
        userId,
        granteeType,
        UUID.fromString(request.getGranteeId()),
        permission,
        UUID.fromString(request.getResourceId()));

    responseObserver.onNext(RevokePermissionByIdResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void listPermissions(
      ListPermissionsRequest request,
      io.grpc.stub.StreamObserver<ListPermissionsResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    List<EffectivePermission> permissions =
        permissionUseCase.listPermissions(userId, request.getUsername());

    ListPermissionsResponse.Builder builder = ListPermissionsResponse.newBuilder();
    permissions.stream().map(PermissionMapper.INSTANCE::toProto).forEach(builder::addPermissions);

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void listPermissionsById(
      ListPermissionsByIdRequest request,
      io.grpc.stub.StreamObserver<ListPermissionsByIdResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    List<EffectivePermission> permissions =
        permissionUseCase.listPermissionsById(userId, UUID.fromString(request.getUserId()));

    ListPermissionsByIdResponse.Builder builder = ListPermissionsByIdResponse.newBuilder();
    permissions.stream().map(PermissionMapper.INSTANCE::toProto).forEach(builder::addPermissions);

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void listPermissionsForRole(
      ListPermissionsForRoleRequest request,
      io.grpc.stub.StreamObserver<ListPermissionsForRoleResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    List<EffectivePermission> permissions =
        permissionUseCase.listPermissionsForRole(userId, request.getRoleName());

    ListPermissionsForRoleResponse.Builder builder = ListPermissionsForRoleResponse.newBuilder();
    permissions.stream().map(PermissionMapper.INSTANCE::toProto).forEach(builder::addPermissions);

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void listPermissionsForRoleById(
      ListPermissionsForRoleByIdRequest request,
      io.grpc.stub.StreamObserver<ListPermissionsForRoleByIdResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    List<EffectivePermission> permissions =
        permissionUseCase.listPermissionsForRoleById(userId, UUID.fromString(request.getRoleId()));

    ListPermissionsForRoleByIdResponse.Builder builder =
        ListPermissionsForRoleByIdResponse.newBuilder();
    permissions.stream().map(PermissionMapper.INSTANCE::toProto).forEach(builder::addPermissions);

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  private UUID getAuthenticatedUserId() {
    return AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
  }
}
