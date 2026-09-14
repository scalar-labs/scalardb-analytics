/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.domain.authz.BuiltInRole;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.repository.impl.spring.repository.authz.AuthzMasterDataSeeder;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import com.scalar.db.analytics.server.config.auth.AuthServerProperties.PasswordAuthProperties;
import com.scalar.db.analytics.server.config.auth.AuthServerProperties.PasswordAuthProperties.InternalProperties;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class SuperAdminBootstrapRunnerTest {

  @Mock private AuthzMasterDataSeeder masterDataSeeder;
  @Mock private RoleRepository<SpringDataJdbcTransactionContext> roleRepository;

  @Mock private RoleAssignmentRepository<SpringDataJdbcTransactionContext> roleAssignmentRepository;

  @Mock private AuthUserRepository<SpringDataJdbcTransactionContext> authUserRepository;

  @Mock
  private PasswordIdentityRepository<SpringDataJdbcTransactionContext> passwordIdentityRepository;

  @Mock
  private InternalCredentialRepository<SpringDataJdbcTransactionContext>
      internalCredentialRepository;

  @Mock private RepositoryTransactionManager<SpringDataJdbcTransactionContext> txManager;
  @Mock private PasswordEncoder passwordEncoder;

  private SuperAdminBootstrapRunner runner;

  @BeforeEach
  void setUp() {
    AuthServerProperties properties = new AuthServerProperties(true, null, null);
    runner = newRunner(properties, Optional.empty(), Optional.empty());
  }

  private SuperAdminBootstrapRunner newRunner(
      AuthServerProperties properties,
      Optional<InternalCredentialRepository<SpringDataJdbcTransactionContext>> internalCredRepo,
      Optional<PasswordEncoder> pwEncoder) {
    return new SuperAdminBootstrapRunner(
        masterDataSeeder,
        roleRepository,
        roleAssignmentRepository,
        authUserRepository,
        passwordIdentityRepository,
        internalCredRepo,
        txManager,
        properties,
        pwEncoder);
  }

  @SuppressWarnings("unchecked")
  private void stubTransactionPassthrough() throws Exception {
    when(txManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              var function =
                  (ThrowableFunction<SpringDataJdbcTransactionContext, Object, Throwable>)
                      invocation.getArgument(0);
              SpringDataJdbcTransactionContext ctx = mock(SpringDataJdbcTransactionContext.class);
              return function.apply(ctx);
            });
  }

  @Test
  void run_shouldSeedResourceTypesAndPermissions() throws Exception {
    stubTransactionPassthrough();
    when(roleRepository.findByName(any(), any())).thenReturn(Optional.of(BuiltInRole.superadmin()));

    runner.run(new DefaultApplicationArguments());

    verify(masterDataSeeder).seedResourceTypes();
    verify(masterDataSeeder).seedPermissions();
  }

  @Test
  void seedRole_shouldCreateSuperadminWhenNotExists() throws Exception {
    stubTransactionPassthrough();
    when(roleRepository.findByName(any(), any())).thenReturn(Optional.empty());

    runner.run(new DefaultApplicationArguments());

    verify(roleRepository).create(any(), any(Role.class));
  }

  @Test
  void seedRole_shouldSkipWhenSuperadminExists() throws Exception {
    stubTransactionPassthrough();
    when(roleRepository.findByName(any(), any())).thenReturn(Optional.of(BuiltInRole.superadmin()));

    runner.run(new DefaultApplicationArguments());

    verify(roleRepository, never()).create(any(), any(Role.class));
  }

  @Test
  void assignAdmin_shouldAssignWhenUserExistsAndNotAssigned() throws Exception {
    stubTransactionPassthrough();
    runner =
        newRunner(
            new AuthServerProperties(true, null, "admin"), Optional.empty(), Optional.empty());

    when(roleRepository.findByName(any(), any())).thenReturn(Optional.of(BuiltInRole.superadmin()));

    UUID userId = UUID.randomUUID();
    when(roleAssignmentRepository.findByRoleId(any(), any())).thenReturn(List.of());
    when(passwordIdentityRepository.findByBackendAndBackendUserId(
            any(), eq(PasswordBackendType.INTERNAL), any()))
        .thenReturn(
            Optional.of(
                new PasswordIdentity(
                    UUID.randomUUID(), userId, PasswordBackendType.INTERNAL, "admin")));

    runner.run(new DefaultApplicationArguments());

    verify(roleAssignmentRepository).create(any(), any(RoleAssignment.class));
  }

  @Test
  void assignAdmin_shouldSkipWhenUsernameNotConfigured() throws Exception {
    stubTransactionPassthrough();
    when(roleRepository.findByName(any(), any())).thenReturn(Optional.of(BuiltInRole.superadmin()));

    runner.run(new DefaultApplicationArguments());

    verifyNoInteractions(passwordIdentityRepository);
    verify(roleAssignmentRepository, never()).create(any(), any(RoleAssignment.class));
  }

  @Test
  void assignAdmin_shouldSkipWhenSuperadminAlreadyHasAssignees() throws Exception {
    stubTransactionPassthrough();
    runner =
        newRunner(
            new AuthServerProperties(true, null, "admin"), Optional.empty(), Optional.empty());

    when(roleRepository.findByName(any(), any())).thenReturn(Optional.of(BuiltInRole.superadmin()));
    when(roleAssignmentRepository.findByRoleId(any(), any()))
        .thenReturn(List.of(new RoleAssignment(UUID.randomUUID(), BuiltInRole.SUPERADMIN_ID)));

    runner.run(new DefaultApplicationArguments());

    verify(passwordIdentityRepository, never()).findByBackendAndBackendUserId(any(), any(), any());
    verify(roleAssignmentRepository, never()).create(any(), any(RoleAssignment.class));
  }

  @Test
  void assignAdmin_shouldCreateUserAndAssignWhenInternalBackendAndPasswordConfigured()
      throws Exception {
    stubTransactionPassthrough();
    PasswordAuthProperties passwordProps =
        new PasswordAuthProperties(null, null, new InternalProperties("s3cret"));
    runner =
        newRunner(
            new AuthServerProperties(true, passwordProps, "admin"),
            Optional.of(internalCredentialRepository),
            Optional.of(passwordEncoder));

    when(roleRepository.findByName(any(), any())).thenReturn(Optional.of(BuiltInRole.superadmin()));
    when(roleAssignmentRepository.findByRoleId(any(), any())).thenReturn(List.of());
    when(passwordIdentityRepository.findByBackendAndBackendUserId(
            any(), eq(PasswordBackendType.INTERNAL), any()))
        .thenReturn(Optional.empty());
    when(passwordEncoder.encode("s3cret")).thenReturn("hashed_s3cret");

    runner.run(new DefaultApplicationArguments());

    verify(authUserRepository).create(any(), any());
    verify(passwordIdentityRepository).create(any(), any());
    verify(internalCredentialRepository).create(any(), any());
    verify(roleAssignmentRepository).create(any(), any(RoleAssignment.class));
  }

  @Test
  void assignAdmin_shouldSkipWhenInternalBackendAndPasswordNotConfigured() throws Exception {
    stubTransactionPassthrough();
    runner =
        newRunner(
            new AuthServerProperties(true, null, "admin"),
            Optional.of(internalCredentialRepository),
            Optional.of(passwordEncoder));

    when(roleRepository.findByName(any(), any())).thenReturn(Optional.of(BuiltInRole.superadmin()));
    when(roleAssignmentRepository.findByRoleId(any(), any())).thenReturn(List.of());
    when(passwordIdentityRepository.findByBackendAndBackendUserId(
            any(), eq(PasswordBackendType.INTERNAL), any()))
        .thenReturn(Optional.empty());

    runner.run(new DefaultApplicationArguments());

    verify(authUserRepository, never()).create(any(), any());
    verify(roleAssignmentRepository, never()).create(any(), any(RoleAssignment.class));
  }

  @Test
  void assignAdmin_shouldLogInfoWhenExternalBackendAndUserNotFound() throws Exception {
    stubTransactionPassthrough();
    runner =
        newRunner(
            new AuthServerProperties(true, null, "admin"), Optional.empty(), Optional.empty());

    when(roleRepository.findByName(any(), any())).thenReturn(Optional.of(BuiltInRole.superadmin()));
    when(roleAssignmentRepository.findByRoleId(any(), any())).thenReturn(List.of());
    when(passwordIdentityRepository.findByBackendAndBackendUserId(
            any(), eq(PasswordBackendType.INTERNAL), any()))
        .thenReturn(Optional.empty());

    runner.run(new DefaultApplicationArguments());

    // No exception thrown, no assignment created — just logs info
    verify(roleAssignmentRepository, never()).create(any(), any(RoleAssignment.class));
  }

  @Test
  void run_shouldBeIdempotentWhenAllDataExists() throws Exception {
    stubTransactionPassthrough();
    runner =
        newRunner(
            new AuthServerProperties(true, null, "admin"), Optional.empty(), Optional.empty());

    when(roleRepository.findByName(any(), any())).thenReturn(Optional.of(BuiltInRole.superadmin()));
    when(roleAssignmentRepository.findByRoleId(any(), any()))
        .thenReturn(List.of(new RoleAssignment(UUID.randomUUID(), BuiltInRole.SUPERADMIN_ID)));

    runner.run(new DefaultApplicationArguments());

    verify(roleRepository, never()).create(any(), any(Role.class));
    verify(roleAssignmentRepository, never()).create(any(), any(RoleAssignment.class));
  }
}
