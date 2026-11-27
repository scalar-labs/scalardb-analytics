/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.RegisterDataSourceResult;
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
import com.scalar.db.analytics.datasource.jdbc.schema.DatabricksSchemaReader;
import com.scalar.db.analytics.datasource.jdbc.schema.JdbcSchemaReader;
import com.scalar.db.analytics.datasource.jdbc.schema.JdbcSchemaReaderOption;
import com.scalar.db.analytics.datasource.jdbc.schema.MySqlSchemaReader;
import com.scalar.db.analytics.datasource.jdbc.schema.OracleSchemaReader;
import com.scalar.db.analytics.datasource.jdbc.schema.PostgreSqlSchemaReader;
import com.scalar.db.analytics.datasource.jdbc.schema.SnowflakeSchemaReader;
import com.scalar.db.analytics.datasource.jdbc.schema.SqlServerSchemaReader;
import com.scalar.db.analytics.datasource.scalardb.schema.ScalarDbSchemaReader;
import com.scalar.db.analytics.lib.functional.Throwables;
import com.scalar.db.analytics.repository.CatalogRepository;
import com.scalar.db.analytics.repository.DataSourceNamespaceQueryService;
import com.scalar.db.analytics.repository.DataSourceNamespaceTableQueryService;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.schema.InvalidDataSourceConfigurationException;
import com.scalar.db.analytics.schema.ResolvedSchema;
import com.scalar.db.analytics.schema.SchemaMetadataValidator;
import com.scalar.db.analytics.schema.SchemaResolverException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
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

  public RegisterDataSourceResult registerDataSource(T ctx, RegisterDataSourceRequest request)
      throws SchemaResolverException {
    Catalog catalog =
        catalogRepository
            .findByName(ctx, request.getCatalogName())
            .orElseThrow(
                () ->
                    new AnalyticsException(
                        AnalyticsErrorCode.CATALOG_NOT_FOUND,
                        Map.of("catalog_name", request.getCatalogName())));

    DataSource dataSource =
        DataSource.create(catalog.getId(), request.getName(), request.getProvider());

    ResolvedSchema resolvedSchema = resolveSchema(dataSource.getId(), request);
    // Every schema, resolved or given manually, is checked before anything is persisted.
    SchemaMetadataValidator.validate(resolvedSchema);

    dataSourceRepository.create(ctx, dataSource);
    resolvedSchema
        .getNamespaces()
        .forEach(Throwables.sneakyThrowableConsumer(ns -> nameSpaceRepository.create(ctx, ns)));
    resolvedSchema
        .getTables()
        .forEach(Throwables.sneakyThrowableConsumer(t -> tableRepository.create(ctx, t)));

    return new RegisterDataSourceResult(dataSource, resolvedSchema.getSkippedColumns());
  }

  private ResolvedSchema resolveSchema(UUID dataSourceId, RegisterDataSourceRequest request)
      throws SchemaResolverException {
    DataSourceProvider provider = request.getProvider();
    @Nullable DataSourceSchema schema = request.getSchema();
    boolean supportsResolution = provider.supportsSchemaResolution();
    if (supportsResolution) {
      if (schema != null) {
        throw invalidSchema(request, "not_allowed_for_provider");
      }
      SchemaResolverVisitor visitor = new SchemaResolverVisitor(dataSourceId);
      try {
        return provider.accept(visitor);
      } catch (InvalidDataSourceConfigurationException e) {
        throw invalidProviderConfiguration(request, e);
      }
    }

    if (schema == null) {
      throw invalidSchema(request, "required_for_provider");
    }

    return ResolvedSchema.fromSupplied(dataSourceId, schema);
  }

  /**
   * Returns the error for a manual schema that is given for a provider that resolves schemas
   * automatically, or that is missing for a provider that does not.
   */
  private static AnalyticsException invalidSchema(
      RegisterDataSourceRequest request, String reason) {
    return new AnalyticsException(
        AnalyticsErrorCode.INVALID_ARGUMENT,
        Map.of(
            "field", "schema_json",
            "reason", reason,
            "catalog_name", request.getCatalogName(),
            "data_source_name", request.getName(),
            "provider_type", request.getProvider().getType()));
  }

  /**
   * Returns the error for a provider whose configuration the data source's client library (such as
   * ScalarDB or a JDBC driver) rejects. The library's message is kept in the cause for server logs
   * but not returned, since it can echo configuration values, which may include credentials.
   */
  private static AnalyticsException invalidProviderConfiguration(
      RegisterDataSourceRequest request, InvalidDataSourceConfigurationException e) {
    return new AnalyticsException(
        AnalyticsErrorCode.INVALID_ARGUMENT,
        Map.of(
            "field", "provider_payload_json",
            "reason", "invalid_configuration",
            "catalog_name", request.getCatalogName(),
            "data_source_name", request.getName(),
            "provider_type", request.getProvider().getType()),
        e);
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
      try (JdbcSchemaReader reader =
          MySqlSchemaReader.open(
              dataSourceId, mySql, JdbcSchemaReaderOption.DEFAULT, mySql.getDatabase())) {
        return reader.read();
      }
    }

    @Override
    public ResolvedSchema visit(PostgreSql postgreSql) {
      try (JdbcSchemaReader reader =
          PostgreSqlSchemaReader.open(dataSourceId, postgreSql, JdbcSchemaReaderOption.DEFAULT)) {
        return reader.read();
      }
    }

    @Override
    public ResolvedSchema visit(Oracle oracle) {
      try (JdbcSchemaReader reader =
          OracleSchemaReader.open(dataSourceId, oracle, JdbcSchemaReaderOption.DEFAULT)) {
        return reader.read();
      }
    }

    @Override
    public ResolvedSchema visit(SqlServer sqlServer) {
      try (JdbcSchemaReader reader =
          SqlServerSchemaReader.open(
              dataSourceId, sqlServer, JdbcSchemaReaderOption.DEFAULT, sqlServer.getDatabase())) {
        return reader.read();
      }
    }

    @Override
    public ResolvedSchema visit(ScalarDbProvider scalarDb) {
      return ScalarDbSchemaReader.create(dataSourceId, scalarDb).read();
    }

    @Override
    public ResolvedSchema visit(DynamoDbProvider dynamoDb) {
      throw new IllegalStateException(
          "Manual schema providers must be handled outside SchemaResolverVisitor");
    }

    @Override
    public ResolvedSchema visit(Databricks databricks) {
      try (JdbcSchemaReader reader =
          DatabricksSchemaReader.open(
              dataSourceId, databricks, JdbcSchemaReaderOption.DEFAULT, databricks.getCatalog())) {
        return reader.read();
      }
    }

    @Override
    public ResolvedSchema visit(Snowflake snowflake) {
      try (JdbcSchemaReader reader =
          SnowflakeSchemaReader.open(
              dataSourceId, snowflake, JdbcSchemaReaderOption.DEFAULT, snowflake.getDatabase())) {
        return reader.read();
      }
    }
  }
}
