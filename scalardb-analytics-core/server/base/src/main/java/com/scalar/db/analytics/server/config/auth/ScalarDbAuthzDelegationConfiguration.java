/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import com.scalar.db.analytics.grpc.generated.scalardb.cluster.rpc.v1.DistributedTransactionAdminGrpc;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import com.scalar.db.analytics.server.auth.InMemoryBackendTokenStore;
import com.scalar.db.analytics.server.auth.ScalarDbAuthorizationDelegateImpl;
import com.scalar.db.analytics.server.auth.ScalarDbPrivilegeClientImpl;
import com.scalar.db.analytics.service.authz.ScalarDbAuthorizationDelegate;
import com.scalar.db.analytics.service.authz.ScalarDbPrivilegeClient;
import com.scalar.db.analytics.usecase.auth.BackendTokenStore;
import io.grpc.ManagedChannel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for ScalarDB authorization delegation.
 *
 * <p>Activated when auth is enabled, the ScalarDB Cluster backend is selected, and {@code
 * acl-delegation} is set to {@code true}. Creates the gRPC privilege client and delegate that check
 * ScalarDB privileges using the authenticated user's backend token.
 */
@Configuration
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth",
    name = "enabled",
    havingValue = "true")
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth.password",
    name = "backend",
    havingValue = "scalardb-cluster")
@ConditionalOnProperty(
    prefix = "scalar.db.analytics.server.auth.password.scalardb-cluster",
    name = "acl-delegation",
    havingValue = "true")
public class ScalarDbAuthzDelegationConfiguration {

  @Bean
  public BackendTokenStore backendTokenStore() {
    return new InMemoryBackendTokenStore();
  }

  @Bean
  public ScalarDbPrivilegeClient scalarDbPrivilegeClient(
      ManagedChannel scalarDbClusterChannel, ScalarDbClusterProperties clusterProperties) {
    DistributedTransactionAdminGrpc.DistributedTransactionAdminBlockingStub adminStub =
        DistributedTransactionAdminGrpc.newBlockingStub(scalarDbClusterChannel);

    return new ScalarDbPrivilegeClientImpl(adminStub, clusterProperties.getDeadlineMillis());
  }

  @Bean
  public ScalarDbAuthorizationDelegate scalarDbAuthorizationDelegate(
      NamespaceRepository<SpringDataJdbcTransactionContext> namespaceRepository,
      TableRepository<SpringDataJdbcTransactionContext> tableRepository,
      PasswordIdentityRepository<SpringDataJdbcTransactionContext> passwordIdentityRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> txManager,
      ScalarDbPrivilegeClient privilegeClient,
      BackendTokenStore backendTokenStore) {
    return new ScalarDbAuthorizationDelegateImpl<>(
        namespaceRepository,
        tableRepository,
        passwordIdentityRepository,
        txManager,
        privilegeClient,
        backendTokenStore);
  }
}
