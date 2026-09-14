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
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import com.scalar.db.analytics.server.grpc.InternalUserDirectoryServiceImpl;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.usecase.auth.PasswordAuthenticationBackend;
import com.scalar.db.analytics.usecase.auth.internal.InternalPasswordBackend;
import com.scalar.db.analytics.usecase.auth.internal.InternalUserDirectoryUseCase;
import com.scalar.db.analytics.usecase.auth.internal.InternalUserDirectoryUseCaseImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth",
    name = "enabled",
    havingValue = "true")
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth.password",
    name = "backend",
    havingValue = "internal",
    matchIfMissing = true)
public class InternalBackendConfiguration {

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public PasswordAuthenticationBackend passwordAuthenticationBackend(
      InternalCredentialRepository<SpringDataJdbcTransactionContext> internalCredentialRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> repositoryTransactionManager,
      PasswordEncoder passwordEncoder) {
    return new InternalPasswordBackend<>(
        internalCredentialRepository, repositoryTransactionManager, passwordEncoder);
  }

  @Bean
  public InternalUserDirectoryUseCase internalUserDirectoryUseCase(
      AuthUserRepository<SpringDataJdbcTransactionContext> authUserRepository,
      PasswordIdentityRepository<SpringDataJdbcTransactionContext> passwordIdentityRepository,
      InternalCredentialRepository<SpringDataJdbcTransactionContext> internalCredentialRepository,
      AccessControlEntryRepository<SpringDataJdbcTransactionContext> aceRepository,
      RoleAssignmentRepository<SpringDataJdbcTransactionContext> roleAssignmentRepository,
      AccessTokenRepository<SpringDataJdbcTransactionContext> accessTokenRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> repositoryTransactionManager,
      PasswordEncoder passwordEncoder,
      AuthorizationService authorizationService) {
    return new InternalUserDirectoryUseCaseImpl<>(
        authUserRepository,
        passwordIdentityRepository,
        internalCredentialRepository,
        aceRepository,
        roleAssignmentRepository,
        accessTokenRepository,
        repositoryTransactionManager,
        passwordEncoder,
        authorizationService);
  }

  @Bean
  public InternalUserDirectoryServiceImpl internalUserDirectoryService(
      InternalUserDirectoryUseCase internalUserDirectoryUseCase) {
    return new InternalUserDirectoryServiceImpl(internalUserDirectoryUseCase);
  }
}
