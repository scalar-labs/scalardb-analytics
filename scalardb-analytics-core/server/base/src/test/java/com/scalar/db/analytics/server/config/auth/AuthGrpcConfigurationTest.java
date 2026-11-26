/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.scalar.db.analytics.grpc.generated.auth.v1.InternalUserDirectoryServiceGrpc.InternalUserDirectoryServiceImplBase;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.server.auth.ScalarDbClusterPasswordBackend;
import com.scalar.db.analytics.server.config.ServerTlsProperties;
import com.scalar.db.analytics.server.grpc.InternalUserDirectoryServiceImpl;
import com.scalar.db.analytics.server.grpc.UnsupportedInternalUserDirectoryServiceImpl;
import com.scalar.db.analytics.server.grpc.UserServiceImpl;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.usecase.auth.AuthenticationUseCase;
import com.scalar.db.analytics.usecase.auth.PasswordAuthenticationBackend;
import com.scalar.db.analytics.usecase.auth.UserUseCase;
import com.scalar.db.analytics.usecase.auth.internal.InternalPasswordBackend;
import com.scalar.db.analytics.usecase.authz.PermissionUseCase;
import com.scalar.db.analytics.usecase.authz.RoleUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Verifies that the auth configuration wires up for each password backend. Repositories and use
 * cases from other configurations are replaced with mocks so that no database or ScalarDB Cluster
 * is needed; the ScalarDB Cluster channel connects lazily, so it is never contacted.
 */
class AuthGrpcConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withUserConfiguration(
              AuthGrpcConfiguration.class,
              InternalBackendConfiguration.class,
              ScalarDbClusterBackendConfiguration.class)
          .withPropertyValues("scalar.db.analytics.server.auth.enabled=true")
          .withBean(
              ServerTlsProperties.class,
              () -> new ServerTlsProperties(true, "/path/to/cert.pem", "/path/to/key.pem"))
          .withBean(AuthenticationUseCase.class, () -> mock(AuthenticationUseCase.class))
          .withBean(RoleUseCase.class, () -> mock(RoleUseCase.class))
          .withBean(PermissionUseCase.class, () -> mock(PermissionUseCase.class))
          .withBean(AuthorizationService.class, () -> mock(AuthorizationService.class))
          .withBean(PasswordIdentityRepository.class, () -> mock(PasswordIdentityRepository.class))
          .withBean(AuthUserRepository.class, () -> mock(AuthUserRepository.class))
          .withBean(RoleAssignmentRepository.class, () -> mock(RoleAssignmentRepository.class))
          .withBean(RoleRepository.class, () -> mock(RoleRepository.class))
          .withBean(
              InternalCredentialRepository.class, () -> mock(InternalCredentialRepository.class))
          .withBean(
              AccessControlEntryRepository.class, () -> mock(AccessControlEntryRepository.class))
          .withBean(AccessTokenRepository.class, () -> mock(AccessTokenRepository.class))
          .withBean(
              RepositoryTransactionManager.class, () -> mock(RepositoryTransactionManager.class));

  @Test
  void givenScalarDbClusterBackend_shouldStartWithoutPasswordEncoder() {
    contextRunner
        .withPropertyValues(
            "scalar.db.analytics.server.auth.password.backend=scalardb-cluster",
            "scalar.db.analytics.server.auth.password.scalardb-cluster.host=localhost",
            "scalar.db.analytics.server.auth.password.scalardb-cluster.port=60053")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).doesNotHaveBean(PasswordEncoder.class);
              assertThat(context).hasSingleBean(UserUseCase.class);
              assertThat(context).hasSingleBean(UserServiceImpl.class);
              assertThat(context.getBean(PasswordAuthenticationBackend.class))
                  .isInstanceOf(ScalarDbClusterPasswordBackend.class);
              // The internal user directory service is still registered, so that its calls fail
              // with a descriptive error rather than UNIMPLEMENTED.
              assertThat(context.getBean(InternalUserDirectoryServiceImplBase.class))
                  .isInstanceOf(UnsupportedInternalUserDirectoryServiceImpl.class);
            });
  }

  @Test
  void givenInternalBackend_shouldStartWithPasswordEncoder() {
    contextRunner
        .withPropertyValues("scalar.db.analytics.server.auth.password.backend=internal")
        .run(
            context -> {
              assertThat(context).hasNotFailed();
              assertThat(context).hasSingleBean(PasswordEncoder.class);
              assertThat(context).hasSingleBean(UserUseCase.class);
              assertThat(context).hasSingleBean(UserServiceImpl.class);
              assertThat(context.getBean(PasswordAuthenticationBackend.class))
                  .isInstanceOf(InternalPasswordBackend.class);
              assertThat(context.getBean(InternalUserDirectoryServiceImplBase.class))
                  .isInstanceOf(InternalUserDirectoryServiceImpl.class);
            });
  }

  @Test
  void givenNoBackendConfigured_shouldDefaultToInternalBackend() {
    contextRunner.run(
        context -> {
          assertThat(context).hasNotFailed();
          assertThat(context).hasSingleBean(PasswordEncoder.class);
          assertThat(context.getBean(PasswordAuthenticationBackend.class))
              .isInstanceOf(InternalPasswordBackend.class);
        });
  }
}
