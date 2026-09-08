/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.lib.functional.Throwables;
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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class CatalogUseCaseImpl<T extends RepositoryTransactionContext> implements CatalogUseCase {
  private final CatalogRepository<T> catalogRepository;
  private final DataSourceQueryService<T> dataSourceQueryService;
  private final RepositoryTransactionManager<T> txManager;
  private final DataSourceService<T> dataSourceService;
  private final AccessControlEntryRepository<T> aceRepository;
  private final AuthorizationService authorizationService;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public CatalogUseCaseImpl(
      CatalogRepository<T> catalogRepository,
      DataSourceRepository<T> dataSourceRepository,
      DataSourceQueryService<T> dataSourceQueryService,
      NamespaceRepository<T> nameSpaceRepository,
      TableRepository<T> tableRepository,
      RepositoryTransactionManager<T> txManager,
      DataSourceNamespaceQueryService<T> dataSourceNamespaceQueryService,
      DataSourceNamespaceTableQueryService<T> dataSourceNamespaceTableQueryService,
      AccessControlEntryRepository<T> aceRepository,
      AuthorizationService authorizationService) {
    this.catalogRepository = catalogRepository;
    this.dataSourceQueryService = dataSourceQueryService;
    this.txManager = txManager;
    this.aceRepository = aceRepository;
    this.authorizationService = authorizationService;

    this.dataSourceService =
        new DataSourceService<>(
            catalogRepository,
            dataSourceRepository,
            nameSpaceRepository,
            tableRepository,
            dataSourceNamespaceQueryService,
            dataSourceNamespaceTableQueryService,
            aceRepository);
  }

  @Override
  public Catalog createCatalog(UUID userId, String catalogName) {
    if (!authorizationService.authorizeSuperAdmin(userId)) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED, Map.of("user_id", userId.toString()));
    }
    Catalog newCatalog = Catalog.create(catalogName);
    catalogRepository.create(txManager.single(), newCatalog);
    return newCatalog;
  }

  @Override
  public Optional<Catalog> findCatalog(UUID userId, String catalogName) {
    Optional<Catalog> catalog = catalogRepository.findByName(txManager.single(), catalogName);
    if (catalog.isEmpty()) {
      return Optional.empty();
    }
    if (!authorizationService.authorize(userId, catalogReadRequirements(catalog.get()))) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED,
          Map.of("user_id", userId.toString(), "catalog_name", catalogName));
    }
    return catalog;
  }

  @Override
  public Optional<Catalog> describeCatalogById(UUID userId, UUID catalogId) {
    Optional<Catalog> catalog = catalogRepository.findById(txManager.single(), catalogId);
    if (catalog.isEmpty()) {
      return Optional.empty();
    }
    if (!authorizationService.authorize(userId, catalogReadRequirements(catalog.get()))) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED,
          Map.of("user_id", userId.toString(), "catalog_name", catalogId.toString()));
    }
    return catalog;
  }

  @Override
  public List<Catalog> listAllCatalogs(UUID userId) {
    List<Catalog> catalogs = catalogRepository.list(txManager.single());
    return authorizationService.filterAuthorized(
        userId, catalogs, CatalogUseCaseImpl::catalogReadRequirements);
  }

  @Override
  public Optional<Catalog> initializeCatalog(
      UUID userId, String catalogName, List<RegisterDataSourceRequest> dataSources) {
    if (!authorizationService.authorizeSuperAdmin(userId)) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED, Map.of("user_id", userId.toString()));
    }
    return txManager.withTransaction(
        (ctx) -> {
          if (catalogRepository.findByName(ctx, catalogName).isPresent()) {
            return Optional.empty();
          }

          Catalog catalog = Catalog.create(catalogName);
          catalogRepository.create(ctx, catalog);

          dataSources.forEach(
              Throwables.sneakyThrowableConsumer(
                  ds -> dataSourceService.registerDataSource(ctx, ds)));

          return Optional.of(catalog);
        });
  }

  @Override
  public boolean deleteCatalog(UUID userId, String catalogName, boolean cascade) {
    return txManager.withTransaction(
        (ctx) -> {
          Optional<Catalog> catalog = catalogRepository.findByName(ctx, catalogName);
          if (catalog.isEmpty()) {
            return false;
          }
          if (!authorizationService.authorize(userId, catalogAdminRequirements(catalog.get()))) {
            throw new AnalyticsException(
                AnalyticsErrorCode.ACCESS_DENIED,
                Map.of("user_id", userId.toString(), "catalog_name", catalogName));
          }
          deleteImpl(ctx, catalog.get(), cascade);
          return true;
        });
  }

  @Override
  public boolean deleteCatalogById(UUID userId, UUID catalogId, boolean cascade) {
    return txManager.withTransaction(
        (ctx) -> {
          Optional<Catalog> catalog = catalogRepository.findById(ctx, catalogId);
          if (catalog.isEmpty()) {
            return false;
          }
          if (!authorizationService.authorize(userId, catalogAdminRequirements(catalog.get()))) {
            throw new AnalyticsException(
                AnalyticsErrorCode.ACCESS_DENIED,
                Map.of("user_id", userId.toString(), "catalog_name", catalogId.toString()));
          }
          deleteImpl(ctx, catalog.get(), cascade);
          return true;
        });
  }

  private void deleteImpl(T ctx, Catalog catalog, boolean cascade) {
    String catalogName = catalog.getName();

    List<DataSource> dataSources = dataSourceQueryService.listByCatalogName(ctx, catalogName);

    if (!cascade) {
      // Check for data sources
      if (!dataSources.isEmpty()) {
        throw new AnalyticsException(
            AnalyticsErrorCode.CATALOG_NOT_EMPTY, Map.of("catalog_name", catalogName));
      }
    } else {
      // Cascade delete all children
      // Delete all data sources (which will cascade delete namespaces, tables, columns)
      for (DataSource dataSource : dataSources) {
        dataSourceService.deleteDataSourceCascade(ctx, dataSource.getId());
      }
    }

    aceRepository.deleteByResourceId(ctx, catalog.getId());
    catalogRepository.deleteByName(ctx, catalogName);
  }

  private static List<PermissionRequirement> catalogReadRequirements(Catalog catalog) {
    return List.of(
        new PermissionRequirement(
            new ResourceRef(ResourceType.CATALOG, catalog.getId()),
            Set.of(Permission.CATALOG_READ, Permission.CATALOG_ADMIN)));
  }

  private static List<PermissionRequirement> catalogAdminRequirements(Catalog catalog) {
    return List.of(
        new PermissionRequirement(
            new ResourceRef(ResourceType.CATALOG, catalog.getId()),
            Set.of(Permission.CATALOG_ADMIN)));
  }
}
