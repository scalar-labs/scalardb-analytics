/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.authz;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.scalar.db.analytics.api.authz.EffectivePermission;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantCatalogPermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantDataSourcePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantNamespacePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantPermissionByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GrantTablePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.GranteeType;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsForRoleByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsForRoleByIdResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsForRoleRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsForRoleResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.ListPermissionsResponse;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionServiceGrpc;
import com.scalar.db.analytics.grpc.generated.auth.v1.PermissionType;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeCatalogPermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeDataSourcePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeNamespacePermissionRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokePermissionByIdRequest;
import com.scalar.db.analytics.grpc.generated.auth.v1.RevokeTablePermissionRequest;
import com.scalar.db.analytics.sdk.exception.GrpcExceptionMapper;
import io.grpc.Channel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * Default implementation of {@link PermissionClient}.
 *
 * <p>This class is for internal SDK use only and should not be instantiated directly by SDK users.
 */
public class BlockingGrpcPermissionClient implements PermissionClient {

  private static final Map<String, GranteeType> GRANTEE_TYPE_MAP = new HashMap<>();
  private static final Map<String, PermissionType> PERMISSION_TYPE_MAP = new HashMap<>();
  private static final Map<PermissionType, String> PERMISSION_TYPE_TO_NAME = new HashMap<>();
  private static final Map<com.scalar.db.analytics.grpc.generated.auth.v1.PermissionSource, String>
      PERMISSION_SOURCE_TO_NAME = new HashMap<>();

  static {
    GRANTEE_TYPE_MAP.put("USER", GranteeType.GRANTEE_TYPE_USER);
    GRANTEE_TYPE_MAP.put("ROLE", GranteeType.GRANTEE_TYPE_ROLE);

    PERMISSION_TYPE_MAP.put("CATALOG_READ", PermissionType.PERMISSION_TYPE_CATALOG_READ);
    PERMISSION_TYPE_MAP.put("CATALOG_WRITE", PermissionType.PERMISSION_TYPE_CATALOG_WRITE);
    PERMISSION_TYPE_MAP.put("CATALOG_ADMIN", PermissionType.PERMISSION_TYPE_CATALOG_ADMIN);
    PERMISSION_TYPE_MAP.put("DATA_SOURCE_READ", PermissionType.PERMISSION_TYPE_DATA_SOURCE_READ);
    PERMISSION_TYPE_MAP.put("DATA_SOURCE_ADMIN", PermissionType.PERMISSION_TYPE_DATA_SOURCE_ADMIN);
    PERMISSION_TYPE_MAP.put("NAMESPACE_READ", PermissionType.PERMISSION_TYPE_NAMESPACE_READ);
    PERMISSION_TYPE_MAP.put("TABLE_READ", PermissionType.PERMISSION_TYPE_TABLE_READ);

    PERMISSION_TYPE_TO_NAME.put(PermissionType.PERMISSION_TYPE_CATALOG_READ, "CATALOG_READ");
    PERMISSION_TYPE_TO_NAME.put(PermissionType.PERMISSION_TYPE_CATALOG_WRITE, "CATALOG_WRITE");
    PERMISSION_TYPE_TO_NAME.put(PermissionType.PERMISSION_TYPE_CATALOG_ADMIN, "CATALOG_ADMIN");
    PERMISSION_TYPE_TO_NAME.put(
        PermissionType.PERMISSION_TYPE_DATA_SOURCE_READ, "DATA_SOURCE_READ");
    PERMISSION_TYPE_TO_NAME.put(
        PermissionType.PERMISSION_TYPE_DATA_SOURCE_ADMIN, "DATA_SOURCE_ADMIN");
    PERMISSION_TYPE_TO_NAME.put(PermissionType.PERMISSION_TYPE_NAMESPACE_READ, "NAMESPACE_READ");
    PERMISSION_TYPE_TO_NAME.put(PermissionType.PERMISSION_TYPE_TABLE_READ, "TABLE_READ");

    PERMISSION_SOURCE_TO_NAME.put(
        com.scalar.db.analytics.grpc.generated.auth.v1.PermissionSource.PERMISSION_SOURCE_DIRECT,
        "DIRECT");
    PERMISSION_SOURCE_TO_NAME.put(
        com.scalar.db.analytics.grpc.generated.auth.v1.PermissionSource.PERMISSION_SOURCE_VIA_ROLE,
        "VIA_ROLE");
  }

