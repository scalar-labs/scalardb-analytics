/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.repository.DataSourceNamespaceTableQueryService;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import com.scalar.db.analytics.service.authz.PermissionRequirement;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class TableUseCaseImpl<T extends RepositoryTransactionContext> implements TableUseCase {
  private final DataSourceNamespaceTableQueryService<T> tableQueryService;
  private final RepositoryTransactionManager<T> txManager;
  private final AuthorizationService authorizationService;

  public TableUseCaseImpl(
      DataSourceNamespaceTableQueryService<T> tableQueryService,
      RepositoryTransactionManager<T> txManager,
      AuthorizationService authorizationService) {
    this.tableQueryService = tableQueryService;
    this.txManager = txManager;
    this.authorizationService = authorizationService;
  }

  @Override
  public List<DataSourceNamespaceTable> listTables(UUID userId, String catalogName) {
    List<DataSourceNamespaceTable> tables =
        tableQueryService.listByCatalogName(txManager.single(), catalogName);
    return authorizationService.filterAuthorized(
        userId, tables, TableUseCaseImpl::tableReadRequirements);
  }

  @Override
  public List<DataSourceNamespaceTable> listTablesByNamespace(
      UUID userId, String catalogName, String dataSourceName, List<String> namespaceNames) {
    List<DataSourceNamespaceTable> tables =
        tableQueryService.listByNamespaceNames(
            txManager.single(), catalogName, dataSourceName, namespaceNames);
    return authorizationService.filterAuthorized(
        userId, tables, TableUseCaseImpl::tableReadRequirements);
  }

  @Override
  public Optional<DataSourceNamespaceTableDetail> describeTable(
      UUID userId,
      String catalogName,
      String dataSourceName,
      List<String> namespaceNames,
      String tableName) {
    Optional<DataSourceNamespaceTableDetail> detail =
        tableQueryService.findDetailByName(
            txManager.single(), catalogName, dataSourceName, namespaceNames, tableName);
    if (detail.isEmpty()) {
      return Optional.empty();
    }
    if (!authorizationService.authorize(userId, tableDetailReadRequirements(detail.get()))) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED,
          Map.of(
              "user_id", userId.toString(),
              "catalog_name", catalogName,
              "data_source_name", dataSourceName,
              "table_name", tableName));
    }
    return detail;
  }

  @Override
  public Optional<DataSourceNamespaceTableDetail> describeTableById(UUID userId, UUID tableId) {
    Optional<DataSourceNamespaceTableDetail> detail =
        tableQueryService.findDetailById(txManager.single(), tableId);
    if (detail.isEmpty()) {
      return Optional.empty();
    }
    if (!authorizationService.authorize(userId, tableDetailReadRequirements(detail.get()))) {
      throw new AnalyticsException(
          AnalyticsErrorCode.ACCESS_DENIED,
          Map.of("user_id", userId.toString(), "table_name", tableId.toString()));
    }
    return detail;
  }

  private static List<PermissionRequirement> tableReadRequirements(DataSourceNamespaceTable table) {
    DataSource ds = table.getDataSource();
    Namespace ns = table.getNamespace();
    String providerType = ds.getProvider().getType();
    return List.of(
        new PermissionRequirement(
            new ResourceRef(ResourceType.TABLE, table.getTable().getInfo().getId(), providerType),
            Set.of(Permission.TABLE_READ)),
        new PermissionRequirement(
            new ResourceRef(ResourceType.NAMESPACE, ns.getId(), providerType),
            Set.of(Permission.NAMESPACE_READ)),
        new PermissionRequirement(
            new ResourceRef(ResourceType.DATA_SOURCE, ds.getId()),
            Set.of(Permission.DATA_SOURCE_READ, Permission.DATA_SOURCE_ADMIN)),
        new PermissionRequirement(
            new ResourceRef(ResourceType.CATALOG, ds.getCatalogId()),
            Set.of(Permission.CATALOG_READ, Permission.CATALOG_ADMIN)));
  }

  private static List<PermissionRequirement> tableDetailReadRequirements(
      DataSourceNamespaceTableDetail detail) {
    DataSource ds = detail.getDataSource();
    Namespace ns = detail.getNamespace();
    String providerType = ds.getProvider().getType();
    return List.of(
        new PermissionRequirement(
            new ResourceRef(ResourceType.TABLE, detail.getTable().getInfo().getId(), providerType),
            Set.of(Permission.TABLE_READ)),
        new PermissionRequirement(
            new ResourceRef(ResourceType.NAMESPACE, ns.getId(), providerType),
            Set.of(Permission.NAMESPACE_READ)),
        new PermissionRequirement(
            new ResourceRef(ResourceType.DATA_SOURCE, ds.getId()),
            Set.of(Permission.DATA_SOURCE_READ, Permission.DATA_SOURCE_ADMIN)),
        new PermissionRequirement(
            new ResourceRef(ResourceType.CATALOG, ds.getCatalogId()),
            Set.of(Permission.CATALOG_READ, Permission.CATALOG_ADMIN)));
  }
}
