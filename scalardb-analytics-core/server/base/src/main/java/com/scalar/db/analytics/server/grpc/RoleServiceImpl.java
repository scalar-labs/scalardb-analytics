/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListRolesRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListRolesResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeRoleByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.RoleServiceGrpc.RoleServiceImplBase;
import com.scalar.db.analytics.server.grpc.mapper.auth.RoleMapper;
import com.scalar.db.analytics.usecase.authz.RoleUseCase;
import java.util.UUID;

public class RoleServiceImpl extends RoleServiceImplBase {
  private final RoleUseCase roleUseCase;

  public RoleServiceImpl(RoleUseCase roleUseCase) {
    this.roleUseCase = roleUseCase;
  }

  @Override
  public void createRole(
      CreateRoleRequest request, io.grpc.stub.StreamObserver<CreateRoleResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    Role role = roleUseCase.createRole(userId, request.getRoleName());

    CreateRoleResponse response =
        CreateRoleResponse.newBuilder().setRole(RoleMapper.INSTANCE.toProto(role)).build();

    responseObserver.onNext(response);
    responseObserver.onCompleted();
  }

  @Override
  public void deleteRole(
      DeleteRoleRequest request, io.grpc.stub.StreamObserver<DeleteRoleResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    boolean deleted = roleUseCase.deleteRole(userId, request.getRoleName());

    responseObserver.onNext(DeleteRoleResponse.newBuilder().setDeleted(deleted).build());
    responseObserver.onCompleted();
  }

  @Override
  public void deleteRoleById(
      DeleteRoleByIdRequest request,
      io.grpc.stub.StreamObserver<DeleteRoleByIdResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    boolean deleted = roleUseCase.deleteRoleById(userId, UUID.fromString(request.getRoleId()));

    responseObserver.onNext(DeleteRoleByIdResponse.newBuilder().setDeleted(deleted).build());
    responseObserver.onCompleted();
  }

  @Override
  public void listRoles(
      ListRolesRequest request, io.grpc.stub.StreamObserver<ListRolesResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    ListRolesResponse.Builder builder = ListRolesResponse.newBuilder();

    roleUseCase.listRoles(userId).stream()
        .map(RoleMapper.INSTANCE::toProto)
        .forEach(builder::addRoles);

    responseObserver.onNext(builder.build());
    responseObserver.onCompleted();
  }

  @Override
  public void grantRole(
      GrantRoleRequest request, io.grpc.stub.StreamObserver<GrantRoleResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    roleUseCase.grantRole(userId, request.getRoleName(), request.getUsername());

    responseObserver.onNext(GrantRoleResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void grantRoleById(
      GrantRoleByIdRequest request,
      io.grpc.stub.StreamObserver<GrantRoleByIdResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    roleUseCase.grantRoleById(
        userId, UUID.fromString(request.getRoleId()), UUID.fromString(request.getUserId()));

    responseObserver.onNext(GrantRoleByIdResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void revokeRole(
      RevokeRoleRequest request, io.grpc.stub.StreamObserver<RevokeRoleResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    roleUseCase.revokeRole(userId, request.getRoleName(), request.getUsername());

    responseObserver.onNext(RevokeRoleResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  @Override
  public void revokeRoleById(
      RevokeRoleByIdRequest request,
      io.grpc.stub.StreamObserver<RevokeRoleByIdResponse> responseObserver) {
    UUID userId = getAuthenticatedUserId();
    roleUseCase.revokeRoleById(
        userId, UUID.fromString(request.getRoleId()), UUID.fromString(request.getUserId()));

    responseObserver.onNext(RevokeRoleByIdResponse.getDefaultInstance());
    responseObserver.onCompleted();
  }

  private UUID getAuthenticatedUserId() {
    return AuthenticationInterceptor.AUTHENTICATED_USER_ID.get();
  }
}
