/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.authz;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.analytics.api.authz.Role;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.CreateRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.DeleteRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListRolesRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListRolesResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RoleServiceGrpc;
import com.scalar.db.analytics.sdk.exception.GrpcExceptionMapper;
import io.grpc.Channel;
import java.util.ArrayList;
import java.util.List;

/**
 * Default implementation of {@link RoleClient}.
 *
 * <p>This class is for internal SDK use only and should not be instantiated directly by SDK users.
 */
public class BlockingGrpcRoleClient implements RoleClient {
  private final RoleServiceGrpc.RoleServiceBlockingStub stub;

  public BlockingGrpcRoleClient(Channel channel) {
    this.stub = RoleServiceGrpc.newBlockingStub(channel);
  }

  @VisibleForTesting
  BlockingGrpcRoleClient(RoleServiceGrpc.RoleServiceBlockingStub stub) {
    this.stub = stub;
  }

  @Override
  public Role createRole(String roleName) {
    try {
      CreateRoleRequest request = CreateRoleRequest.newBuilder().setRoleName(roleName).build();
      CreateRoleResponse response = stub.createRole(request);
      return toDomain(response.getRole());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "createRole", ImmutableMap.of("roleName", roleName));
    }
  }

  @Override
  public boolean deleteRole(String roleName) {
    try {
      DeleteRoleRequest request = DeleteRoleRequest.newBuilder().setRoleName(roleName).build();
      DeleteRoleResponse response = stub.deleteRole(request);
      return response.getDeleted();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "deleteRole", ImmutableMap.of("roleName", roleName));
    }
  }

  @Override
  public boolean deleteRoleById(String roleId) {
    try {
      DeleteRoleByIdRequest request = DeleteRoleByIdRequest.newBuilder().setRoleId(roleId).build();
      DeleteRoleByIdResponse response = stub.deleteRoleById(request);
      return response.getDeleted();
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "deleteRoleById", ImmutableMap.of("roleId", roleId));
    }
  }

  @Override
  public List<Role> listRoles() {
    try {
      ListRolesRequest request = ListRolesRequest.newBuilder().build();
      ListRolesResponse response = stub.listRoles(request);
      List<Role> roles = new ArrayList<>();
      for (com.scalar.db.analytics.grpc.generated.auth.v1.Role protoRole :
          response.getRolesList()) {
        roles.add(toDomain(protoRole));
      }
      return roles;
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(e, "listRoles");
    }
  }

  @Override
  public void grantRole(String roleName, String username) {
    try {
      GrantRoleRequest request =
          GrantRoleRequest.newBuilder().setRoleName(roleName).setUsername(username).build();
      stub.grantRole(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "grantRole", ImmutableMap.of("roleName", roleName, "username", username));
    }
  }

  @Override
  public void grantRoleById(String roleId, String userId) {
    try {
      GrantRoleByIdRequest request =
          GrantRoleByIdRequest.newBuilder().setRoleId(roleId).setUserId(userId).build();
      stub.grantRoleById(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "grantRoleById", ImmutableMap.of("roleId", roleId, "userId", userId));
    }
  }

  @Override
  public void revokeRole(String roleName, String username) {
    try {
      RevokeRoleRequest request =
          RevokeRoleRequest.newBuilder().setRoleName(roleName).setUsername(username).build();
      stub.revokeRole(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "revokeRole", ImmutableMap.of("roleName", roleName, "username", username));
    }
  }

  @Override
  public void revokeRoleById(String roleId, String userId) {
    try {
      RevokeRoleByIdRequest request =
          RevokeRoleByIdRequest.newBuilder().setRoleId(roleId).setUserId(userId).build();
      stub.revokeRoleById(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "revokeRoleById", ImmutableMap.of("roleId", roleId, "userId", userId));
    }
  }

  private static Role toDomain(com.scalar.db.analytics.grpc.generated.auth.v1.Role proto) {
    return new Role(proto.getId(), proto.getName(), proto.getBuiltIn());
  }
}
