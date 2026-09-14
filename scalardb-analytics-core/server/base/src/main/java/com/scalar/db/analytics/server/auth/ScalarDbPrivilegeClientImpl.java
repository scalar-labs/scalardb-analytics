/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.auth;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.DistributedTransactionAdminGrpc;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.HasPrivilegeRequest;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.HasPrivilegeResponse;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.Privilege;
import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.RequestHeader;
import com.scalar.db.analytics.service.authz.ScalarDbPrivilegeClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;

/**
 * gRPC implementation of {@link ScalarDbPrivilegeClient}.
 *
 * <p>Uses the caller-provided auth token to call the HasPrivilege RPC. Results are cached with a
 * configurable TTL.
 */
public class ScalarDbPrivilegeClientImpl implements ScalarDbPrivilegeClient {

  static final Duration DEFAULT_CACHE_TTL = Duration.ofSeconds(60);
  static final long DEFAULT_CACHE_MAX_SIZE = 10_000;

  private final DistributedTransactionAdminGrpc.DistributedTransactionAdminBlockingStub adminStub;
  private final long deadlineMillis;
  private final Cache<CacheKey, Boolean> privilegeCache;

  public ScalarDbPrivilegeClientImpl(
      DistributedTransactionAdminGrpc.DistributedTransactionAdminBlockingStub adminStub,
      long deadlineMillis) {
    this(adminStub, deadlineMillis, DEFAULT_CACHE_TTL, DEFAULT_CACHE_MAX_SIZE);
  }

  public ScalarDbPrivilegeClientImpl(
      DistributedTransactionAdminGrpc.DistributedTransactionAdminBlockingStub adminStub,
      long deadlineMillis,
      Duration cacheTtl,
      long cacheMaxSize) {
    this.adminStub = adminStub;
    this.deadlineMillis = deadlineMillis;
    this.privilegeCache =
        Caffeine.newBuilder().expireAfterWrite(cacheTtl).maximumSize(cacheMaxSize).build();
  }

  @Override
  public boolean hasSelectPrivilege(
      String authToken, String username, String namespaceName, @Nullable String tableName) {
    CacheKey cacheKey = new CacheKey(username, namespaceName, tableName);
    Boolean cached = privilegeCache.getIfPresent(cacheKey);
    if (cached != null) {
      return cached;
    }

    boolean result = callHasPrivilege(authToken, username, namespaceName, tableName);
    privilegeCache.put(cacheKey, result);
    return result;
  }

  private boolean callHasPrivilege(
      String authToken, String username, String namespaceName, @Nullable String tableName) {
    try {
      HasPrivilegeRequest.Builder requestBuilder =
          HasPrivilegeRequest.newBuilder()
              .setRequestHeader(RequestHeader.newBuilder().setAuthToken(authToken).build())
              .setUsername(username)
              .setNamespaceName(namespaceName)
              .setPrivilege(Privilege.PRIVILEGE_READ);

      if (tableName != null) {
        requestBuilder.setTableName(tableName);
      }

      HasPrivilegeResponse response =
          adminStub
              .withDeadlineAfter(deadlineMillis, TimeUnit.MILLISECONDS)
              .hasPrivilege(requestBuilder.build());

      return response.getHasPrivilege();
    } catch (StatusRuntimeException e) {
      throw mapException(e);
    }
  }

  private record CacheKey(String username, String namespaceName, @Nullable String tableName) {}

  private static AnalyticsException mapException(StatusRuntimeException e) {
    Status.Code code = e.getStatus().getCode();
    return switch (code) {
      case UNAUTHENTICATED ->
          new AnalyticsException(AnalyticsErrorCode.SCALARDB_BACKEND_TOKEN_EXPIRED, e);
      case UNAVAILABLE, DEADLINE_EXCEEDED ->
          new AnalyticsException(AnalyticsErrorCode.SCALARDB_CLUSTER_UNAVAILABLE, e);
      default -> new AnalyticsException(AnalyticsErrorCode.SCALARDB_PRIVILEGE_CHECK_FAILURE, e);
    };
  }
}
