/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config;

import com.scalar.db.analytics.repository.CatalogRepository;
import com.scalar.db.analytics.repository.DataSourceNamespaceQueryService;
import com.scalar.db.analytics.repository.DataSourceNamespaceTableQueryService;
import com.scalar.db.analytics.repository.DataSourceQueryService;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.usecase.CatalogUseCase;
import com.scalar.db.analytics.usecase.CatalogUseCaseImpl;
import com.scalar.db.analytics.usecase.DataSourceUseCase;
import com.scalar.db.analytics.usecase.DataSourceUseCaseImpl;
import com.scalar.db.analytics.usecase.NamespaceUseCase;
import com.scalar.db.analytics.usecase.NamespaceUseCaseImpl;
import com.scalar.db.analytics.usecase.TableUseCase;
import com.scalar.db.analytics.usecase.TableUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UseCaseConfiguration {

  @Bean
  public CatalogUseCase catalogUseCase(
      CatalogRepository<SpringDataJdbcTransactionContext> catalogRepository,
      DataSourceRepository<SpringDataJdbcTransactionContext> dataSourceRepository,
      DataSourceQueryService<SpringDataJdbcTransactionContext> dataSourceQueryService,
      NamespaceRepository<SpringDataJdbcTransactionContext> namespaceRepository,
      TableRepository<SpringDataJdbcTransactionContext> tableRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> repositoryTransactionManager,
      DataSourceNamespaceQueryService<SpringDataJdbcTransactionContext>
          dataSourceNamespaceQueryService,
      DataSourceNamespaceTableQueryService<SpringDataJdbcTransactionContext>
          dataSourceNamespaceTableQueryService,
      AccessControlEntryRepository<SpringDataJdbcTransactionContext> aceRepository,
      AuthorizationService authorizationService) {
    return new CatalogUseCaseImpl<>(
        catalogRepository,
        dataSourceRepository,
        dataSourceQueryService,
        namespaceRepository,
        tableRepository,
        repositoryTransactionManager,
        dataSourceNamespaceQueryService,
        dataSourceNamespaceTableQueryService,
        aceRepository,
        authorizationService);
  }

  @Bean
  public DataSourceUseCase dataSourceUseCase(
      CatalogRepository<SpringDataJdbcTransactionContext> catalogRepository,
      DataSourceRepository<SpringDataJdbcTransactionContext> dataSourceRepository,
      NamespaceRepository<SpringDataJdbcTransactionContext> namespaceRepository,
      TableRepository<SpringDataJdbcTransactionContext> tableRepository,
      DataSourceQueryService<SpringDataJdbcTransactionContext> dataSourceQueryService,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> repositoryTransactionManager,
      DataSourceNamespaceQueryService<SpringDataJdbcTransactionContext>
          dataSourceNamespaceQueryService,
      DataSourceNamespaceTableQueryService<SpringDataJdbcTransactionContext>
          dataSourceNamespaceTableQueryService,
      AccessControlEntryRepository<SpringDataJdbcTransactionContext> aceRepository,
      AuthorizationService authorizationService) {
    return new DataSourceUseCaseImpl<>(
        catalogRepository,
        dataSourceRepository,
        namespaceRepository,
        tableRepository,
        dataSourceQueryService,
        repositoryTransactionManager,
        dataSourceNamespaceQueryService,
        dataSourceNamespaceTableQueryService,
        aceRepository,
        authorizationService);
  }

  @Bean
  public NamespaceUseCase namespaceUseCase(
      DataSourceNamespaceQueryService<SpringDataJdbcTransactionContext>
          dataSourceNamespaceQueryService,
      NamespaceRepository<SpringDataJdbcTransactionContext> namespaceRepository,
      DataSourceRepository<SpringDataJdbcTransactionContext> dataSourceRepository,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> repositoryTransactionManager,
      AuthorizationService authorizationService) {
    return new NamespaceUseCaseImpl<>(
        dataSourceNamespaceQueryService,
        namespaceRepository,
        dataSourceRepository,
        repositoryTransactionManager,
        authorizationService);
  }

  @Bean
  public TableUseCase tableUseCase(
      DataSourceNamespaceTableQueryService<SpringDataJdbcTransactionContext>
          dataSourceNamespaceTableQueryService,
      RepositoryTransactionManager<SpringDataJdbcTransactionContext> repositoryTransactionManager,
      AuthorizationService authorizationService) {
    return new TableUseCaseImpl<>(
        dataSourceNamespaceTableQueryService, repositoryTransactionManager, authorizationService);
  }
}
