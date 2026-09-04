/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth.internal;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import java.util.UUID;

/**
 * Shared teardown helpers used by the cascade paths of user/internal-backend-user deletion.
 *
 * <p>Decomposes the formerly bundled {@code unregisterUser} teardown into three reusable operations
 * so both {@code UserUseCaseImpl.deleteUser(cascade=true)} and {@code
 * InternalUserDirectoryUseCaseImpl.deleteInternalBackendUser(cascade=true)} can compose them
 * without duplicating logic.
 *
 * <p>Each method operates within the caller's transaction context and only performs repository
 * deletions; it does not enforce authorization or pre-condition checks.
 *
 * @param <T> the type of transaction context
 */
public class UserCascadeHelper<T extends RepositoryTransactionContext> {

  private final AccessControlEntryRepository<T> aceRepository;
  private final RoleAssignmentRepository<T> roleAssignmentRepository;
  private final AccessTokenRepository<T> accessTokenRepository;
  private final InternalCredentialRepository<T> internalCredentialRepository;
  private final PasswordIdentityRepository<T> passwordIdentityRepository;
  private final AuthUserRepository<T> authUserRepository;

  public UserCascadeHelper(
      AccessControlEntryRepository<T> aceRepository,
      RoleAssignmentRepository<T> roleAssignmentRepository,
      AccessTokenRepository<T> accessTokenRepository,
      InternalCredentialRepository<T> internalCredentialRepository,
      PasswordIdentityRepository<T> passwordIdentityRepository,
      AuthUserRepository<T> authUserRepository) {
    this.aceRepository = aceRepository;
    this.roleAssignmentRepository = roleAssignmentRepository;
    this.accessTokenRepository = accessTokenRepository;
    this.internalCredentialRepository = internalCredentialRepository;
    this.passwordIdentityRepository = passwordIdentityRepository;
    this.authUserRepository = authUserRepository;
  }

  /**
   * Deletes all authorization-related auxiliary records attached to a principal: ACEs granted to
   * the user, role assignments held by the user, and any active access tokens.
   */
  public void deleteUserAuxiliaries(T ctx, UUID userId) {
    aceRepository.deleteByGrantee(ctx, GranteeType.USER, userId);
    roleAssignmentRepository.deleteByUserId(ctx, userId);
    accessTokenRepository.deleteByUserId(ctx, userId);
  }

  /**
   * Deletes only the principal's access tokens. Used by the non-cascade user-delete path to clean
   * up transient session state (which is invisible to the admin and meaningless once the principal
   * is gone) without touching admin-visible state such as ACEs or role assignments.
   */
  public void deleteAccessTokens(T ctx, UUID userId) {
    accessTokenRepository.deleteByUserId(ctx, userId);
  }

  /**
   * Deletes the internal-backend credential and the {@code INTERNAL} password identity that links
   * it to a principal. Use when an internal-backend user is being removed (with or without the
   * principal itself). Identities the principal holds on other backends are left intact.
   */
  public void deleteInternalCredentialAndIdentity(T ctx, String backendUsername, UUID userId) {
    internalCredentialRepository.deleteByUsername(ctx, backendUsername);
    passwordIdentityRepository.deleteByUserIdAndBackend(ctx, userId, PasswordBackendType.INTERNAL);
  }

  /** Deletes only the principal (AuthUser) row, leaving everything else untouched. */
  public void deletePrincipalRow(T ctx, UUID userId) {
    authUserRepository.deleteById(ctx, userId);
  }
}
