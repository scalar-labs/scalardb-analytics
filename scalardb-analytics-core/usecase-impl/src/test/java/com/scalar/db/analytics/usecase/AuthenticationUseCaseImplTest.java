/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.usecase.auth.AuthenticationUseCaseImpl;
import com.scalar.db.analytics.usecase.auth.BackendAuthResult;
import com.scalar.db.analytics.usecase.auth.JitProvisioningBackend;
import com.scalar.db.analytics.usecase.auth.PasswordAuthenticationBackend;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthenticationUseCaseImplTest {

  @Mock private AuthUserRepository<RepositoryTransactionContext> authUserRepository;
  @Mock private PasswordIdentityRepository<RepositoryTransactionContext> passwordIdentityRepository;
  @Mock private AccessTokenRepository<RepositoryTransactionContext> accessTokenRepository;

  @Mock private RoleAssignmentRepository<RepositoryTransactionContext> roleAssignmentRepository;

  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> txManager;
  @Mock private PasswordAuthenticationBackend passwordBackend;

  private AuthenticationUseCaseImpl<RepositoryTransactionContext> useCase;

  @BeforeEach
  void setUp() {
    useCase =
        new AuthenticationUseCaseImpl<>(
            authUserRepository,
            passwordIdentityRepository,
            accessTokenRepository,
            roleAssignmentRepository,
            txManager,
            passwordBackend,
            Duration.ofHours(24),
            null);
  }

  @SuppressWarnings("unchecked")
  @Test
  void authenticateWithPassword_shouldReturnAccessTokenOnSuccess() throws Exception {
    // Arrange
    String username = "alice";
    String password = "secret123";
    UUID userId = UUID.randomUUID();
    UUID identityId = UUID.randomUUID();

    PasswordIdentity identity =
        new PasswordIdentity(identityId, userId, PasswordBackendType.INTERNAL, username);
    AuthUser user = new AuthUser(userId, "alice");

    // Mock backend to return username as backendUserId
    when(passwordBackend.verifyCredential(username, password))
        .thenReturn(BackendAuthResult.of(username));

    when(txManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              var function =
                  (ThrowableFunction<RepositoryTransactionContext, AccessToken, Throwable>)
                      invocation.getArgument(0);
              RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
              return function.apply(ctx);
            });

    when(passwordIdentityRepository.findByBackendAndBackendUserId(any(), any(), eq(username)))
        .thenReturn(Optional.of(identity));
    when(authUserRepository.findById(any(), eq(userId))).thenReturn(Optional.of(user));

    PasswordCredential credential = new PasswordCredential(username, password);

    // Act
    AccessToken result = useCase.authenticateWithPassword(credential);

    // Assert
    assertThat(result.getToken()).isNotBlank();
    assertThat(result.getExpiresAt()).isNotNull();
    assertThat(result.getExpiresAt().getNano() % 1_000_000).isZero();
    assertThat(result.getUserId()).isEqualTo(userId.toString());

    ArgumentCaptor<IssuedToken> tokenCaptor = ArgumentCaptor.forClass(IssuedToken.class);
    verify(accessTokenRepository).create(any(), tokenCaptor.capture());
    assertThat(tokenCaptor.getValue().userId()).isEqualTo(userId);
  }

  @Test
  void authenticateWithPassword_shouldThrowExceptionOnInvalidCredentials() throws Exception {
    // Arrange
    String username = "unknown";
    String password = "secret123";

    // Mock backend to throw AnalyticsException with INVALID_CREDENTIALS
    when(passwordBackend.verifyCredential(username, password))
        .thenThrow(new AnalyticsException(AnalyticsErrorCode.AUTHENTICATION_FAILED));

    PasswordCredential credential = new PasswordCredential(username, password);

    // Act & Assert
    assertThatThrownBy(() -> useCase.authenticateWithPassword(credential))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.AUTHENTICATION_FAILED));
  }

  @SuppressWarnings("unchecked")
  @Test
  void authenticateWithPassword_shouldThrowExceptionOnMissingUserIdentity() throws Exception {
    // Arrange
    String username = "alice";
    String password = "secret123";

    // Mock backend to return username (credentials valid)
    when(passwordBackend.verifyCredential(username, password))
        .thenReturn(BackendAuthResult.of(username));

    when(txManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              var function =
                  (ThrowableFunction<RepositoryTransactionContext, AccessToken, Throwable>)
                      invocation.getArgument(0);
              RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
              return function.apply(ctx);
            });

    // Mock missing user identity
    when(passwordIdentityRepository.findByBackendAndBackendUserId(any(), any(), eq(username)))
        .thenReturn(Optional.empty());

    PasswordCredential credential = new PasswordCredential(username, password);

    // Act & Assert
    assertThatThrownBy(() -> useCase.authenticateWithPassword(credential))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.AUTHENTICATION_FAILED));
  }

  @SuppressWarnings("unchecked")
  @Test
  void authenticateWithPassword_shouldThrowOnOrphanedIdentityWithoutDeleting() throws Exception {
    // Arrange
    String username = "alice";
    String password = "secret123";
    UUID userId = UUID.randomUUID();
    UUID identityId = UUID.randomUUID();

    PasswordIdentity orphanedIdentity =
        new PasswordIdentity(identityId, userId, PasswordBackendType.INTERNAL, username);

    when(passwordBackend.verifyCredential(username, password))
        .thenReturn(BackendAuthResult.of(username));

    when(txManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              var function =
                  (ThrowableFunction<RepositoryTransactionContext, AccessToken, Throwable>)
                      invocation.getArgument(0);
              RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
              return function.apply(ctx);
            });

    // Identity exists but AuthUser has been deleted
    when(passwordIdentityRepository.findByBackendAndBackendUserId(any(), any(), eq(username)))
        .thenReturn(Optional.of(orphanedIdentity));
    when(authUserRepository.findById(any(), eq(userId))).thenReturn(Optional.empty());

    PasswordCredential credential = new PasswordCredential(username, password);

    // Act & Assert
    assertThatThrownBy(() -> useCase.authenticateWithPassword(credential))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            ex ->
                assertThat(((AnalyticsException) ex).getErrorCode())
                    .isEqualTo(AnalyticsErrorCode.AUTHENTICATION_FAILED));

    // Verify orphaned identity was NOT deleted (preserved for operator investigation)
    verify(passwordIdentityRepository, never()).deleteByUserIdAndBackend(any(), any(), any());
  }

  @Nested
  @ExtendWith(MockitoExtension.class)
  class JitProvisioningTest {

    @Mock private AuthUserRepository<RepositoryTransactionContext> authUserRepository;

    @Mock
    private PasswordIdentityRepository<RepositoryTransactionContext> passwordIdentityRepository;

    @Mock private AccessTokenRepository<RepositoryTransactionContext> accessTokenRepository;

    @Mock private RoleAssignmentRepository<RepositoryTransactionContext> roleAssignmentRepository;

    @Mock private RepositoryTransactionManager<RepositoryTransactionContext> txManager;
    @Mock private JitProvisioningBackend jitBackend;

    private AuthenticationUseCaseImpl<RepositoryTransactionContext> jitUseCase;

    @BeforeEach
    void setUp() {
      jitUseCase =
          new AuthenticationUseCaseImpl<>(
              authUserRepository,
              passwordIdentityRepository,
              accessTokenRepository,
              roleAssignmentRepository,
              txManager,
              jitBackend,
              Duration.ofHours(24),
              null);
    }

    @SuppressWarnings("unchecked")
    @Test
    void authenticateWithPassword_shouldProvisionUserOnFirstLogin() throws Exception {
      String username = "alice";
      String password = "secret123";

      when(jitBackend.verifyCredential(username, password))
          .thenReturn(BackendAuthResult.of(username));
      when(jitBackend.getBackendType()).thenReturn(PasswordBackendType.SCALARDB_CLUSTER);

      when(txManager.withTransaction(any()))
          .thenAnswer(
              invocation -> {
                var function =
                    (ThrowableFunction<RepositoryTransactionContext, AccessToken, Throwable>)
                        invocation.getArgument(0);
                RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
                return function.apply(ctx);
              });

      when(passwordIdentityRepository.findByBackendAndBackendUserId(any(), any(), eq(username)))
          .thenReturn(Optional.empty());

      PasswordCredential credential = new PasswordCredential(username, password);

      AccessToken result = jitUseCase.authenticateWithPassword(credential);

      assertThat(result.getToken()).isNotBlank();
      assertThat(result.getExpiresAt()).isNotNull();

      // Verify user was created
      ArgumentCaptor<AuthUser> userCaptor = ArgumentCaptor.forClass(AuthUser.class);
      verify(authUserRepository).create(any(), userCaptor.capture());
      assertThat(userCaptor.getValue().getUserId()).isNotNull();

      // Verify identity was created with correct backend type
      ArgumentCaptor<PasswordIdentity> identityCaptor =
          ArgumentCaptor.forClass(PasswordIdentity.class);
      verify(passwordIdentityRepository).create(any(), identityCaptor.capture());
      PasswordIdentity createdIdentity = identityCaptor.getValue();
      assertThat(createdIdentity.backend()).isEqualTo(PasswordBackendType.SCALARDB_CLUSTER);
      assertThat(createdIdentity.backendUserId()).isEqualTo(username);
      assertThat(createdIdentity.userId()).isEqualTo(userCaptor.getValue().getUserId());

      // Verify token was created
      verify(accessTokenRepository).create(any(), any(IssuedToken.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void authenticateWithPassword_shouldRetryTransactionOnConcurrentJitProvisioning()
        throws Exception {
      String username = "alice";
      String password = "secret123";
      UUID userId = UUID.randomUUID();
      UUID identityId = UUID.randomUUID();

      PasswordIdentity existingIdentity =
          new PasswordIdentity(identityId, userId, PasswordBackendType.SCALARDB_CLUSTER, username);
      AuthUser user = new AuthUser(userId, "alice");

      when(jitBackend.verifyCredential(username, password))
          .thenReturn(BackendAuthResult.of(username));
      lenient().when(jitBackend.getBackendType()).thenReturn(PasswordBackendType.SCALARDB_CLUSTER);

      // First TX call: JIT provisioning fails with AnalyticsException (simulating constraint
      // violation causing TX rollback). Second TX call: existing identity found, token issued.
      when(txManager.withTransaction(any()))
          .thenThrow(new AnalyticsException(AnalyticsErrorCode.USER_ALREADY_EXISTS))
          .thenAnswer(
              invocation -> {
                var function =
                    (ThrowableFunction<RepositoryTransactionContext, AccessToken, Throwable>)
                        invocation.getArgument(0);
                RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
                return function.apply(ctx);
              });

      // On retry TX, identity exists
      when(passwordIdentityRepository.findByBackendAndBackendUserId(any(), any(), eq(username)))
          .thenReturn(Optional.of(existingIdentity));
      when(authUserRepository.findById(any(), eq(userId))).thenReturn(Optional.of(user));

      PasswordCredential credential = new PasswordCredential(username, password);

      AccessToken result = jitUseCase.authenticateWithPassword(credential);

      assertThat(result.getToken()).isNotBlank();
      assertThat(result.getUserId()).isEqualTo(userId.toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    void authenticateWithPassword_shouldReProvisionOnOrphanedIdentity() throws Exception {
      String username = "alice";
      String password = "secret123";
      UUID orphanedUserId = UUID.randomUUID();
      UUID orphanedIdentityId = UUID.randomUUID();

      PasswordIdentity orphanedIdentity =
          new PasswordIdentity(
              orphanedIdentityId, orphanedUserId, PasswordBackendType.SCALARDB_CLUSTER, username);

      when(jitBackend.verifyCredential(username, password))
          .thenReturn(BackendAuthResult.of(username));
      when(jitBackend.getBackendType()).thenReturn(PasswordBackendType.SCALARDB_CLUSTER);

      when(txManager.withTransaction(any()))
          .thenAnswer(
              invocation -> {
                var function =
                    (ThrowableFunction<RepositoryTransactionContext, AccessToken, Throwable>)
                        invocation.getArgument(0);
                RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
                return function.apply(ctx);
              });

      // Identity exists but AuthUser has been deleted
      when(passwordIdentityRepository.findByBackendAndBackendUserId(any(), any(), eq(username)))
          .thenReturn(Optional.of(orphanedIdentity));
      when(authUserRepository.findById(any(), eq(orphanedUserId))).thenReturn(Optional.empty());

      PasswordCredential credential = new PasswordCredential(username, password);

      AccessToken result = jitUseCase.authenticateWithPassword(credential);

      assertThat(result.getToken()).isNotBlank();

      // Verify the orphaned identity on the current backend was deleted (scoped, so identities
      // on other backends would be left intact).
      verify(passwordIdentityRepository)
          .deleteByUserIdAndBackend(
              any(), eq(orphanedUserId), eq(PasswordBackendType.SCALARDB_CLUSTER));

      // Verify re-provisioning occurred
      verify(authUserRepository).create(any(), any(AuthUser.class));
      verify(passwordIdentityRepository).create(any(), any(PasswordIdentity.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void authenticateWithPassword_shouldNotRetryOnNonConflictRepositoryException()
        throws Exception {
      String username = "alice";
      String password = "secret123";

      when(jitBackend.verifyCredential(username, password))
          .thenReturn(BackendAuthResult.of(username));

      // Transaction fails with a non-conflict error (e.g., CONNECTION_ERROR)
      when(txManager.withTransaction(any()))
          .thenThrow(new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_CONNECTION_FAILED));

      PasswordCredential credential = new PasswordCredential(username, password);

      assertThatThrownBy(() -> jitUseCase.authenticateWithPassword(credential))
          .isInstanceOf(AnalyticsException.class)
          .satisfies(
              ex ->
                  assertThat(((AnalyticsException) ex).getErrorCode())
                      .isEqualTo(AnalyticsErrorCode.ANALYTICS_DB_CONNECTION_FAILED));

      // Verify transaction was only attempted once (no retry)
      verify(txManager).withTransaction(any());
    }

    @SuppressWarnings("unchecked")
    @Test
    void authenticateWithPassword_shouldUseExistingIdentityOnSubsequentLogin() throws Exception {
      String username = "alice";
      String password = "secret123";
      UUID userId = UUID.randomUUID();
      UUID identityId = UUID.randomUUID();

      PasswordIdentity identity =
          new PasswordIdentity(identityId, userId, PasswordBackendType.SCALARDB_CLUSTER, username);
      AuthUser user = new AuthUser(userId, "alice");

      when(jitBackend.verifyCredential(username, password))
          .thenReturn(BackendAuthResult.of(username));

      when(txManager.withTransaction(any()))
          .thenAnswer(
              invocation -> {
                var function =
                    (ThrowableFunction<RepositoryTransactionContext, AccessToken, Throwable>)
                        invocation.getArgument(0);
                RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
                return function.apply(ctx);
              });

      when(passwordIdentityRepository.findByBackendAndBackendUserId(any(), any(), eq(username)))
          .thenReturn(Optional.of(identity));
      when(authUserRepository.findById(any(), eq(userId))).thenReturn(Optional.of(user));

      PasswordCredential credential = new PasswordCredential(username, password);

      AccessToken result = jitUseCase.authenticateWithPassword(credential);

      assertThat(result.getToken()).isNotBlank();
      assertThat(result.getUserId()).isEqualTo(userId.toString());
    }

    @SuppressWarnings("unchecked")
    @Test
    void authenticateWithPassword_shouldAssignSuperadminOnJitProvisioningWhenInitialAdmin()
        throws Exception {
      String username = "admin";
      String password = "secret123";

      // Create use case with initialAdminUsername configured
      jitUseCase =
          new AuthenticationUseCaseImpl<>(
              authUserRepository,
              passwordIdentityRepository,
              accessTokenRepository,
              roleAssignmentRepository,
              txManager,
              jitBackend,
              Duration.ofHours(24),
              "admin");

      when(jitBackend.verifyCredential(username, password))
          .thenReturn(BackendAuthResult.of(username));
      when(jitBackend.getBackendType()).thenReturn(PasswordBackendType.SCALARDB_CLUSTER);

      when(txManager.withTransaction(any()))
          .thenAnswer(
              invocation -> {
                var function =
                    (ThrowableFunction<RepositoryTransactionContext, AccessToken, Throwable>)
                        invocation.getArgument(0);
                RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
                return function.apply(ctx);
              });

      when(passwordIdentityRepository.findByBackendAndBackendUserId(any(), any(), eq(username)))
          .thenReturn(Optional.empty());
      when(roleAssignmentRepository.findByRoleId(any(), eq(BuiltInRole.SUPERADMIN_ID)))
          .thenReturn(List.of());

      PasswordCredential credential = new PasswordCredential(username, password);

      AccessToken result = jitUseCase.authenticateWithPassword(credential);

      assertThat(result.getToken()).isNotBlank();

      // Verify SUPERADMIN role assignment was created
      ArgumentCaptor<RoleAssignment> assignmentCaptor =
          ArgumentCaptor.forClass(RoleAssignment.class);
      verify(roleAssignmentRepository).create(any(), assignmentCaptor.capture());
      assertThat(assignmentCaptor.getValue().roleId()).isEqualTo(BuiltInRole.SUPERADMIN_ID);
    }

    @SuppressWarnings("unchecked")
    @Test
    void authenticateWithPassword_shouldNotAssignSuperadminWhenNotInitialAdmin() throws Exception {
      String username = "alice";
      String password = "secret123";

      jitUseCase =
          new AuthenticationUseCaseImpl<>(
              authUserRepository,
              passwordIdentityRepository,
              accessTokenRepository,
              roleAssignmentRepository,
              txManager,
              jitBackend,
              Duration.ofHours(24),
              "admin");

      when(jitBackend.verifyCredential(username, password))
          .thenReturn(BackendAuthResult.of(username));
      when(jitBackend.getBackendType()).thenReturn(PasswordBackendType.SCALARDB_CLUSTER);

      when(txManager.withTransaction(any()))
          .thenAnswer(
              invocation -> {
                var function =
                    (ThrowableFunction<RepositoryTransactionContext, AccessToken, Throwable>)
                        invocation.getArgument(0);
                RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
                return function.apply(ctx);
              });

      when(passwordIdentityRepository.findByBackendAndBackendUserId(any(), any(), eq(username)))
          .thenReturn(Optional.empty());

      PasswordCredential credential = new PasswordCredential(username, password);

      jitUseCase.authenticateWithPassword(credential);

      // Verify no SUPERADMIN assignment
      verify(roleAssignmentRepository, never()).create(any(), any(RoleAssignment.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void authenticateWithPassword_shouldNotAssignSuperadminWhenAlreadyAssigned() throws Exception {
      String username = "admin";
      String password = "secret123";

      jitUseCase =
          new AuthenticationUseCaseImpl<>(
              authUserRepository,
              passwordIdentityRepository,
              accessTokenRepository,
              roleAssignmentRepository,
              txManager,
              jitBackend,
              Duration.ofHours(24),
              "admin");

      when(jitBackend.verifyCredential(username, password))
          .thenReturn(BackendAuthResult.of(username));
      when(jitBackend.getBackendType()).thenReturn(PasswordBackendType.SCALARDB_CLUSTER);

      when(txManager.withTransaction(any()))
          .thenAnswer(
              invocation -> {
                var function =
                    (ThrowableFunction<RepositoryTransactionContext, AccessToken, Throwable>)
                        invocation.getArgument(0);
                RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
                return function.apply(ctx);
              });

      when(passwordIdentityRepository.findByBackendAndBackendUserId(any(), any(), eq(username)))
          .thenReturn(Optional.empty());
      when(roleAssignmentRepository.findByRoleId(any(), eq(BuiltInRole.SUPERADMIN_ID)))
          .thenReturn(List.of(new RoleAssignment(UUID.randomUUID(), BuiltInRole.SUPERADMIN_ID)));

      PasswordCredential credential = new PasswordCredential(username, password);

      jitUseCase.authenticateWithPassword(credential);

      // Verify no new SUPERADMIN assignment
      verify(roleAssignmentRepository, never()).create(any(), any(RoleAssignment.class));
    }
  }
}