  private final PermissionServiceGrpc.PermissionServiceBlockingStub stub;

  public BlockingGrpcPermissionClient(Channel channel) {
    this.stub = PermissionServiceGrpc.newBlockingStub(channel);
  }

  @VisibleForTesting
  BlockingGrpcPermissionClient(PermissionServiceGrpc.PermissionServiceBlockingStub stub) {
    this.stub = stub;
  }

  @Override
  public void grantCatalogPermission(
      String granteeType, String granteeName, String permission, String catalogName) {
    try {
      GrantCatalogPermissionRequest request =
          GrantCatalogPermissionRequest.newBuilder()
              .setGranteeType(toProtoGranteeType(granteeType))
              .setGranteeName(granteeName)
              .setPermission(toProtoPermissionType(permission))
              .setCatalogName(catalogName)
              .build();
      stub.grantCatalogPermission(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "grantCatalogPermission",
          ImmutableMap.of("granteeName", granteeName, "catalogName", catalogName));
    }
  }

  @Override
  public void grantDataSourcePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName) {
    try {
      GrantDataSourcePermissionRequest request =
          GrantDataSourcePermissionRequest.newBuilder()
              .setGranteeType(toProtoGranteeType(granteeType))
              .setGranteeName(granteeName)
              .setPermission(toProtoPermissionType(permission))
              .setCatalogName(catalogName)
              .setDataSourceName(dataSourceName)
              .build();
      stub.grantDataSourcePermission(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "grantDataSourcePermission",
          ImmutableMap.of(
              "granteeName",
              granteeName,
              "catalogName",
              catalogName,
              "dataSourceName",
              dataSourceName));
    }
  }

  @Override
  public void grantNamespacePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames) {
    try {
      GrantNamespacePermissionRequest request =
          GrantNamespacePermissionRequest.newBuilder()
              .setGranteeType(toProtoGranteeType(granteeType))
              .setGranteeName(granteeName)
              .setPermission(toProtoPermissionType(permission))
              .setCatalogName(catalogName)
              .setDataSourceName(dataSourceName)
              .addAllNamespaceNames(namespaceNames)
              .build();
      stub.grantNamespacePermission(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "grantNamespacePermission",
          ImmutableMap.of(
              "granteeName",
              granteeName,
              "catalogName",
              catalogName,
              "dataSourceName",
              dataSourceName));
    }
  }

  @Override
  public void grantTablePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName) {
    try {
      GrantTablePermissionRequest request =
          GrantTablePermissionRequest.newBuilder()
              .setGranteeType(toProtoGranteeType(granteeType))
              .setGranteeName(granteeName)
              .setPermission(toProtoPermissionType(permission))
              .setCatalogName(catalogName)
              .setDataSourceName(dataSourceName)
              .addAllNamespaceNames(namespaceNames)
              .setTableName(tableName)
              .build();
      stub.grantTablePermission(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "grantTablePermission",
          ImmutableMap.of(
              "granteeName",
              granteeName,
              "catalogName",
              catalogName,
              "dataSourceName",
              dataSourceName,
              "tableName",
              tableName));
    }
  }

  @Override
  public void grantPermissionById(
      String granteeType, String granteeId, String permission, String resourceId) {
    try {
      GrantPermissionByIdRequest request =
          GrantPermissionByIdRequest.newBuilder()
              .setGranteeType(toProtoGranteeType(granteeType))
              .setGranteeId(granteeId)
              .setPermission(toProtoPermissionType(permission))
              .setResourceId(resourceId)
              .build();
      stub.grantPermissionById(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "grantPermissionById",
          ImmutableMap.of("granteeId", granteeId, "resourceId", resourceId));
    }
  }

  @Override
  public void revokeCatalogPermission(
      String granteeType, String granteeName, String permission, String catalogName) {
    try {
      RevokeCatalogPermissionRequest request =
          RevokeCatalogPermissionRequest.newBuilder()
              .setGranteeType(toProtoGranteeType(granteeType))
              .setGranteeName(granteeName)
              .setPermission(toProtoPermissionType(permission))
              .setCatalogName(catalogName)
              .build();
      stub.revokeCatalogPermission(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "revokeCatalogPermission",
          ImmutableMap.of("granteeName", granteeName, "catalogName", catalogName));
    }
  }

  @Override
  public void revokeDataSourcePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName) {
    try {
      RevokeDataSourcePermissionRequest request =
          RevokeDataSourcePermissionRequest.newBuilder()
              .setGranteeType(toProtoGranteeType(granteeType))
              .setGranteeName(granteeName)
              .setPermission(toProtoPermissionType(permission))
              .setCatalogName(catalogName)
              .setDataSourceName(dataSourceName)
              .build();
      stub.revokeDataSourcePermission(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "revokeDataSourcePermission",
          ImmutableMap.of(
              "granteeName",
              granteeName,
              "catalogName",
              catalogName,
              "dataSourceName",
              dataSourceName));
    }
  }

  @Override
  public void revokeNamespacePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames) {
    try {
      RevokeNamespacePermissionRequest request =
          RevokeNamespacePermissionRequest.newBuilder()
              .setGranteeType(toProtoGranteeType(granteeType))
              .setGranteeName(granteeName)
              .setPermission(toProtoPermissionType(permission))
              .setCatalogName(catalogName)
              .setDataSourceName(dataSourceName)
              .addAllNamespaceNames(namespaceNames)
              .build();
      stub.revokeNamespacePermission(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "revokeNamespacePermission",
          ImmutableMap.of(
              "granteeName",
              granteeName,
              "catalogName",
              catalogName,
              "dataSourceName",
              dataSourceName));
    }
  }

  @Override
  public void revokeTablePermission(
      String granteeType,
      String granteeName,
      String permission,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName) {
    try {
      RevokeTablePermissionRequest request =
          RevokeTablePermissionRequest.newBuilder()
              .setGranteeType(toProtoGranteeType(granteeType))
              .setGranteeName(granteeName)
              .setPermission(toProtoPermissionType(permission))
              .setCatalogName(catalogName)
              .setDataSourceName(dataSourceName)
              .addAllNamespaceNames(namespaceNames)
              .setTableName(tableName)
              .build();
      stub.revokeTablePermission(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "revokeTablePermission",
          ImmutableMap.of(
              "granteeName",
              granteeName,
              "catalogName",
              catalogName,
              "dataSourceName",
              dataSourceName,
              "tableName",
              tableName));
    }
  }

  @Override
  public void revokePermissionById(
      String granteeType, String granteeId, String permission, String resourceId) {
    try {
      RevokePermissionByIdRequest request =
          RevokePermissionByIdRequest.newBuilder()
              .setGranteeType(toProtoGranteeType(granteeType))
              .setGranteeId(granteeId)
              .setPermission(toProtoPermissionType(permission))
              .setResourceId(resourceId)
              .build();
      stub.revokePermissionById(request);
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e,
          "revokePermissionById",
          ImmutableMap.of("granteeId", granteeId, "resourceId", resourceId));
    }
  }

  @Override
  public List<EffectivePermission> listPermissions(String username) {
    try {
      ListPermissionsRequest request =
          ListPermissionsRequest.newBuilder().setUsername(username).build();
      ListPermissionsResponse response = stub.listPermissions(request);
      return toEffectivePermissions(response.getPermissionsList());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "listPermissions", ImmutableMap.of("username", username));
    }
  }

  @Override
  public List<EffectivePermission> listPermissionsById(String userId) {
    try {
      ListPermissionsByIdRequest request =
          ListPermissionsByIdRequest.newBuilder().setUserId(userId).build();
      ListPermissionsByIdResponse response = stub.listPermissionsById(request);
      return toEffectivePermissions(response.getPermissionsList());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "listPermissionsById", ImmutableMap.of("userId", userId));
    }
  }

  @Override
  public List<EffectivePermission> listPermissionsForRole(String roleName) {
    try {
      ListPermissionsForRoleRequest request =
          ListPermissionsForRoleRequest.newBuilder().setRoleName(roleName).build();
      ListPermissionsForRoleResponse response = stub.listPermissionsForRole(request);
      return toEffectivePermissions(response.getPermissionsList());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "listPermissionsForRole", ImmutableMap.of("roleName", roleName));
    }
  }

  @Override
  public List<EffectivePermission> listPermissionsForRoleById(String roleId) {
    try {
      ListPermissionsForRoleByIdRequest request =
          ListPermissionsForRoleByIdRequest.newBuilder().setRoleId(roleId).build();
      ListPermissionsForRoleByIdResponse response = stub.listPermissionsForRoleById(request);
      return toEffectivePermissions(response.getPermissionsList());
    } catch (Exception e) {
      throw GrpcExceptionMapper.toAnalyticsException(
          e, "listPermissionsForRoleById", ImmutableMap.of("roleId", roleId));
    }
  }

  private static List<EffectivePermission> toEffectivePermissions(
      List<com.scalar.db.analytics.grpc.generated.auth.v1.EffectivePermission> protos) {
    List<EffectivePermission> result = new ArrayList<>();
    for (com.scalar.db.analytics.grpc.generated.auth.v1.EffectivePermission proto : protos) {
      result.add(toDomain(proto));
    }
    return result;
  }

  private static GranteeType toProtoGranteeType(String granteeType) {
    GranteeType result =
        granteeType == null ? null : GRANTEE_TYPE_MAP.get(granteeType.toUpperCase(Locale.ROOT));
    if (result == null) {
      throw new AnalyticsException(
          AnalyticsErrorCode.INVALID_ARGUMENT,
          ImmutableMap.of("field", "grantee_type", "value", String.valueOf(granteeType)));
    }
    return result;
  }

  private static PermissionType toProtoPermissionType(String permission) {
    PermissionType result =
        permission == null ? null : PERMISSION_TYPE_MAP.get(permission.toUpperCase(Locale.ROOT));
    if (result == null) {
      throw new AnalyticsException(
          AnalyticsErrorCode.INVALID_ARGUMENT,
          ImmutableMap.of("field", "permission", "value", String.valueOf(permission)));
    }
    return result;
  }

  private static EffectivePermission toDomain(
      com.scalar.db.analytics.grpc.generated.auth.v1.EffectivePermission proto) {
    String permissionName = PERMISSION_TYPE_TO_NAME.get(proto.getPermission());
    if (permissionName == null) {
      throw new AnalyticsException(
          AnalyticsErrorCode.CLIENT_INTERNAL_ERROR,
          ImmutableMap.of("permission_type", String.valueOf(proto.getPermission())));
    }
    String sourceName = PERMISSION_SOURCE_TO_NAME.get(proto.getSource());
    if (sourceName == null) {
      throw new AnalyticsException(
          AnalyticsErrorCode.CLIENT_INTERNAL_ERROR,
          ImmutableMap.of("permission_source", String.valueOf(proto.getSource())));
    }
    @Nullable String viaRoleId = proto.hasViaRoleId() ? proto.getViaRoleId() : null;
    @Nullable String viaRoleName = proto.hasViaRoleName() ? proto.getViaRoleName() : null;
    return new EffectivePermission(
        permissionName, proto.getResourceId(), sourceName, viaRoleId, viaRoleName);
  }
}
