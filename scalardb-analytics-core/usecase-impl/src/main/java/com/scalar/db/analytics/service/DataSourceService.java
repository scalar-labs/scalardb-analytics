/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service;

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.DataSourceProviderVisitor;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Oracle;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.SqlServer;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.datasource.scalardb.schema.ScalarDbSchemaResolver;
import com.scalar.db.analytics.lib.functional.Throwables;
import com.scalar.db.analytics.repository.CatalogRepository;
import com.scalar.db.analytics.repository.DataSourceNamespaceQueryService;
import com.scalar.db.analytics.repository.DataSourceNamespaceTableQueryService;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.service.datasource.NoOpSchemaResolver;
import com.scalar.db.analytics.service.datasource.ResolvedSchema;
import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import com.scalar.db.analytics.service.datasource.rdbms.RdbmsSchemaResolver;
import com.scalar.db.analytics.service.datasource.rdbms.RdbmsSchemaResolverOption;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverDatabricks;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverMySql;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverOracle;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverPostgreSql;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverSnowflake;
import com.scalar.db.analytics.service.datasource.rdbms.SchemaResolverSqlServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

public class DataSourceService<T extends RepositoryTransactionContext> {
  private final CatalogRepository<T> catalogRepository;
  private final DataSourceRepository<T> dataSourceRepository;
  private final NamespaceRepository<T> nameSpaceRepository;
  private final TableRepository<T> tableRepository;
  private final DataSourceNamespaceQueryService<T> namespaceQueryService;
  private final DataSourceNamespaceTableQueryService<T> tableQueryService;
  private final AccessControlEntryRepository<T> aceRepository;

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public DataSourceService(
      CatalogRepository<T> catalogRepository,
      DataSourceRepository<T> dataSourceRepository,
      NamespaceRepository<T> nameSpaceRepository,
      TableRepository<T> tableRepository,
      DataSourceNamespaceQueryService<T> namespaceQueryService,
      DataSourceNamespaceTableQueryService<T> tableQueryService,
      AccessControlEntryRepository<T> aceRepository) {
    this.catalogRepository = catalogRepository;
    this.dataSourceRepository = dataSourceRepository;
    this.nameSpaceRepository = nameSpaceRepository;
    this.tableRepository = tableRepository;
    this.namespaceQueryService = namespaceQueryService;
    this.tableQueryService = tableQueryService;
    this.aceRepository = aceRepository;
  }

  public DataSource registerDataSource(T ctx, RegisterDataSourceRequest request)
      throws SchemaResolverException {
    Catalog catalog =
        catalogRepository
            .findByName(ctx, request.getCatalogName())
            .orElseThrow(
                () ->
                    new IllegalArgumentException(
                        "The catalog does not exist. Catalog name:" + request.getCatalogName()));

    DataSource dataSource =
        DataSource.create(catalog.getId(), request.getName(), request.getProvider());

    DataSourceSchema schema = request.getSchema();
    DataSourceProvider provider = request.getProvider();
    ResolvedSchema resolvedSchema = resolveSchema(dataSource.getId(), provider, schema);

    dataSourceRepository.create(ctx, dataSource);
    resolvedSchema
        .getNamespaces()
        .forEach(Throwables.sneakyThrowableConsumer(ns -> nameSpaceRepository.create(ctx, ns)));
    resolvedSchema
        .getTables()
        .forEach(Throwables.sneakyThrowableConsumer(t -> tableRepository.create(ctx, t)));

    return dataSource;
  }

  private ResolvedSchema resolveSchema(
      UUID dataSourceId, DataSourceProvider provider, @Nullable DataSourceSchema schema)
      throws SchemaResolverException {
    boolean supportsResolution = provider.supportsSchemaResolution();
    if (supportsResolution) {
      if (schema != null) {
        throw new IllegalArgumentException(
            "Provider type '%s' resolves schemas automatically; omit manual schema."
                .formatted(provider.getType()));
      }
      SchemaResolverVisitor visitor = new SchemaResolverVisitor(dataSourceId);
      return provider.accept(visitor);
    }

    if (schema == null) {
      throw new IllegalArgumentException(
          "Provider type '%s' requires a schema specification.".formatted(provider.getType()));
    }

    return new NoOpSchemaResolver(dataSourceId, schema).resolveSchema();
  }

