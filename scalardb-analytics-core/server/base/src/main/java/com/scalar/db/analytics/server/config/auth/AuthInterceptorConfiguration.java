/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AccessTokenRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import com.scalar.db.analytics.server.grpc.AuthenticationInterceptor;
import com.scalar.db.analytics.usecase.auth.TokenValidationUseCase;
import com.scalar.db.analytics.usecase.auth.TokenValidationUseCaseImpl;
import java.time.Clock;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth",
    name = "enabled",
    havingValue = "true")
public class AuthInterceptorConfiguration {

  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  @Bean
  public TokenValidationUseCase tokenValidationUseCase(
      AccessTokenRepository<SpringDataJdbcTransactionContext> accessTokenRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> repositoryTransactionManager,
      Clock clock) {
    return new TokenValidationUseCaseImpl<>(
        accessTokenRepository, repositoryTransactionManager, clock);
  }

  @Bean
  public AuthenticationInterceptor authenticationInterceptor(
      TokenValidationUseCase tokenValidationUseCase) {
    return new AuthenticationInterceptor(tokenValidationUseCase);
  }
}
