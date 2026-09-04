/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.service.authz.AuthorizationServiceImpl;
import com.scalar.db.analytics.service.authz.ScalarDbAuthorizationDelegate;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth",
    name = "enabled",
    havingValue = "true")
public class AuthzServiceConfiguration {

  @Bean
  public AuthorizationService authorizationService(
      RoleRepository<SpringDataJdbcTransactionContext> roleRepository,
      RoleAssignmentRepository<SpringDataJdbcTransactionContext> roleAssignmentRepository,
      AccessControlEntryRepository<SpringDataJdbcTransactionContext> aceRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> txManager,
      Optional<ScalarDbAuthorizationDelegate> scalarDbDelegate) {
    return new AuthorizationServiceImpl<>(
        roleRepository,
        roleAssignmentRepository,
        aceRepository,
        txManager,
        scalarDbDelegate.orElse(null));
  }
}
