/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.repository.CatalogRepository;
import com.scalar.db.analytics.repository.DataSourceNamespaceQueryService;
import com.scalar.db.analytics.repository.DataSourceNamespaceTableQueryService;
import com.scalar.db.analytics.repository.DataSourceQueryService;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.service.DataSourceService;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.service.authz.PermissionRequirement;
import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class DataSourceUseCaseImpl<T extends RepositoryTransactionContext>
    implements DataSourceUseCase {
  private final CatalogRepository<T> catalogRepository;
  private final DataSourceRepository<T> dataSourceRepository;
  private final DataSourceQueryService<T> dataSourceQueryService;
  private final DataSourceNamespaceQueryService<T> dataSourceNamespaceQueryService;
  private final RepositoryTransactionManager<T> txManager;
  private final DataSourceService<T> dataSourceService;
  private final AccessControlEntryRepository<T> aceRepository;
  private final AuthorizationService authorizationService;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public DataSourceUseCaseImpl(
      CatalogRepository<T> catalogRepository,
      DataSourceRepository<T> dataSourceRepository,
      NamespaceRepository<T> namespaceRepository,
      TableRepository<T> tableRepository,
      DataSourceQueryService<T> dataSourceQueryService,
      RepositoryTransactionManager<T> txManager,
      DataSourceNamespaceQueryService<T> dataSourceNamespaceQueryService,
      DataSourceNamespaceTableQueryService<T> dataSourceNamespaceTableQueryService,
      AccessControlEntryRepository<T> aceRepository,
      AuthorizationService authorizationService) {
    this.catalogRepository = catalogRepository;
    this.dataSourceRepository = dataSourceRepository;
    this.dataSourceQueryService = dataSourceQueryService;
    this.dataSourceNamespaceQueryService = dataSourceNamespaceQueryService;
    this.txManager = txManager;
    this.aceRepository = aceRepository;
    this.authorizationService = authorizationService;

    this.dataSourceService =
        new DataSourceService<>(
            catalogRepository,
            dataSourceRepository,
            namespaceRepository,
            tableRepository,
            dataSourceNamespaceQueryService,
            dataSourceNamespaceTableQueryService,
            aceRepository);
  }

  @Override
  public Optional<DataSource> findDataSource(
      UUID userId, String catalogName, String dataSourceName) {
    Optional<DataSource> dataSource =
        dataSourceQueryService.findByName(txManager.single(), catalogName, dataSourceName);
    if (dataSource.isEmpty()) {
      return Optional.empty();
    }
    if (!authorizationService.authorize(userId, dataSourceReadRequirements(dataSource.get()))) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED,
          Map.of(
              "user_id", userId.toString(),
              "catalog_name", catalogName,
              "data_source_name", dataSourceName));
    }
    return dataSource;
  }

  @Override
  public Optional<DataSource> describeDataSourceById(UUID userId, UUID dataSourceId) {
    Optional<DataSource> dataSource =
        dataSourceRepository.findById(txManager.single(), dataSourceId);
    if (dataSource.isEmpty()) {
      return Optional.empty();
    }
    if (!authorizationService.authorize(userId, dataSourceReadRequirements(dataSource.get()))) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED,
          Map.of("user_id", userId.toString(), "data_source_name", dataSourceId.toString()));
    }
    return dataSource;
  }

  @Override
  public List<DataSource> listDataSources(UUID userId, String catalogName) {
    List<DataSource> dataSources =
        dataSourceQueryService.listByCatalogName(txManager.single(), catalogName);
    return authorizationService.filterAuthorized(
        userId, dataSources, DataSourceUseCaseImpl::dataSourceReadRequirements);
  }

  @Override
  public DataSource register(UUID userId, RegisterDataSourceRequest request) {
    try {
      return txManager.withTransaction(
          (ctx) -> {
            Optional<Catalog> catalog = catalogRepository.findByName(ctx, request.getCatalogName());
            if (catalog.isEmpty()) {
              throw new IllegalArgumentException("Catalog not found: " + request.getCatalogName());
            }
            if (!authorizationService.authorize(
                userId,
                List.of(
                    new PermissionRequirement(
                        new ResourceRef(ResourceType.CATALOG, catalog.get().getId()),
                        Set.of(Permission.CATALOG_WRITE, Permission.CATALOG_ADMIN))))) {
              throw new AnalyticsException(
                  AnalyticsErrorCode.ACCESS_DENIED,
                  Map.of(
                      "user_id", userId.toString(),
                      "catalog_name", request.getCatalogName()));
            }
            return this.dataSourceService.registerDataSource(ctx, request);
          });
    } catch (SchemaResolverException e) {
      throw new AnalyticsException(AnalyticsErrorCode.DATA_SOURCE_UNREACHABLE, e);
    }
  }

  @Override
  public boolean deleteDataSource(
      UUID userId, String catalogName, String dataSourceName, boolean cascade) {
    return txManager.withTransaction(
        (ctx) -> {
          Optional<DataSource> dataSource =
              dataSourceQueryService.findByName(ctx, catalogName, dataSourceName);
          if (dataSource.isEmpty()) {
            return false;
          }
          if (!authorizationService.authorize(
              userId, dataSourceAdminRequirements(dataSource.get()))) {
            throw new AnalyticsException(
                AnalyticsErrorCode.ACCESS_DENIED,
                Map.of(
                    "user_id", userId.toString(),
                    "catalog_name", catalogName,
                    "data_source_name", dataSourceName));
          }
          deleteImpl(ctx, dataSource.get(), cascade);
          return true;
        });
  }

  @Override
  public boolean deleteDataSourceById(UUID userId, UUID dataSourceId, boolean cascade) {
    return txManager.withTransaction(
        (ctx) -> {
          Optional<DataSource> dataSource = dataSourceRepository.findById(ctx, dataSourceId);
          if (dataSource.isEmpty()) {
            return false;
          }
          if (!authorizationService.authorize(
              userId, dataSourceAdminRequirements(dataSource.get()))) {
            throw new AnalyticsException(
                AnalyticsErrorCode.ACCESS_DENIED,
                Map.of("user_id", userId.toString(), "data_source_name", dataSourceId.toString()));
          }
          deleteImpl(ctx, dataSource.get(), cascade);
          return true;
        });
  }

  private void deleteImpl(T ctx, DataSource dataSource, boolean cascade) {

    if (!cascade) {
      List<DataSourceNamespace> namespaces =
          dataSourceNamespaceQueryService.listByDataSourceId(ctx, dataSource.getId());
      if (!namespaces.isEmpty()) {
        throw new AnalyticsException(
            AnalyticsErrorCode.DATA_SOURCE_NOT_EMPTY,
            Map.of("data_source_name", dataSource.getName()));
      }
      aceRepository.deleteByResourceId(ctx, dataSource.getId());
      dataSourceRepository.deleteById(ctx, dataSource.getId());
    } else {
      dataSourceService.deleteDataSourceCascade(ctx, dataSource.getId());
    }
  }

  private static List<PermissionRequirement> dataSourceReadRequirements(DataSource dataSource) {
    return List.of(
        new PermissionRequirement(
            new ResourceRef(ResourceType.DATA_SOURCE, dataSource.getId()),
            Set.of(Permission.DATA_SOURCE_READ, Permission.DATA_SOURCE_ADMIN)),
        new PermissionRequirement(
            new ResourceRef(ResourceType.CATALOG, dataSource.getCatalogId()),
            Set.of(Permission.CATALOG_READ, Permission.CATALOG_ADMIN)));
  }

  private static List<PermissionRequirement> dataSourceAdminRequirements(DataSource dataSource) {
    return List.of(
        new PermissionRequirement(
            new ResourceRef(ResourceType.DATA_SOURCE, dataSource.getId()),
            Set.of(Permission.DATA_SOURCE_ADMIN)),
        new PermissionRequirement(
            new ResourceRef(ResourceType.CATALOG, dataSource.getCatalogId()),
            Set.of(Permission.CATALOG_ADMIN)));
  }
}