  public void deleteDataSourceCascade(T ctx, UUID dataSourceId) {
    // Explicitly delete all child entities in the correct order

    // 1. Get all namespaces for this data source
    List<DataSourceNamespace> namespaces =
        namespaceQueryService.listByDataSourceId(ctx, dataSourceId);

    // 2. For each namespace, delete all tables
    for (DataSourceNamespace namespace : namespaces) {
      List<DataSourceNamespaceTable> tables =
          tableQueryService.listByNamespaceId(ctx, namespace.getNamespace().getId());

      // 3. Delete each table's ACEs and then the table itself
      for (DataSourceNamespaceTable table : tables) {
        UUID tableId = table.getTable().getInfo().getId();
        aceRepository.deleteByResourceId(ctx, tableId);
        tableRepository.deleteById(ctx, tableId);
      }

      // 4. Delete the namespace's ACEs and then the namespace
      UUID namespaceId = namespace.getNamespace().getId();
      aceRepository.deleteByResourceId(ctx, namespaceId);
      nameSpaceRepository.deleteById(ctx, namespaceId);
    }

    // 5. Delete the data source's ACEs and then the data source itself
    aceRepository.deleteByResourceId(ctx, dataSourceId);
    dataSourceRepository.deleteById(ctx, dataSourceId);
  }

  private static class SchemaResolverVisitor implements DataSourceProviderVisitor<ResolvedSchema> {
    private final UUID dataSourceId;

    private SchemaResolverVisitor(UUID dataSourceId) {
      this.dataSourceId = dataSourceId;
    }

    @Override
    public ResolvedSchema visit(MySql mySql) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverMySql.open(
              dataSourceId, mySql, RdbmsSchemaResolverOption.DEFAULT, mySql.getDatabase())) {
        return resolver.resolveSchema();
      }
    }

    @Override
    public ResolvedSchema visit(PostgreSql postgreSql) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverPostgreSql.open(
              dataSourceId, postgreSql, RdbmsSchemaResolverOption.DEFAULT)) {
        return resolver.resolveSchema();
      }
    }

    @Override
    public ResolvedSchema visit(Oracle oracle) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverOracle.open(dataSourceId, oracle, RdbmsSchemaResolverOption.DEFAULT)) {
        return resolver.resolveSchema();
      }
    }

    @Override
    public ResolvedSchema visit(SqlServer sqlServer) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverSqlServer.open(
              dataSourceId,
              sqlServer,
              RdbmsSchemaResolverOption.DEFAULT,
              sqlServer.getDatabase())) {
        return resolver.resolveSchema();
      }
    }

    @Override
    public ResolvedSchema visit(ScalarDbProvider scalarDb) {
      return ScalarDbSchemaResolver.create(dataSourceId, scalarDb).resolveSchema();
    }

    @Override
    public ResolvedSchema visit(DynamoDbProvider dynamoDb) {
      throw new IllegalStateException(
          "Manual schema providers must be handled outside SchemaResolverVisitor");
    }

    @Override
    public ResolvedSchema visit(Databricks databricks) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverDatabricks.open(
              dataSourceId,
              databricks,
              RdbmsSchemaResolverOption.DEFAULT,
              databricks.getCatalog())) {
        return resolver.resolveSchema();
      }
    }

    @Override
    public ResolvedSchema visit(Snowflake snowflake) {
      try (RdbmsSchemaResolver resolver =
          SchemaResolverSnowflake.open(
              dataSourceId,
              snowflake,
              RdbmsSchemaResolverOption.DEFAULT,
              snowflake.getDatabase())) {
        return resolver.resolveSchema();
      }
    }
  }
}
