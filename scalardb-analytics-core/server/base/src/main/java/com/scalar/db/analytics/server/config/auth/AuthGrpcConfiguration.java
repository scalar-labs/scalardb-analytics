/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.auth.internal.InternalCredentialRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import com.scalar.db.analytics.server.config.ServerTlsProperties;
import com.scalar.db.analytics.server.grpc.AuthServiceImpl;
import com.scalar.db.analytics.server.grpc.PermissionServiceImpl;
import com.scalar.db.analytics.server.grpc.RoleServiceImpl;
import com.scalar.db.analytics.server.grpc.UserServiceImpl;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.usecase.auth.AuthenticationUseCase;
import com.scalar.db.analytics.usecase.auth.UserUseCase;
import com.scalar.db.analytics.usecase.auth.UserUseCaseImpl;
import com.scalar.db.analytics.usecase.authz.PermissionUseCase;
import com.scalar.db.analytics.usecase.authz.RoleUseCase;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth",
    name = "enabled",
    havingValue = "true")
public class AuthGrpcConfiguration {

  public AuthGrpcConfiguration(ServerTlsProperties serverTlsProperties) {
    if (!serverTlsProperties.isEnabled()) {
      throw new IllegalStateException(
          "Auth services require TLS to be enabled. "
              + "Set scalar.db.analytics.server.tls.enabled=true with valid certificates, "
              + "or disable authentication by removing scalar.db.analytics.server.auth.enabled.");
    }
  }

  @Bean
  public AuthServiceImpl authService(AuthenticationUseCase authenticationUseCase) {
    return new AuthServiceImpl(authenticationUseCase);
  }

  @Bean
  public RoleServiceImpl roleService(RoleUseCase roleUseCase) {
    return new RoleServiceImpl(roleUseCase);
  }

  @Bean
  public PermissionServiceImpl permissionService(PermissionUseCase permissionUseCase) {
    return new PermissionServiceImpl(permissionUseCase);
  }

  @Bean
  public UserUseCase userUseCase(
      PasswordIdentityRepository<SpringDataJdbcTransactionContext> passwordIdentityRepository,
      AuthUserRepository<SpringDataJdbcTransactionContext> authUserRepository,
      RoleAssignmentRepository<SpringDataJdbcTransactionContext> roleAssignmentRepository,
      RoleRepository<SpringDataJdbcTransactionContext> roleRepository,
      InternalCredentialRepository<SpringDataJdbcTransactionContext> internalCredentialRepository,
      AccessControlEntryRepository<SpringDataJdbcTransactionContext> aceRepository,
      AccessTokenRepository<SpringDataJdbcTransactionContext> accessTokenRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> repositoryTransactionManager,
      AuthorizationService authorizationService,
      PasswordEncoder passwordEncoder) {
    return new UserUseCaseImpl<>(
        passwordIdentityRepository,
        authUserRepository,
        roleAssignmentRepository,
        roleRepository,
        internalCredentialRepository,
        aceRepository,
        accessTokenRepository,
        repositoryTransactionManager,
        authorizationService,
        passwordEncoder);
  }

  @Bean
  public UserServiceImpl userService(UserUseCase userUseCase) {
    return new UserServiceImpl(userUseCase);
  }
}
