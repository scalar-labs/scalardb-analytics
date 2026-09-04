/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

import static java.util.Objects.requireNonNull;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.auth.UserDetail;
import com.scalar.db.analytics.api.auth.UserInfo;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.domain.auth.internal.InternalCredential;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.usecase.auth.internal.UserCascadeHelper;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Implementation of {@link UserUseCase}.
 *
 * @param <T> the type of transaction context
 */
public class UserUseCaseImpl<T extends RepositoryTransactionContext> implements UserUseCase {

  private final PasswordIdentityRepository<T> passwordIdentityRepository;
  private final AuthUserRepository<T> authUserRepository;
  private final RoleAssignmentRepository<T> roleAssignmentRepository;
  private final RoleRepository<T> roleRepository;
  private final InternalCredentialRepository<T> internalCredentialRepository;
  private final AccessControlEntryRepository<T> aceRepository;
  private final RepositoryTransactionManager<T> txManager;
  private final AuthorizationService authorizationService;
  private final PasswordEncoder passwordEncoder;
  private final UserCascadeHelper<T> cascadeHelper;

  public UserUseCaseImpl(
      PasswordIdentityRepository<T> passwordIdentityRepository,
      AuthUserRepository<T> authUserRepository,
      RoleAssignmentRepository<T> roleAssignmentRepository,
      RoleRepository<T> roleRepository,
      InternalCredentialRepository<T> internalCredentialRepository,
      AccessControlEntryRepository<T> aceRepository,
      AccessTokenRepository<T> accessTokenRepository,
      RepositoryTransactionManager<T> txManager,
      AuthorizationService authorizationService,
      PasswordEncoder passwordEncoder) {
    this.passwordIdentityRepository = passwordIdentityRepository;
    this.authUserRepository = authUserRepository;
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.roleRepository = roleRepository;
    this.internalCredentialRepository = internalCredentialRepository;
    this.aceRepository = aceRepository;
    this.txManager = txManager;
    this.authorizationService = authorizationService;
    this.passwordEncoder = passwordEncoder;
    this.cascadeHelper =
        new UserCascadeHelper<>(
            aceRepository,
            roleAssignmentRepository,
            accessTokenRepository,
            internalCredentialRepository,
            passwordIdentityRepository,
            authUserRepository);
  }

  // ---------------- Read operations (unchanged behavior) ----------------

  @Override
  public List<UserInfo> listUsers(UUID userId) {
    requireSuperAdmin(userId);
    return txManager.withTransaction(
        (ctx) -> {
          List<UserInfo> result = new ArrayList<>();
          // List principals directly so that principals without any backend identity
          // (e.g. created via `user create` without a backend user) are still returned.
          for (AuthUser principal : authUserRepository.findAll(ctx)) {
            UUID principalId = principal.getUserId();
            // TODO: This is an N+1 query — each principal's identities are looked up
            // individually. A batch/join lookup should be introduced for large directories.
            // TODO: Currently surfaces only the first identity (mirrors describeUser). When
            // multiple backends per principal are supported, return all of them.
            List<PasswordIdentity> identities =
                passwordIdentityRepository.findByUserId(ctx, principalId);
            String backendUserId = null;
            PasswordBackendType backend = null;
            if (!identities.isEmpty()) {
              PasswordIdentity identity = identities.get(0);
              backendUserId = identity.backendUserId();
              backend = identity.backend();
            }
            result.add(
                new UserInfo(
                    principalId.toString(), backendUserId, backend, principal.getUsername()));
          }
          return result;
        });
  }

  @Override
  public Optional<UserDetail> describeUserById(UUID userId, UUID targetUserId) {
    requireSuperAdmin(userId);
    return txManager.withTransaction(
        (ctx) -> {
          Optional<AuthUser> authUser = authUserRepository.findById(ctx, targetUserId);
          if (authUser.isEmpty()) {
            return Optional.empty();
          }
          return Optional.of(buildUserDetail(ctx, authUser.get()));
        });
  }

  @Override
  public Optional<UserDetail> describeUserByName(UUID userId, String username) {
    requireSuperAdmin(userId);
    return txManager.withTransaction(
        (ctx) -> {
          Optional<AuthUser> authUser = authUserRepository.findByUsername(ctx, username);
          if (authUser.isEmpty()) {
            return Optional.empty();
          }
          return Optional.of(buildUserDetail(ctx, authUser.get()));
        });
  }

  // ---------------- Write operations: principal ----------------

  @Override
  public AuthUser createUser(UUID userId, String username) {
    requireSuperAdmin(userId);
    return txManager.withTransaction(
        ctx -> {
          if (authUserRepository.findByUsername(ctx, username).isPresent()) {
            throw new AnalyticsException(
                AnalyticsErrorCode.USER_ALREADY_EXISTS, Map.of("username", username));
          }
          AuthUser user = AuthUser.create(username);
          authUserRepository.create(ctx, user);
          return user;
        });
  }

