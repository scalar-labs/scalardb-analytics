/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth.internal;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.internal.InternalCredential;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.usecase.auth.BackendAuthResult;
import com.scalar.db.analytics.usecase.auth.PasswordAuthenticationBackend;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * {@link PasswordAuthenticationBackend} implementation that verifies credentials against the
 * internal user directory.
 *
 * <p>Looks up {@link InternalCredential} by username and verifies the password using a {@link
 * PasswordEncoder}. Returns the username as the external ID on success.
 *
 * @param <T> the type of transaction context
 */
public class InternalPasswordBackend<T extends RepositoryTransactionContext>
    implements PasswordAuthenticationBackend {

  private final InternalCredentialRepository<T> internalCredentialRepository;
  private final RepositoryTransactionManager<T> txManager;
  private final PasswordEncoder passwordEncoder;

  public InternalPasswordBackend(
      InternalCredentialRepository<T> internalCredentialRepository,
      RepositoryTransactionManager<T> txManager,
      PasswordEncoder passwordEncoder) {
    this.internalCredentialRepository = internalCredentialRepository;
    this.txManager = txManager;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public BackendAuthResult verifyCredential(String username, String password) {
    return txManager.withTransaction(
        ctx -> {
          InternalCredential credential =
              internalCredentialRepository
                  .findByUsername(ctx, username)
                  .orElseThrow(
                      () -> new AnalyticsException(AnalyticsErrorCode.AUTHENTICATION_FAILED));

          if (!passwordEncoder.matches(password, credential.passwordHash())) {
            throw new AnalyticsException(AnalyticsErrorCode.AUTHENTICATION_FAILED);
          }

          return BackendAuthResult.of(username);
        });
  }

  @Override
  public PasswordBackendType getBackendType() {
    return PasswordBackendType.INTERNAL;
  }
}
