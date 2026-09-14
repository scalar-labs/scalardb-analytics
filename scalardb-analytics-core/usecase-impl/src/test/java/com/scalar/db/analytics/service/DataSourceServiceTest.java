/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataType;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.api.request.RegisterDataSourceRequest;
import com.scalar.db.analytics.api.request.schema.ColumnSchema;
import com.scalar.db.analytics.api.request.schema.DataSourceSchema;
import com.scalar.db.analytics.api.request.schema.NamespaceSchema;
import com.scalar.db.analytics.api.request.schema.TableSchema;
import com.scalar.db.analytics.repository.CatalogRepository;
import com.scalar.db.analytics.repository.DataSourceNamespaceQueryService;
import com.scalar.db.analytics.repository.DataSourceNamespaceTableQueryService;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.service.datasource.SchemaResolverException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DataSourceServiceTest {

  private static final String CATALOG_NAME = "test-catalog";
  private static final String DATA_SOURCE_NAME = "test-datasource";

  @Mock private CatalogRepository<RepositoryTransactionContext> catalogRepository;
  @Mock private DataSourceRepository<RepositoryTransactionContext> dataSourceRepository;
  @Mock private NamespaceRepository<RepositoryTransactionContext> namespaceRepository;
  @Mock private TableRepository<RepositoryTransactionContext> tableRepository;
  @Mock private DataSourceNamespaceQueryService<RepositoryTransactionContext> namespaceQueryService;

  @Mock
  private DataSourceNamespaceTableQueryService<RepositoryTransactionContext>
      namespaceTableQueryService;

  @Mock private AccessControlEntryRepository<RepositoryTransactionContext> aceRepository;

  private DataSourceService<RepositoryTransactionContext> service;

  private final RepositoryTransactionContext ctx = new RepositoryTransactionContext() {};
  private final UUID catalogId = UUID.randomUUID();

  @BeforeEach
  void setUp() {
    service =
        new DataSourceService<>(
            catalogRepository,
            dataSourceRepository,
            namespaceRepository,
            tableRepository,
            namespaceQueryService,
            namespaceTableQueryService,
            aceRepository);
  }

  @Test
  void registerDataSource_withManualSchema_shouldPersistEntities() throws SchemaResolverException {
    // Arrange
    DataSourceSchema schema = createSchema("ns", "tbl", "id");
    RegisterDataSourceRequest request =
        new RegisterDataSourceRequest(
            CATALOG_NAME,
            DATA_SOURCE_NAME,
            DynamoDbProvider.builder().region("us-east-1").build(),
            schema);

    when(catalogRepository.findByName(ctx, CATALOG_NAME))
        .thenReturn(Optional.of(Catalog.of(catalogId, CATALOG_NAME)));

    // Act
    DataSource dataSource = service.registerDataSource(ctx, request);

    // Assert
    assertThat(dataSource.getName()).isEqualTo(DATA_SOURCE_NAME);
    ArgumentCaptor<DataSource> captor = ArgumentCaptor.forClass(DataSource.class);
    verify(dataSourceRepository).create(any(), captor.capture());
    assertThat(captor.getValue().getCatalogId()).isEqualTo(catalogId);
    verify(namespaceRepository).create(any(), any());
    verify(tableRepository).create(any(), any());
  }

  @Test
  void registerDataSource_manualProviderWithoutSchema_shouldThrowIllegalArgumentException()
      throws Exception {
    RegisterDataSourceRequest request =
        new RegisterDataSourceRequest(
            CATALOG_NAME,
            DATA_SOURCE_NAME,
            DynamoDbProvider.builder().region("us-east-1").build(),
            null);

    when(catalogRepository.findByName(ctx, CATALOG_NAME))
        .thenReturn(Optional.of(Catalog.of(catalogId, CATALOG_NAME)));

    assertThatThrownBy(() -> service.registerDataSource(ctx, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("requires a schema specification");

    verify(dataSourceRepository, never()).create(any(), any());
  }

  @Test
  void registerDataSource_autoProviderWithSchema_shouldThrowIllegalArgumentException()
      throws Exception {
    DataSourceSchema schema = createSchema("ns", "tbl", "id");
    RegisterDataSourceRequest request =
        new RegisterDataSourceRequest(
            CATALOG_NAME,
            DATA_SOURCE_NAME,
            PostgreSql.builder()
                .host("localhost")
                .port(5432)
                .username("user")
                .password("pass")
                .database("db")
                .build(),
            schema);

    when(catalogRepository.findByName(ctx, CATALOG_NAME))
        .thenReturn(Optional.of(Catalog.of(catalogId, CATALOG_NAME)));

    assertThatThrownBy(() -> service.registerDataSource(ctx, request))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("resolves schemas automatically");

    verify(dataSourceRepository, never()).create(any(), any());
  }

  @Test
  void deleteDataSourceCascade_shouldDeleteAcesForTablesNamespacesAndDataSource() throws Exception {
    UUID dataSourceId = UUID.randomUUID();
    UUID namespaceId = UUID.randomUUID();
    UUID tableId = UUID.randomUUID();

    DataSource dataSource =
        new DataSource(
            dataSourceId,
            catalogId,
            DATA_SOURCE_NAME,
            DynamoDbProvider.builder().region("us-east-1").build());
    Namespace namespace = new Namespace(namespaceId, dataSourceId, List.of("ns1"));
    DataSourceNamespace dsNamespace = new DataSourceNamespace(dataSource, namespace);

    TableInfo tableInfo = new TableInfo(tableId, namespaceId, "tbl1");
    Table table = new Table(tableInfo);
    DataSourceNamespaceTable dsTable = new DataSourceNamespaceTable(dataSource, namespace, table);

    when(namespaceQueryService.listByDataSourceId(ctx, dataSourceId))
        .thenReturn(List.of(dsNamespace));
    when(namespaceTableQueryService.listByNamespaceId(ctx, namespaceId))
        .thenReturn(List.of(dsTable));

    service.deleteDataSourceCascade(ctx, dataSourceId);

    // Verify ACEs are deleted for table, namespace, and data source
    verify(aceRepository).deleteByResourceId(ctx, tableId);
    verify(aceRepository).deleteByResourceId(ctx, namespaceId);
    verify(aceRepository).deleteByResourceId(ctx, dataSourceId);

    // Verify entities are deleted
    verify(tableRepository).deleteById(ctx, tableId);
    verify(namespaceRepository).deleteById(ctx, namespaceId);
    verify(dataSourceRepository).deleteById(ctx, dataSourceId);
  }

  private static DataSourceSchema createSchema(String namespace, String table, String column) {
    return DataSourceSchema.builder()
        .namespaces(
            List.of(
                NamespaceSchema.builder()
                    .names(List.of(namespace))
                    .tables(
                        List.of(
                            TableSchema.builder()
                                .name(table)
                                .columns(
                                    List.of(
                                        ColumnSchema.builder()
                                            .name(column)
                                            .type(DataType.Text.INSTANCE)
                                            .nullable(false)
                                            .build()))
                                .build()))
                    .build()))
        .build();
  }
}
