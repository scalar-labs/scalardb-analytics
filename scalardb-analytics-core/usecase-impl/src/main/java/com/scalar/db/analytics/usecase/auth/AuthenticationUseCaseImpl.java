/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

import com.scalar.db.analytics.api.auth.AccessToken;
import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.IssuedToken;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.domain.authz.BuiltInRole;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link AuthenticationUseCase} for password authentication.
 *
 * <p>This implementation uses a {@link PasswordAuthenticationBackend} strategy to verify
 * credentials, allowing for different backend implementations (internal, ScalarDB Cluster, etc.).
 *
 * @param <T> the type of transaction context
 */
public class AuthenticationUseCaseImpl<T extends RepositoryTransactionContext>
    implements AuthenticationUseCase {

  private static final Logger logger = LoggerFactory.getLogger(AuthenticationUseCaseImpl.class);

  private final AuthUserRepository<T> authUserRepository;
  private final PasswordIdentityRepository<T> passwordIdentityRepository;
  private final AccessTokenRepository<T> accessTokenRepository;
  private final RoleAssignmentRepository<T> roleAssignmentRepository;
  private final RepositoryTransactionManager<T> txManager;
  private final PasswordAuthenticationBackend passwordBackend;
  private final Duration tokenTtl;
  private final @Nullable String initialAdminUsername;
  private final @Nullable BackendTokenStore backendTokenStore;

  @SuppressWarnings("UnusedVariable") // ErrorProne false positive: record components are accessed
  private record ResolvedIdentity(PasswordIdentity identity, boolean jitProvisioned) {}

  public AuthenticationUseCaseImpl(
      AuthUserRepository<T> authUserRepository,
      PasswordIdentityRepository<T> passwordIdentityRepository,
      AccessTokenRepository<T> accessTokenRepository,
      RoleAssignmentRepository<T> roleAssignmentRepository,
      RepositoryTransactionManager<T> txManager,
      PasswordAuthenticationBackend passwordBackend,
      Duration tokenTtl,
      @Nullable String initialAdminUsername) {
    this(
        authUserRepository,
        passwordIdentityRepository,
        accessTokenRepository,
        roleAssignmentRepository,
        txManager,
        passwordBackend,
        tokenTtl,
        initialAdminUsername,
        null);
  }

  public AuthenticationUseCaseImpl(
      AuthUserRepository<T> authUserRepository,
      PasswordIdentityRepository<T> passwordIdentityRepository,
      AccessTokenRepository<T> accessTokenRepository,
      RoleAssignmentRepository<T> roleAssignmentRepository,
      RepositoryTransactionManager<T> txManager,
      PasswordAuthenticationBackend passwordBackend,
      Duration tokenTtl,
      @Nullable String initialAdminUsername,
      @Nullable BackendTokenStore backendTokenStore) {
    this.authUserRepository = authUserRepository;
    this.passwordIdentityRepository = passwordIdentityRepository;
    this.accessTokenRepository = accessTokenRepository;
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.txManager = txManager;
    this.passwordBackend = passwordBackend;
    this.tokenTtl = tokenTtl;
    this.initialAdminUsername = initialAdminUsername;
    this.backendTokenStore = backendTokenStore;
  }

  private static final int MAX_JIT_RETRIES = 1;

  @Override
  public AccessToken authenticateWithPassword(PasswordCredential credential) {
    // Step 1: Verify credentials via backend (returns backend user ID + optional token)
    BackendAuthResult authResult =
        passwordBackend.verifyCredential(credential.getUsername(), credential.getPassword());

    // Step 2: Resolve user via PasswordIdentity and issue token.
    // Retry the entire transaction on conflict to handle concurrent JIT provisioning:
    // on the first attempt, JIT provisioning may fail due to a duplicate identity created by
    // a concurrent request; on retry, the existing identity is found and used.
    AnalyticsException lastException = null;
    for (int attempt = 0; attempt <= MAX_JIT_RETRIES; attempt++) {
      try {
        AccessToken accessToken =
            txManager.withTransaction(
                ctx -> {
                  ResolvedIdentity resolved = resolveIdentity(ctx, authResult.backendUserId());
                  if (resolved.jitProvisioned()) {
                    assignInitialAdminIfApplicable(
                        ctx, resolved.identity(), authResult.backendUserId());
                  }
                  return issueToken(ctx, resolved.identity());
                });

        // Store backend auth result outside the transaction (in-memory only)
        if (backendTokenStore != null && authResult.backendToken() != null) {
          backendTokenStore.store(UUID.fromString(accessToken.getUserId()), authResult);
        }

        return accessToken;
      } catch (AnalyticsException e) {
        if (isConflictError(e)) {
          lastException = e;
          logger.info(
              "Transaction failed due to conflict for backend user id: {}, attempt {}/{}",
              authResult.backendUserId(),
              attempt + 1,
              MAX_JIT_RETRIES + 1);
        } else {
          throw e;
        }
      }
    }
    throw lastException;
  }

  private ResolvedIdentity resolveIdentity(T ctx, String backendUserId) {
    PasswordBackendType backendType = passwordBackend.getBackendType();
    Optional<PasswordIdentity> existing =
        passwordIdentityRepository.findByBackendAndBackendUserId(ctx, backendType, backendUserId);

    // Happy path: both PasswordIdentity and its owning AuthUser exist.
    if (existing.isPresent()) {
      PasswordIdentity identity = existing.get();
      if (authUserRepository.findById(ctx, identity.userId()).isPresent()) {
        return new ResolvedIdentity(identity, false);
      }
      // PasswordIdentity exists but AuthUser has been deleted (orphaned).
      // This can happen because ScalarDB does not support foreign key constraints
      // or cascade deletes, so a programming error could leave stale records.
      // Fall through to the JIT / non-JIT handling below.
    }

    if (passwordBackend instanceof JitProvisioningBackend jitBackend) {
      // JIT backend: the external system is the source of truth, so we can auto-recover.
      // Clean up the orphaned record (if any) and re-provision a fresh AuthUser + identity.
      if (existing.isPresent()) {
        logger.warn(
            "Deleting orphaned password identity for user id: {}, backend: {}, backend user id: {}",
            existing.get().userId(),
            backendType,
            backendUserId);
        // Scope the cleanup to this backend so identities the principal holds on other backends
        // are not collaterally removed.
        passwordIdentityRepository.deleteByUserIdAndBackend(
            ctx, existing.get().userId(), backendType);
      }
      PasswordIdentity identity = jitProvision(ctx, jitBackend, backendUserId);
      return new ResolvedIdentity(identity, true);
    } else {
      // Non-JIT backend: the local database is the source of truth and there is no
      // automatic recovery path. Log the situation so operators can investigate, but
      // return a generic credentials error to avoid leaking internal state to clients.
      if (existing.isPresent()) {
        logger.error(
            "Orphaned password identity detected for user id: {}, backend user id: {}. "
                + "The PasswordIdentity record exists but the owning AuthUser has been deleted.",
            existing.get().userId(),
            backendUserId);
      } else {
        logger.warn(
            "Authentication failed: password identity not found for backend user id: {}",
            backendUserId);
      }
      throw new AnalyticsException(AnalyticsErrorCode.AUTHENTICATION_FAILED);
    }
  }

  private PasswordIdentity jitProvision(
      T ctx, JitProvisioningBackend jitBackend, String backendUserId) {
    logger.info("JIT provisioning user for backend user id: {}", backendUserId);
    AuthUser user = AuthUser.create(backendUserId);
    authUserRepository.create(ctx, user);

    PasswordIdentity identity =
        PasswordIdentity.create(user.getUserId(), jitBackend.getBackendType(), backendUserId);
    passwordIdentityRepository.create(ctx, identity);
    return identity;
  }

  private void assignInitialAdminIfApplicable(
      T ctx, PasswordIdentity identity, String backendUserId) {
    if (initialAdminUsername == null || !backendUserId.equals(initialAdminUsername)) {
      return;
    }
    if (!roleAssignmentRepository.findByRoleId(ctx, BuiltInRole.SUPERADMIN_ID).isEmpty()) {
      return;
    }
    roleAssignmentRepository.create(
        ctx, new RoleAssignment(identity.userId(), BuiltInRole.SUPERADMIN_ID));
    logger.info(
        "Assigned initial admin '{}' to SUPERADMIN role via JIT provisioning", backendUserId);
  }

  private AccessToken issueToken(T ctx, PasswordIdentity identity) {
    Instant expiresAt = Instant.now().plus(tokenTtl).truncatedTo(ChronoUnit.MILLIS);
    IssuedToken issuedToken = IssuedToken.issue(identity.userId(), expiresAt);

    // Replace any existing token for the user (1:1 user-to-token relationship, ADR-0005)
    accessTokenRepository.deleteByUserId(ctx, identity.userId());
    accessTokenRepository.create(ctx, issuedToken);

    return new AccessToken(issuedToken.token(), expiresAt, identity.userId().toString());
  }

  private static boolean isConflictError(AnalyticsException e) {
    return e.getErrorCode() == AnalyticsErrorCode.USER_ALREADY_EXISTS;
  }
}
