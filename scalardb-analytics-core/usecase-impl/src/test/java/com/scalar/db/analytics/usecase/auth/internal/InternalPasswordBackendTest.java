/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.internal.InternalCredential;
import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.usecase.auth.BackendAuthResult;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InternalPasswordBackendTest {

  @Mock
  private InternalCredentialRepository<RepositoryTransactionContext> internalCredentialRepository;

  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> txManager;
  @Mock private PasswordEncoder passwordEncoder;

  private InternalPasswordBackend<RepositoryTransactionContext> backend;

  @SuppressWarnings("unchecked")
  @BeforeEach
  void setUp() throws Exception {
    backend =
        new InternalPasswordBackend<>(internalCredentialRepository, txManager, passwordEncoder);

    when(txManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              var function =
                  (ThrowableFunction<RepositoryTransactionContext, BackendAuthResult, Throwable>)
                      invocation.getArgument(0);
              RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
              return function.apply(ctx);
            });
  }

  @Test
  void verifyCredential_shouldReturnUsernameOnValidCredentials() throws Exception {
    String username = "alice";
    String password = "secret123";
    String hashedPassword = "$2a$10$hashedPassword";

    when(internalCredentialRepository.findByUsername(any(), eq(username)))
        .thenReturn(Optional.of(new InternalCredential(username, hashedPassword)));
    when(passwordEncoder.matches(password, hashedPassword)).thenReturn(true);

    BackendAuthResult result = backend.verifyCredential(username, password);

    assertThat(result.backendUserId()).isEqualTo(username);
    assertThat(result.backendToken()).isNull();
  }

  @Test
  void verifyCredential_shouldThrowOnUnknownUsername() throws Exception {
    String username = "unknown";
    String password = "secret123";

    when(internalCredentialRepository.findByUsername(any(), eq(username)))
        .thenReturn(Optional.empty());

    assertThatThrownBy(() -> backend.verifyCredential(username, password))
        .isInstanceOf(AnalyticsException.class)
        .hasMessageContaining("Authentication failed");
  }

  @Test
  void verifyCredential_shouldThrowOnWrongPassword() throws Exception {
    String username = "alice";
    String password = "wrongpassword";
    String hashedPassword = "$2a$10$hashedPassword";

    when(internalCredentialRepository.findByUsername(any(), eq(username)))
        .thenReturn(Optional.of(new InternalCredential(username, hashedPassword)));
    when(passwordEncoder.matches(password, hashedPassword)).thenReturn(false);

    assertThatThrownBy(() -> backend.verifyCredential(username, password))
        .isInstanceOf(AnalyticsException.class)
        .hasMessageContaining("Authentication failed");
  }
}
