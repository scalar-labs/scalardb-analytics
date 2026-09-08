/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.service.authz.NoOpAuthorizationService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth",
    name = "enabled",
    havingValue = "false",
    matchIfMissing = true)
public class NoOpAuthzConfiguration {

  @Bean
  public AuthorizationService authorizationService() {
    return new NoOpAuthorizationService();
  }
}
