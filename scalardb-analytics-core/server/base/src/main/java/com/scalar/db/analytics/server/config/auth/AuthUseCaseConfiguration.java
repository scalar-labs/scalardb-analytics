/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import com.scalar.db.analytics.repository.CatalogRepository;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.usecase.auth.AuthenticationUseCase;
import com.scalar.db.analytics.usecase.auth.AuthenticationUseCaseImpl;
import com.scalar.db.analytics.usecase.auth.BackendTokenStore;
import com.scalar.db.analytics.usecase.auth.PasswordAuthenticationBackend;
import com.scalar.db.analytics.usecase.authz.PermissionUseCase;
import com.scalar.db.analytics.usecase.authz.PermissionUseCaseImpl;
import com.scalar.db.analytics.usecase.authz.RoleUseCase;
import com.scalar.db.analytics.usecase.authz.RoleUseCaseImpl;
import java.time.Duration;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth",
    name = "enabled",
    havingValue = "true")
public class AuthUseCaseConfiguration {

  @Bean
  public AuthenticationUseCase authenticationUseCase(
      AuthUserRepository<SpringDataJdbcTransactionContext> authUserRepository,
      PasswordIdentityRepository<SpringDataJdbcTransactionContext> passwordIdentityRepository,
      AccessTokenRepository<SpringDataJdbcTransactionContext> accessTokenRepository,
      RoleAssignmentRepository<SpringDataJdbcTransactionContext> roleAssignmentRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> repositoryTransactionManager,
      PasswordAuthenticationBackend passwordAuthenticationBackend,
      AuthServerProperties authServerProperties,
      Optional<BackendTokenStore> backendTokenStore) {
    Duration tokenTtl = Duration.ofSeconds(authServerProperties.getPassword().getTokenTtlSeconds());
    return new AuthenticationUseCaseImpl<>(
        authUserRepository,
        passwordIdentityRepository,
        accessTokenRepository,
        roleAssignmentRepository,
        repositoryTransactionManager,
        passwordAuthenticationBackend,
        tokenTtl,
        authServerProperties.getInitialAdminUsername(),
        backendTokenStore.orElse(null));
  }

  @Bean
  public RoleUseCase roleUseCase(
      RoleRepository<SpringDataJdbcTransactionContext> roleRepository,
      RoleAssignmentRepository<SpringDataJdbcTransactionContext> roleAssignmentRepository,
      AuthUserRepository<SpringDataJdbcTransactionContext> authUserRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> repositoryTransactionManager,
      AuthorizationService authorizationService) {
    return new RoleUseCaseImpl<>(
        roleRepository,
        roleAssignmentRepository,
        authUserRepository,
        repositoryTransactionManager,
        authorizationService);
  }

  @Bean
  public PermissionUseCase permissionUseCase(
      AccessControlEntryRepository<SpringDataJdbcTransactionContext> aceRepository,
      RoleRepository<SpringDataJdbcTransactionContext> roleRepository,
      RoleAssignmentRepository<SpringDataJdbcTransactionContext> roleAssignmentRepository,
      CatalogRepository<SpringDataJdbcTransactionContext> catalogRepository,
      DataSourceRepository<SpringDataJdbcTransactionContext> dataSourceRepository,
      NamespaceRepository<SpringDataJdbcTransactionContext> namespaceRepository,
      TableRepository<SpringDataJdbcTransactionContext> tableRepository,
      AuthUserRepository<SpringDataJdbcTransactionContext> authUserRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> repositoryTransactionManager,
      AuthorizationService authorizationService) {
    return new PermissionUseCaseImpl<>(
        aceRepository,
        roleRepository,
        roleAssignmentRepository,
        catalogRepository,
        dataSourceRepository,
        namespaceRepository,
        tableRepository,
        authUserRepository,
        repositoryTransactionManager,
        authorizationService);
  }
}
