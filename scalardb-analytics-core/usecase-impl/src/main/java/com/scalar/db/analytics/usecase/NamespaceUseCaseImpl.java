/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.repository.DataSourceNamespaceQueryService;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.service.authz.PermissionRequirement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class NamespaceUseCaseImpl<T extends RepositoryTransactionContext>
    implements NamespaceUseCase {
  private final DataSourceNamespaceQueryService<T> namespaceQueryService;
  private final NamespaceRepository<T> namespaceRepository;
  private final DataSourceRepository<T> dataSourceRepository;
  private final RepositoryTransactionManager<T> txManager;
  private final AuthorizationService authorizationService;

  public NamespaceUseCaseImpl(
      DataSourceNamespaceQueryService<T> namespaceQueryService,
      NamespaceRepository<T> namespaceRepository,
      DataSourceRepository<T> dataSourceRepository,
      RepositoryTransactionManager<T> txManager,
      AuthorizationService authorizationService) {
    this.namespaceQueryService = namespaceQueryService;
    this.namespaceRepository = namespaceRepository;
    this.dataSourceRepository = dataSourceRepository;
    this.txManager = txManager;
    this.authorizationService = authorizationService;
  }

  @Override
  public List<DataSourceNamespace> listNamespaces(UUID userId, String catalogName) {
    List<DataSourceNamespace> namespaces =
        namespaceQueryService.listByCatalogName(txManager.single(), catalogName);
    return authorizationService.filterAuthorized(
        userId, namespaces, NamespaceUseCaseImpl::namespaceReadRequirements);
  }

  @Override
  public Optional<Namespace> describeNamespace(
      UUID userId, String catalogName, String dataSourceName, List<String> namespaceNames) {
    Optional<DataSourceNamespace> dataSourceNamespace =
        namespaceQueryService.findByCatalogAndDataSourceAndNames(
            txManager.single(), catalogName, dataSourceName, namespaceNames);
    if (dataSourceNamespace.isEmpty()) {
      return Optional.empty();
    }
    if (!authorizationService.authorize(
        userId, namespaceReadRequirements(dataSourceNamespace.get()))) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED,
          Map.of(
              "user_id", userId.toString(),
              "catalog_name", catalogName,
              "data_source_name", dataSourceName));
    }
    return dataSourceNamespace.map(DataSourceNamespace::getNamespace);
  }

  @Override
  public Optional<Namespace> describeNamespaceById(UUID userId, UUID namespaceId) {
    Optional<Namespace> namespace = namespaceRepository.findById(txManager.single(), namespaceId);
    if (namespace.isEmpty()) {
      return Optional.empty();
    }
    Optional<DataSource> dataSource =
        dataSourceRepository.findById(txManager.single(), namespace.get().getDataSourceId());
    if (dataSource.isEmpty()) {
      throw new IllegalStateException(
          "Data integrity error: namespace "
              + namespaceId
              + " references non-existent data source "
              + namespace.get().getDataSourceId());
    }
    if (!authorizationService.authorize(
        userId, namespaceReadRequirements(namespace.get(), dataSource.get()))) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED,
          Map.of("user_id", userId.toString(), "namespace_name", namespaceId.toString()));
    }
    return namespace;
  }

  private static List<PermissionRequirement> namespaceReadRequirements(
      DataSourceNamespace dsNamespace) {
    return namespaceReadRequirements(dsNamespace.getNamespace(), dsNamespace.getDataSource());
  }

  private static List<PermissionRequirement> namespaceReadRequirements(
      Namespace namespace, DataSource dataSource) {
    String providerType = dataSource.getProvider().getType();
    return List.of(
        new PermissionRequirement(
            new ResourceRef(ResourceType.NAMESPACE, namespace.getId(), providerType),
            Set.of(Permission.NAMESPACE_READ)),
        new PermissionRequirement(
            new ResourceRef(ResourceType.DATA_SOURCE, dataSource.getId()),
            Set.of(Permission.DATA_SOURCE_READ, Permission.DATA_SOURCE_ADMIN)),
        new PermissionRequirement(
            new ResourceRef(ResourceType.CATALOG, dataSource.getCatalogId()),
            Set.of(Permission.CATALOG_READ, Permission.CATALOG_ADMIN)));
  }
}