  @Override
  public boolean deleteUser(UUID userId, String username, boolean cascade) {
    requireSuperAdmin(userId);
    return txManager.withTransaction(
        ctx -> {
          Optional<AuthUser> authUser = authUserRepository.findByUsername(ctx, username);
          if (authUser.isEmpty()) {
            return false;
          }
          return doDeleteUser(ctx, authUser.get(), cascade);
        });
  }

  @Override
  public boolean deleteUserById(UUID userId, UUID targetUserId, boolean cascade) {
    requireSuperAdmin(userId);
    return txManager.withTransaction(
        ctx -> {
          Optional<AuthUser> authUser = authUserRepository.findById(ctx, targetUserId);
          if (authUser.isEmpty()) {
            return false;
          }
          return doDeleteUser(ctx, authUser.get(), cascade);
        });
  }

  @Override
  public AuthUser createUserWithBackendUser(
      UUID userId, String username, String backendUsername, String password) {
    requireSuperAdmin(userId);
    return txManager.withTransaction(
        ctx -> {
          if (authUserRepository.findByUsername(ctx, username).isPresent()) {
            throw new AnalyticsException(
                AnalyticsErrorCode.USER_ALREADY_EXISTS, Map.of("username", username));
          }
          if (internalCredentialRepository.findByUsername(ctx, backendUsername).isPresent()) {
            throw new AnalyticsException(
                AnalyticsErrorCode.USER_ALREADY_EXISTS, Map.of("username", backendUsername));
          }

          AuthUser user = AuthUser.create(username);
          authUserRepository.create(ctx, user);

          String passwordHash = requireNonNull(passwordEncoder.encode(password));
          InternalCredential credential = new InternalCredential(backendUsername, passwordHash);
          internalCredentialRepository.create(ctx, credential);

          PasswordIdentity identity =
              PasswordIdentity.create(
                  user.getUserId(), PasswordBackendType.INTERNAL, backendUsername);
          passwordIdentityRepository.create(ctx, identity);

          return user;
        });
  }

  // ---------------- Write operations: link ----------------

  // TODO(#481): linkBackendUser/unlinkBackendUser throw raw IllegalArgumentException, which is
  // inconsistent with the structured AnalyticsException(AnalyticsErrorCode.*) style used elsewhere
  // in this class. Reconcile when the server-side error-handling refactor (#481) migrates this
  // layer onto the unified error codes.
  @Override
  public void linkBackendUser(
      UUID userId, String username, String backendUsername, PasswordBackendType backend) {
    requireSuperAdmin(userId);
    txManager.withTransaction(
        ctx -> {
          AuthUser principal =
              authUserRepository
                  .findByUsername(ctx, username)
                  .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
          // TODO(#508): Only INTERNAL backend users are verified to exist here. For non-INTERNAL
          // backends (e.g. ScalarDB Cluster) the backend user is reference-only / managed
          // externally, so existence is not checked. Linking a non-existent user creates an inert
          // PasswordIdentity that does nothing until that backend user authenticates. Add an
          // existence check when a backend admin API is available. Relatedly, whether
          // external-backend login should require an explicit link (JIT-optional) is tracked in
          // #507.
          if (backend == PasswordBackendType.INTERNAL
              && internalCredentialRepository.findByUsername(ctx, backendUsername).isEmpty()) {
            throw new IllegalArgumentException(
                "Internal-backend user not found: " + backendUsername);
          }
          Optional<PasswordIdentity> existingForBackendUser =
              passwordIdentityRepository.findByBackendAndBackendUserId(
                  ctx, backend, backendUsername);
          if (existingForBackendUser.isPresent()) {
            throw new IllegalArgumentException(
                "Backend user already linked: backend="
                    + backend.name()
                    + ", backendUser="
                    + backendUsername);
          }
          // Also refuse if the principal already has an identity on the same backend
          // (one identity per principal per backend).
          for (PasswordIdentity existing :
              passwordIdentityRepository.findByUserId(ctx, principal.getUserId())) {
            if (existing.backend() == backend) {
              throw new IllegalArgumentException(
                  "User '"
                      + username
                      + "' is already linked to a backend user on backend "
                      + backend.name()
                      + " (existing backend user: "
                      + existing.backendUserId()
                      + ")");
            }
          }
          PasswordIdentity identity =
              PasswordIdentity.create(principal.getUserId(), backend, backendUsername);
          passwordIdentityRepository.create(ctx, identity);
          return null;
        });
  }

