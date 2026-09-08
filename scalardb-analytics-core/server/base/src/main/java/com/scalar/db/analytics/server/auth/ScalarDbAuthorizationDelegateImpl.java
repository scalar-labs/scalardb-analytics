/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.auth;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.service.authz.PermissionRequirement;
import com.scalar.db.analytics.service.authz.ScalarDbAuthorizationDelegate;
import com.scalar.db.analytics.service.authz.ScalarDbPrivilegeClient;
import com.scalar.db.analytics.usecase.auth.BackendAuthResult;
import com.scalar.db.analytics.usecase.auth.BackendTokenStore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link ScalarDbAuthorizationDelegate} that checks ScalarDB Cluster privileges.
 *
 * <p>Metadata (usernames, namespace/table names) is resolved within a DB transaction. The gRPC
 * privilege checks are performed outside the transaction to avoid holding DB connections during
 * network I/O.
 *
 * @param <T> the type of transaction context
 */
public class ScalarDbAuthorizationDelegateImpl<T extends RepositoryTransactionContext>
    implements ScalarDbAuthorizationDelegate {

  private static final Logger logger =
      LoggerFactory.getLogger(ScalarDbAuthorizationDelegateImpl.class);

  private final NamespaceRepository<T> namespaceRepository;
  private final TableRepository<T> tableRepository;
  private final PasswordIdentityRepository<T> passwordIdentityRepository;
  private final RepositoryTransactionManager<T> txManager;
  private final ScalarDbPrivilegeClient privilegeClient;
  private final BackendTokenStore backendTokenStore;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public ScalarDbAuthorizationDelegateImpl(
      NamespaceRepository<T> namespaceRepository,
      TableRepository<T> tableRepository,
      PasswordIdentityRepository<T> passwordIdentityRepository,
      RepositoryTransactionManager<T> txManager,
      ScalarDbPrivilegeClient privilegeClient,
      BackendTokenStore backendTokenStore) {
    this.namespaceRepository = namespaceRepository;
    this.tableRepository = tableRepository;
    this.passwordIdentityRepository = passwordIdentityRepository;
    this.txManager = txManager;
    this.privilegeClient = privilegeClient;
    this.backendTokenStore = backendTokenStore;
  }

  @Override
  public boolean authorize(UUID userId, List<PermissionRequirement> requirements) {
    // Phase 1: Resolve metadata inside a DB transaction
    // Returns Optional.empty() if no ScalarDB identity exists for this user,
    // or Optional.of(list) with the resolved privilege checks.
    Optional<List<PrivilegeCheck>> maybeChecks =
        txManager.withTransaction(
            ctx -> {
              Optional<String> scalarDbUsername = findScalarDbUsername(ctx, userId);
              if (scalarDbUsername.isEmpty()) {
                return Optional.empty();
              }
              return Optional.of(resolveChecks(ctx, scalarDbUsername.get(), requirements));
            });

    if (maybeChecks.isEmpty()) {
      // No ScalarDB Cluster identity found for this user — they cannot have ScalarDB
      // privileges, so delegation must deny access.
      return false;
    }

    List<PrivilegeCheck> checks = maybeChecks.get();
    if (checks.isEmpty()) {
      return true;
    }

    // Phase 2: Retrieve backend auth result and perform gRPC privilege checks outside the
    // transaction
    Optional<BackendAuthResult> authResult = backendTokenStore.get(userId);
    if (authResult.isEmpty() || authResult.get().backendToken() == null) {
      // No backend token is available (e.g., after a server restart while the client's
      // Analytics token is still valid). Treat this as an expired token so the client
      // receives UNAUTHENTICATED and re-authenticates, which refreshes the backend token.
      throw new AnalyticsException(AnalyticsErrorCode.SCALARDB_BACKEND_TOKEN_EXPIRED);
    }
    String backendToken = authResult.get().backendToken();

    for (PrivilegeCheck check : checks) {
      boolean hasPrivilege;
      try {
        hasPrivilege =
            privilegeClient.hasSelectPrivilege(
                backendToken, check.username, check.namespaceName, check.tableName);
      } catch (AnalyticsException e) {
        if (e.getErrorCode() == AnalyticsErrorCode.SCALARDB_BACKEND_TOKEN_EXPIRED) {
          // The backend token has expired. Remove it so the client receives UNAUTHENTICATED
          // and re-authenticates, which refreshes both the Analytics token and the backend token.
          backendTokenStore.remove(userId);
        }
        throw e;
      }
      if (!hasPrivilege) {
        if (check.tableName != null) {
          logger.warn(
              "ScalarDB privilege denied: user={}, namespace={}, table={}",
              check.username,
              check.namespaceName,
              check.tableName);
        } else {
          logger.warn(
              "ScalarDB privilege denied: user={}, namespace={}",
              check.username,
              check.namespaceName);
        }
        return false;
      }
    }
    return true;
  }

  private List<PrivilegeCheck> resolveChecks(
      T ctx, String scalarDbUsername, List<PermissionRequirement> requirements) {
    List<PrivilegeCheck> checks = new ArrayList<>();
    for (PermissionRequirement req : requirements) {
      ResourceRef ref = req.resource();

      if (!ScalarDbProvider.TYPE.equals(ref.dataSourceProviderType())) {
        continue;
      }

      if (ref.resourceType() == ResourceType.NAMESPACE) {
        resolveNamespaceCheck(ctx, scalarDbUsername, ref.resourceId()).ifPresent(checks::add);
      } else if (ref.resourceType() == ResourceType.TABLE) {
        resolveTableCheck(ctx, scalarDbUsername, ref.resourceId()).ifPresent(checks::add);
      }
    }
    return checks;
  }

  private Optional<String> findScalarDbUsername(T ctx, UUID userId) {
    List<PasswordIdentity> identities = passwordIdentityRepository.findByUserId(ctx, userId);
    return identities.stream()
        .filter(id -> id.backend() == PasswordBackendType.SCALARDB_CLUSTER)
        .map(PasswordIdentity::backendUserId)
        .findFirst();
  }

  private Optional<PrivilegeCheck> resolveNamespaceCheck(
      T ctx, String scalarDbUsername, UUID namespaceId) {
    Optional<Namespace> namespace = namespaceRepository.findById(ctx, namespaceId);
    if (namespace.isEmpty()) {
      return Optional.empty();
    }
    String namespaceName = namespace.get().getNames().get(0);
    return Optional.of(new PrivilegeCheck(scalarDbUsername, namespaceName, null));
  }

  private Optional<PrivilegeCheck> resolveTableCheck(T ctx, String scalarDbUsername, UUID tableId) {
    Optional<Table> table = tableRepository.findById(ctx, tableId);
    if (table.isEmpty()) {
      return Optional.empty();
    }
    UUID namespaceId = table.get().getInfo().getNamespaceId();
    Optional<Namespace> namespace = namespaceRepository.findById(ctx, namespaceId);
    if (namespace.isEmpty()) {
      return Optional.empty();
    }
    String namespaceName = namespace.get().getNames().get(0);
    String tableName = table.get().getInfo().getName();
    return Optional.of(new PrivilegeCheck(scalarDbUsername, namespaceName, tableName));
  }

  private record PrivilegeCheck(
      String username, String namespaceName, @Nullable String tableName) {}
}
