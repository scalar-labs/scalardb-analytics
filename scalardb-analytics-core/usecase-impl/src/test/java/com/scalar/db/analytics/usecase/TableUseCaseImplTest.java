/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTable;
import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableDetail;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.repository.DataSourceNamespaceTableQueryService;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TableUseCaseImplTest {
  private static final String CATALOG_NAME = "test-catalog";
  private static final String DATA_SOURCE_NAME = "test-datasource";
  private static final String TABLE_NAME = "test-table";
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final UUID DATA_SOURCE_ID = UUID.randomUUID();
  private static final UUID NAMESPACE_ID = UUID.randomUUID();
  private static final UUID TABLE_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final List<String> NAMESPACE_NAMES = List.of("public");

  @Mock
  private DataSourceNamespaceTableQueryService<RepositoryTransactionContext> tableQueryService;

  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> transactionManager;
  @Mock private AuthorizationService authorizationService;

  private TableUseCaseImpl<RepositoryTransactionContext> tableUseCase;
  private DataSource testDataSource;
  private Namespace testNamespace;
  private DataSourceNamespaceTable testTable;
  private DataSourceNamespaceTableDetail testTableDetail;

  @BeforeEach
  void setUp() {
    tableUseCase =
        new TableUseCaseImpl<>(tableQueryService, transactionManager, authorizationService);

    DataSourceProvider provider = new PostgreSql("localhost", 5432, "user", "password", "test");
    testDataSource = new DataSource(DATA_SOURCE_ID, CATALOG_ID, DATA_SOURCE_NAME, provider);
    testNamespace = new Namespace(NAMESPACE_ID, DATA_SOURCE_ID, NAMESPACE_NAMES);

    TableInfo tableInfo = new TableInfo(TABLE_ID, NAMESPACE_ID, TABLE_NAME);
    Table table = new Table(tableInfo);
    testTable = new DataSourceNamespaceTable(testDataSource, testNamespace, table);

    TableDetail tableDetail = new TableDetail(tableInfo, Collections.emptyList());
    testTableDetail =
        new DataSourceNamespaceTableDetail(testDataSource, testNamespace, tableDetail);

    org.mockito.Mockito.lenient()
        .when(authorizationService.authorize(any(), any()))
        .thenReturn(true);
  }

  // listTables

  @Test
  void listTables_shouldFilterUnauthorizedTables() throws Throwable {
    TableInfo info2 = new TableInfo(UUID.randomUUID(), NAMESPACE_ID, "other-table");
    Table table2 = new Table(info2);
    DataSourceNamespaceTable t2 =
        new DataSourceNamespaceTable(testDataSource, testNamespace, table2);
    when(tableQueryService.listByCatalogName(any(), eq(CATALOG_NAME)))
        .thenReturn(List.of(testTable, t2));
    when(authorizationService.filterAuthorized(eq(USER_ID), any(), any())).thenReturn(List.of(t2));

    List<DataSourceNamespaceTable> result = tableUseCase.listTables(USER_ID, CATALOG_NAME);

    assertThat(result).containsExactly(t2);
  }

  // listTablesByNamespace

  @Test
  void listTablesByNamespace_shouldFilterUnauthorizedTables() throws Throwable {
    when(tableQueryService.listByNamespaceNames(
            any(), eq(CATALOG_NAME), eq(DATA_SOURCE_NAME), eq(NAMESPACE_NAMES)))
        .thenReturn(List.of(testTable));
    when(authorizationService.filterAuthorized(eq(USER_ID), any(), any()))
        .thenReturn(List.of(testTable));

    List<DataSourceNamespaceTable> result =
        tableUseCase.listTablesByNamespace(
            USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES);

    assertThat(result).containsExactly(testTable);
  }

  // describeTable

  @Test
  void describeTable_shouldAuthorizeAndReturnDetail() throws Throwable {
    when(tableQueryService.findDetailByName(
            any(), eq(CATALOG_NAME), eq(DATA_SOURCE_NAME), eq(NAMESPACE_NAMES), eq(TABLE_NAME)))
        .thenReturn(Optional.of(testTableDetail));

    Optional<DataSourceNamespaceTableDetail> result =
        tableUseCase.describeTable(
            USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES, TABLE_NAME);

    assertThat(result).isPresent().contains(testTableDetail);
    verify(authorizationService).authorize(eq(USER_ID), any());
  }

  @Test
  void describeTable_whenAccessDenied_shouldThrowException() throws Throwable {
    when(tableQueryService.findDetailByName(
            any(), eq(CATALOG_NAME), eq(DATA_SOURCE_NAME), eq(NAMESPACE_NAMES), eq(TABLE_NAME)))
        .thenReturn(Optional.of(testTableDetail));
    when(authorizationService.authorize(eq(USER_ID), any())).thenReturn(false);

    assertThatThrownBy(
            () ->
                tableUseCase.describeTable(
                    USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES, TABLE_NAME))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void describeTable_whenNotFound_shouldReturnEmpty() throws Throwable {
    when(tableQueryService.findDetailByName(
            any(), eq(CATALOG_NAME), eq(DATA_SOURCE_NAME), eq(NAMESPACE_NAMES), eq(TABLE_NAME)))
        .thenReturn(Optional.empty());

    Optional<DataSourceNamespaceTableDetail> result =
        tableUseCase.describeTable(
            USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES, TABLE_NAME);

    assertThat(result).isEmpty();
    verify(authorizationService, never()).authorize(any(), any());
  }

  // describeTableById

  @Test
  void describeTableById_shouldAuthorizeAndReturnDetail() throws Throwable {
    when(tableQueryService.findDetailById(any(), eq(TABLE_ID)))
        .thenReturn(Optional.of(testTableDetail));

    Optional<DataSourceNamespaceTableDetail> result =
        tableUseCase.describeTableById(USER_ID, TABLE_ID);

    assertThat(result).isPresent().contains(testTableDetail);
    verify(authorizationService).authorize(eq(USER_ID), any());
  }

  @Test
  void describeTableById_whenAccessDenied_shouldThrowException() throws Throwable {
    when(tableQueryService.findDetailById(any(), eq(TABLE_ID)))
        .thenReturn(Optional.of(testTableDetail));
    when(authorizationService.authorize(eq(USER_ID), any())).thenReturn(false);

    assertThatThrownBy(() -> tableUseCase.describeTableById(USER_ID, TABLE_ID))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void describeTableById_whenNotFound_shouldReturnEmpty() throws Throwable {
    when(tableQueryService.findDetailById(any(), eq(TABLE_ID))).thenReturn(Optional.empty());

    Optional<DataSourceNamespaceTableDetail> result =
        tableUseCase.describeTableById(USER_ID, TABLE_ID);

    assertThat(result).isEmpty();
    verify(authorizationService, never()).authorize(any(), any());
  }

  // RepositoryException wrapping

  @Test
  void listTables_whenRepositoryThrowsException_shouldPropagateAnalyticsException()
      throws Throwable {
    AnalyticsException analyticsException =
        new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
    when(tableQueryService.listByCatalogName(any(), eq(CATALOG_NAME)))
        .thenThrow(analyticsException);

    assertThatThrownBy(() -> tableUseCase.listTables(USER_ID, CATALOG_NAME))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(analyticsException);
  }

  @Test
  void describeTable_whenRepositoryThrowsException_shouldPropagateAnalyticsException()
      throws Throwable {
    AnalyticsException analyticsException =
        new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
    when(tableQueryService.findDetailByName(
            any(), eq(CATALOG_NAME), eq(DATA_SOURCE_NAME), eq(NAMESPACE_NAMES), eq(TABLE_NAME)))
        .thenThrow(analyticsException);

    assertThatThrownBy(
            () ->
                tableUseCase.describeTable(
                    USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES, TABLE_NAME))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(analyticsException);
  }
}