  @Override
  public void unlinkBackendUser(
      UUID userId, String username, String backendUsername, PasswordBackendType backend) {
    requireSuperAdmin(userId);
    txManager.withTransaction(
        ctx -> {
          AuthUser principal =
              authUserRepository
                  .findByUsername(ctx, username)
                  .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
          Optional<PasswordIdentity> identityOpt =
              passwordIdentityRepository.findByBackendAndBackendUserId(
                  ctx, backend, backendUsername);
          if (identityOpt.isEmpty() || !identityOpt.get().userId().equals(principal.getUserId())) {
            throw new IllegalArgumentException(
                "No link exists between user '"
                    + username
                    + "' and backend user '"
                    + backendUsername
                    + "' on backend "
                    + backend.name());
          }
          passwordIdentityRepository.deleteByUserIdAndBackend(ctx, principal.getUserId(), backend);
          // Unlinking removes a way the principal authenticates, so revoke its access tokens
          // on the safe side. Tokens are transient per-principal session state, so clearing
          // them is cheap (re-authentication re-issues them); symmetric with
          // InternalUserDirectoryUseCaseImpl.deleteInternalBackendUser(cascade=true).
          cascadeHelper.deleteAccessTokens(ctx, principal.getUserId());
          return null;
        });
  }

  // ---------------- Helpers ----------------

  private void requireSuperAdmin(UUID userId) {
    if (!authorizationService.authorizeSuperAdmin(userId)) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED, Map.of("user_id", userId.toString()));
    }
  }

  private boolean doDeleteUser(T ctx, AuthUser principal, boolean cascade) {
    UUID targetUserId = principal.getUserId();
    List<PasswordIdentity> identities = passwordIdentityRepository.findByUserId(ctx, targetUserId);
    List<RoleAssignment> assignments = roleAssignmentRepository.findByUserId(ctx, targetUserId);
    List<AccessControlEntry> directAces =
        aceRepository.findByGrantee(ctx, GranteeType.USER, targetUserId);

    if (!cascade && (!identities.isEmpty() || !assignments.isEmpty() || !directAces.isEmpty())) {
      throw new AnalyticsException(
          AnalyticsErrorCode.USER_NOT_EMPTY, Map.of("username", principal.getUsername()));
    }

    if (cascade) {
      for (PasswordIdentity identity : identities) {
        if (identity.backend() == PasswordBackendType.INTERNAL) {
          internalCredentialRepository.deleteByUsername(ctx, identity.backendUserId());
        }
      }
      // Clear all identities and auxiliary records (ACE, role assignments, access tokens).
      passwordIdentityRepository.deleteByUserId(ctx, targetUserId);
      cascadeHelper.deleteUserAuxiliaries(ctx, targetUserId);
    } else {
      // Guard has already ensured identities/assignments/direct ACEs are empty. Access tokens
      // are transient operational state that is invisible to the admin (no listing API) and
      // becomes meaningless once the principal is gone; always clean them up so the
      // principal-row delete does not leave orphan token rows pointing at a vanished userId.
      cascadeHelper.deleteAccessTokens(ctx, targetUserId);
    }
    cascadeHelper.deletePrincipalRow(ctx, targetUserId);
    return true;
  }

  private UserDetail buildUserDetail(T ctx, AuthUser authUser) {
    UUID targetUserId = authUser.getUserId();
    // TODO: Currently assumes one identity per user. If multiple backends per user are
    // supported in the future, this should return all identities or accept a backend
    // parameter.
    List<PasswordIdentity> identities = passwordIdentityRepository.findByUserId(ctx, targetUserId);
    String backendUserId = null;
    PasswordBackendType backend = null;
    if (!identities.isEmpty()) {
      PasswordIdentity identity = identities.get(0);
      backendUserId = identity.backendUserId();
      backend = identity.backend();
    }

    List<String> roleNames = resolveRoleNames(ctx, targetUserId);

    return new UserDetail(
        targetUserId.toString(), backendUserId, backend, roleNames, authUser.getUsername());
  }

  // TODO: This is an N+1 query — each roleId is looked up individually. A dedicated
  // QueryService (e.g., RoleAssignmentQueryService.findRolesForUser) should be
  // introduced to resolve role names in a single query. The number of roles assigned
  // to a user is small in practice (typically 1-3), so this is acceptable for now.
  private List<String> resolveRoleNames(T ctx, UUID targetUserId) {
    List<RoleAssignment> assignments = roleAssignmentRepository.findByUserId(ctx, targetUserId);
    List<String> roleNames = new ArrayList<>();
    for (RoleAssignment assignment : assignments) {
      roleRepository
          .findById(ctx, assignment.roleId())
          .ifPresent(role -> roleNames.add(role.name()));
    }
    return roleNames;
  }
}
