/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.domain.auth.internal.InternalCredential;
import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class InternalUserDirectoryUseCaseImplTest {

  private static final UUID ADMIN_ID = UUID.randomUUID();

  @Mock private AuthUserRepository<RepositoryTransactionContext> authUserRepository;
  @Mock private PasswordIdentityRepository<RepositoryTransactionContext> passwordIdentityRepository;

  @Mock
  private InternalCredentialRepository<RepositoryTransactionContext> internalCredentialRepository;

  @Mock private AccessControlEntryRepository<RepositoryTransactionContext> aceRepository;
  @Mock private RoleAssignmentRepository<RepositoryTransactionContext> roleAssignmentRepository;
  @Mock private AccessTokenRepository<RepositoryTransactionContext> accessTokenRepository;
  @Mock private RepositoryTransactionContext txContext;
  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> txManager;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuthorizationService authorizationService;

  private InternalUserDirectoryUseCaseImpl<RepositoryTransactionContext> useCase;

  @BeforeEach
  void setUp() throws Exception {
    useCase =
        new InternalUserDirectoryUseCaseImpl<>(
            authUserRepository,
            passwordIdentityRepository,
            internalCredentialRepository,
            aceRepository,
            roleAssignmentRepository,
            accessTokenRepository,
            txManager,
            passwordEncoder,
            authorizationService);

    lenient().when(authorizationService.authorizeSuperAdmin(any())).thenReturn(true);
    lenient()
        .when(txManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, ?, Exception> function =
                  invocation.getArgument(0);
              return function.apply(txContext);
            });
  }

  @Test
  void deleteInternalBackendUser_cascade_shouldRevokeAccessTokensOfLinkedPrincipal()
      throws Exception {
    UUID principalId = UUID.randomUUID();
    InternalCredential credential = new InternalCredential("alice-internal", "hash");
    PasswordIdentity identity =
        new PasswordIdentity(
            UUID.randomUUID(), principalId, PasswordBackendType.INTERNAL, "alice-internal");

    when(internalCredentialRepository.findByUsername(txContext, "alice-internal"))
        .thenReturn(Optional.of(credential));
    when(passwordIdentityRepository.findByBackendAndBackendUserId(
            txContext, PasswordBackendType.INTERNAL, "alice-internal"))
        .thenReturn(Optional.of(identity));

    boolean deleted = useCase.deleteInternalBackendUser(ADMIN_ID, "alice-internal", true);

    assertThat(deleted).isTrue();
    // Credential + INTERNAL identity are torn down, and the principal's access tokens are revoked
    // on the safe side (symmetric with UserUseCaseImpl.unlinkBackendUser).
    verify(internalCredentialRepository).deleteByUsername(txContext, "alice-internal");
    verify(passwordIdentityRepository)
        .deleteByUserIdAndBackend(txContext, principalId, PasswordBackendType.INTERNAL);
    verify(accessTokenRepository).deleteByUserId(txContext, principalId);
  }

  @Test
  void createInternalBackendUser_shouldCreateCredentialWhenUsernameAvailable() throws Exception {
    when(internalCredentialRepository.findByUsername(txContext, "alice-internal"))
        .thenReturn(Optional.empty());
    when(passwordEncoder.encode("pw")).thenReturn("hash");

    useCase.createInternalBackendUser(ADMIN_ID, "alice-internal", "pw");

    verify(internalCredentialRepository).create(eq(txContext), any(InternalCredential.class));
  }

  @Test
  void createInternalBackendUser_shouldThrowWhenUsernameAlreadyExists() throws Exception {
    when(internalCredentialRepository.findByUsername(txContext, "alice-internal"))
        .thenReturn(Optional.of(new InternalCredential("alice-internal", "hash")));

    assertThatThrownBy(() -> useCase.createInternalBackendUser(ADMIN_ID, "alice-internal", "pw"))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.USER_ALREADY_EXISTS);

    verify(internalCredentialRepository, never()).create(any(), any());
  }

  @Test
  void deleteInternalBackendUser_withoutCascade_shouldDeleteCredentialWhenNotLinked()
      throws Exception {
    when(internalCredentialRepository.findByUsername(txContext, "alice-internal"))
        .thenReturn(Optional.of(new InternalCredential("alice-internal", "hash")));
    when(passwordIdentityRepository.findByBackendAndBackendUserId(
            txContext, PasswordBackendType.INTERNAL, "alice-internal"))
        .thenReturn(Optional.empty());

    boolean deleted = useCase.deleteInternalBackendUser(ADMIN_ID, "alice-internal", false);

    assertThat(deleted).isTrue();
    verify(internalCredentialRepository).deleteByUsername(txContext, "alice-internal");
    verify(passwordIdentityRepository, never()).deleteByUserIdAndBackend(any(), any(), any());
    verify(accessTokenRepository, never()).deleteByUserId(any(), any());
  }

  @Test
  void deleteInternalBackendUser_withoutCascade_shouldBlockWhenLinked() throws Exception {
    when(internalCredentialRepository.findByUsername(txContext, "alice-internal"))
        .thenReturn(Optional.of(new InternalCredential("alice-internal", "hash")));
    when(passwordIdentityRepository.findByBackendAndBackendUserId(
            txContext, PasswordBackendType.INTERNAL, "alice-internal"))
        .thenReturn(
            Optional.of(
                new PasswordIdentity(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    PasswordBackendType.INTERNAL,
                    "alice-internal")));

    assertThatThrownBy(() -> useCase.deleteInternalBackendUser(ADMIN_ID, "alice-internal", false))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.USER_NOT_EMPTY);

    verify(internalCredentialRepository, never()).deleteByUsername(any(), any());
  }

  @Test
  void deleteInternalBackendUser_shouldReturnFalseWhenCredentialMissing() throws Exception {
    when(internalCredentialRepository.findByUsername(txContext, "alice-internal"))
        .thenReturn(Optional.empty());

    assertThat(useCase.deleteInternalBackendUser(ADMIN_ID, "alice-internal", false)).isFalse();

    verify(internalCredentialRepository, never()).deleteByUsername(any(), any());
  }
}
