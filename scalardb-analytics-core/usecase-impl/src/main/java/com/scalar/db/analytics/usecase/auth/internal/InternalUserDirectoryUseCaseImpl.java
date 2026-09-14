/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth.internal;

import static java.util.Objects.requireNonNull;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.domain.auth.internal.InternalCredential;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Implementation of {@link InternalUserDirectoryUseCase}.
 *
 * <p>Manages internal-backend credentials only. Principal (AuthUser) management and the principal ↔
 * backend-user link are handled by {@code UserUseCase}.
 *
 * @param <T> the type of transaction context
 */
public class InternalUserDirectoryUseCaseImpl<T extends RepositoryTransactionContext>
    implements InternalUserDirectoryUseCase {

  private final InternalCredentialRepository<T> internalCredentialRepository;
  private final PasswordIdentityRepository<T> passwordIdentityRepository;
  private final RepositoryTransactionManager<T> txManager;
  private final PasswordEncoder passwordEncoder;
  private final AuthorizationService authorizationService;
  private final UserCascadeHelper<T> cascadeHelper;

  public InternalUserDirectoryUseCaseImpl(
      AuthUserRepository<T> authUserRepository,
      PasswordIdentityRepository<T> passwordIdentityRepository,
      InternalCredentialRepository<T> internalCredentialRepository,
      AccessControlEntryRepository<T> aceRepository,
      RoleAssignmentRepository<T> roleAssignmentRepository,
      AccessTokenRepository<T> accessTokenRepository,
      RepositoryTransactionManager<T> txManager,
      PasswordEncoder passwordEncoder,
      AuthorizationService authorizationService) {
    this.internalCredentialRepository = internalCredentialRepository;
    this.passwordIdentityRepository = passwordIdentityRepository;
    this.txManager = txManager;
    this.passwordEncoder = passwordEncoder;
    this.authorizationService = authorizationService;
    this.cascadeHelper =
        new UserCascadeHelper<>(
            aceRepository,
            roleAssignmentRepository,
            accessTokenRepository,
            internalCredentialRepository,
            passwordIdentityRepository,
            authUserRepository);
  }

  @Override
  public void createInternalBackendUser(UUID userId, String username, String password) {
    if (!authorizationService.authorizeSuperAdmin(userId)) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED, Map.of("user_id", userId.toString()));
    }
    txManager.withTransaction(
        ctx -> {
          if (internalCredentialRepository.findByUsername(ctx, username).isPresent()) {
            throw new AnalyticsException(
                AnalyticsErrorCode.USER_ALREADY_EXISTS, Map.of("username", username));
          }
          String passwordHash = requireNonNull(passwordEncoder.encode(password));
          InternalCredential credential = new InternalCredential(username, passwordHash);
          internalCredentialRepository.create(ctx, credential);
          return null;
        });
  }

  @Override
  public boolean deleteInternalBackendUser(UUID userId, String username, boolean cascade) {
    if (!authorizationService.authorizeSuperAdmin(userId)) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED, Map.of("user_id", userId.toString()));
    }
    return txManager.withTransaction(
        ctx -> {
          Optional<InternalCredential> credentialOpt =
              internalCredentialRepository.findByUsername(ctx, username);
          if (credentialOpt.isEmpty()) {
            return false;
          }

          Optional<PasswordIdentity> identityOpt =
              passwordIdentityRepository.findByBackendAndBackendUserId(
                  ctx, PasswordBackendType.INTERNAL, username);
          boolean linked = identityOpt.isPresent();

          if (linked && !cascade) {
            throw new AnalyticsException(
                AnalyticsErrorCode.USER_NOT_EMPTY, Map.of("username", username));
          }

          if (linked) {
            UUID principalId = identityOpt.get().userId();
            cascadeHelper.deleteInternalCredentialAndIdentity(ctx, username, principalId);
            // Unlinking removes a way the principal authenticates, so revoke its access
            // tokens on the safe side. Tokens are transient per-principal session state, so
            // clearing them is cheap (re-authentication re-issues them); symmetric with
            // UserUseCaseImpl.unlinkBackendUser.
            cascadeHelper.deleteAccessTokens(ctx, principalId);
          } else {
            internalCredentialRepository.deleteByUsername(ctx, username);
          }
          return true;
        });
  }
}
