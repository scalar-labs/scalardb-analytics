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
import com.scalar.db.analytics.api.model.DataSourceNamespace;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.PostgreSql;
import com.scalar.db.analytics.lib.functional.ThrowableFunction;
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
import com.scalar.db.analytics.service.authz.AuthorizationService;
import java.util.Arrays;
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
class DataSourceUseCaseImplTest {
  private static final String CATALOG_NAME = "test-catalog";
  private static final String DATA_SOURCE_NAME = "test-datasource";
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final UUID DATA_SOURCE_ID = UUID.randomUUID();
  private static final UUID NAMESPACE_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private CatalogRepository<RepositoryTransactionContext> catalogRepository;
  @Mock private DataSourceRepository<RepositoryTransactionContext> dataSourceRepository;
  @Mock private NamespaceRepository<RepositoryTransactionContext> namespaceRepository;
  @Mock private TableRepository<RepositoryTransactionContext> tableRepository;
  @Mock private DataSourceQueryService<RepositoryTransactionContext> dataSourceQueryService;

  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> transactionManager;

  @Mock
  private DataSourceNamespaceQueryService<RepositoryTransactionContext>
      dataSourceNamespaceQueryService;

  @Mock
  private DataSourceNamespaceTableQueryService<RepositoryTransactionContext>
      dataSourceNamespaceTableQueryService;

  @Mock private RepositoryTransactionContext transactionContext;
  @Mock private AccessControlEntryRepository<RepositoryTransactionContext> aceRepository;
  @Mock private AuthorizationService authorizationService;

  private DataSourceUseCaseImpl<RepositoryTransactionContext> dataSourceUseCase;

  private DataSource testDataSource;
  private DataSourceProvider testProvider;

  @BeforeEach
  void setUp() {
    dataSourceUseCase =
        new DataSourceUseCaseImpl<>(
            catalogRepository,
            dataSourceRepository,
            namespaceRepository,
            tableRepository,
            dataSourceQueryService,
            transactionManager,
            dataSourceNamespaceQueryService,
            dataSourceNamespaceTableQueryService,
            aceRepository,
            authorizationService);

    testProvider = new PostgreSql("localhost", 5432, "user", "password", "test");
    testDataSource = new DataSource(DATA_SOURCE_ID, CATALOG_ID, DATA_SOURCE_NAME, testProvider);

    // Default: authorize returns true (happy path)
    org.mockito.Mockito.lenient()
        .when(authorizationService.authorize(any(), any()))
        .thenReturn(true);
    org.mockito.Mockito.lenient()
        .when(authorizationService.authorizeSuperAdmin(any()))
        .thenReturn(true);
  }

  @Test
  void findDataSource_shouldAuthorizeAndReturnDataSource() throws Throwable {
    when(dataSourceQueryService.findByName(any(), eq(CATALOG_NAME), eq(DATA_SOURCE_NAME)))
        .thenReturn(Optional.of(testDataSource));

    Optional<DataSource> result =
        dataSourceUseCase.findDataSource(USER_ID, CATALOG_NAME, DATA_SOURCE_NAME);

    assertThat(result).isPresent().contains(testDataSource);
    verify(authorizationService).authorize(eq(USER_ID), any());
  }

  @Test
  void findDataSource_whenAccessDenied_shouldThrowException() throws Throwable {
    when(dataSourceQueryService.findByName(any(), eq(CATALOG_NAME), eq(DATA_SOURCE_NAME)))
        .thenReturn(Optional.of(testDataSource));
    when(authorizationService.authorize(eq(USER_ID), any())).thenReturn(false);

    assertThatThrownBy(
            () -> dataSourceUseCase.findDataSource(USER_ID, CATALOG_NAME, DATA_SOURCE_NAME))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void listDataSources_shouldFilterUnauthorizedDataSources() throws Throwable {
    DataSource ds2 = new DataSource(UUID.randomUUID(), CATALOG_ID, "ds2", testProvider);
    when(dataSourceQueryService.listByCatalogName(any(), eq(CATALOG_NAME)))
        .thenReturn(List.of(testDataSource, ds2));
    when(authorizationService.filterAuthorized(eq(USER_ID), any(), any())).thenReturn(List.of(ds2));

    List<DataSource> result = dataSourceUseCase.listDataSources(USER_ID, CATALOG_NAME);

    assertThat(result).containsExactly(ds2);
  }

  @Test
  void deleteDataSource_WithEmptyNamespacesAndCascadeFalse_ShouldDeleteDataSource()
      throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(dataSourceQueryService.findByName(transactionContext, CATALOG_NAME, DATA_SOURCE_NAME))
        .thenReturn(Optional.of(testDataSource));
    when(dataSourceNamespaceQueryService.listByDataSourceId(transactionContext, DATA_SOURCE_ID))
        .thenReturn(Collections.emptyList());

    boolean result =
        dataSourceUseCase.deleteDataSource(USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, false);

    assertThat(result).isTrue();
    verify(authorizationService).authorize(eq(USER_ID), any());
    verify(aceRepository).deleteByResourceId(transactionContext, DATA_SOURCE_ID);
    verify(dataSourceRepository).deleteById(transactionContext, DATA_SOURCE_ID);
  }

  @Test
  void deleteDataSource_WithEmptyNamespacesAndCascadeTrue_ShouldCallDeleteDataSourceCascade()
      throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(dataSourceQueryService.findByName(transactionContext, CATALOG_NAME, DATA_SOURCE_NAME))
        .thenReturn(Optional.of(testDataSource));

    boolean result =
        dataSourceUseCase.deleteDataSource(USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, true);

    assertThat(result).isTrue();
    verify(authorizationService).authorize(eq(USER_ID), any());
    verify(dataSourceRepository).deleteById(transactionContext, DATA_SOURCE_ID);
  }

  @Test
  void deleteDataSource_WithNonEmptyNamespacesAndCascadeFalse_ShouldThrowException()
      throws Throwable {
    Namespace namespace = new Namespace(NAMESPACE_ID, DATA_SOURCE_ID, Arrays.asList("schema1"));
    DataSourceNamespace dataSourceNamespace = new DataSourceNamespace(testDataSource, namespace);
    List<DataSourceNamespace> namespaces = Collections.singletonList(dataSourceNamespace);

    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(dataSourceQueryService.findByName(transactionContext, CATALOG_NAME, DATA_SOURCE_NAME))
        .thenReturn(Optional.of(testDataSource));
    when(dataSourceNamespaceQueryService.listByDataSourceId(transactionContext, DATA_SOURCE_ID))
        .thenReturn(namespaces);

    assertThatThrownBy(
            () ->
                dataSourceUseCase.deleteDataSource(USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, false))
        .isInstanceOf(AnalyticsException.class)
        .hasMessageContaining(DATA_SOURCE_NAME);

    verify(dataSourceRepository, never()).deleteById(any(), any());
  }

  @Test
  void deleteDataSource_WhenDataSourceNotFound_ShouldReturnFalse() throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(dataSourceQueryService.findByName(transactionContext, CATALOG_NAME, DATA_SOURCE_NAME))
        .thenReturn(Optional.empty());

    boolean result =
        dataSourceUseCase.deleteDataSource(USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, false);

    assertThat(result).isFalse();
    verify(dataSourceRepository, never()).deleteById(any(), any());
    verify(dataSourceNamespaceQueryService, never()).listByDataSourceId(any(), any());
  }

  @Test
  void deleteDataSource_WhenRepositoryThrowsException_ShouldPropagateAnalyticsException()
      throws Throwable {
    AnalyticsException analyticsException =
        new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
    when(transactionManager.withTransaction(any())).thenThrow(analyticsException);

    assertThatThrownBy(
            () ->
                dataSourceUseCase.deleteDataSource(USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, false))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(analyticsException);
  }

  @Test
  void deleteDataSourceById_WithEmptyNamespacesAndCascadeFalse_ShouldDeleteDataSource()
      throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(dataSourceRepository.findById(transactionContext, DATA_SOURCE_ID))
        .thenReturn(Optional.of(testDataSource));
    when(dataSourceNamespaceQueryService.listByDataSourceId(transactionContext, DATA_SOURCE_ID))
        .thenReturn(Collections.emptyList());

    boolean result = dataSourceUseCase.deleteDataSourceById(USER_ID, DATA_SOURCE_ID, false);

    assertThat(result).isTrue();
    verify(authorizationService).authorize(eq(USER_ID), any());
    verify(aceRepository).deleteByResourceId(transactionContext, DATA_SOURCE_ID);
    verify(dataSourceRepository).deleteById(transactionContext, DATA_SOURCE_ID);
  }

  @Test
  void deleteDataSourceById_WithNonEmptyNamespacesAndCascadeFalse_ShouldThrowException()
      throws Throwable {
    Namespace namespace =
        new Namespace(NAMESPACE_ID, DATA_SOURCE_ID, Arrays.asList("schema1", "schema2"));
    DataSourceNamespace dataSourceNamespace = new DataSourceNamespace(testDataSource, namespace);
    List<DataSourceNamespace> namespaces = Collections.singletonList(dataSourceNamespace);

    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(dataSourceRepository.findById(transactionContext, DATA_SOURCE_ID))
        .thenReturn(Optional.of(testDataSource));
    when(dataSourceNamespaceQueryService.listByDataSourceId(transactionContext, DATA_SOURCE_ID))
        .thenReturn(namespaces);

    assertThatThrownBy(() -> dataSourceUseCase.deleteDataSourceById(USER_ID, DATA_SOURCE_ID, false))
        .isInstanceOf(AnalyticsException.class)
        .hasMessageContaining(DATA_SOURCE_NAME);

    verify(dataSourceRepository, never()).deleteById(any(), any());
  }

  @Test
  void deleteDataSourceById_WhenRepositoryThrowsException_ShouldPropagateAnalyticsException()
      throws Throwable {
    AnalyticsException analyticsException =
        new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
    when(transactionManager.withTransaction(any())).thenThrow(analyticsException);

    assertThatThrownBy(() -> dataSourceUseCase.deleteDataSourceById(USER_ID, DATA_SOURCE_ID, false))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(analyticsException);
  }

  @Test
  void deleteDataSource_whenAccessDenied_shouldThrowException() throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(dataSourceQueryService.findByName(transactionContext, CATALOG_NAME, DATA_SOURCE_NAME))
        .thenReturn(Optional.of(testDataSource));
    when(authorizationService.authorize(eq(USER_ID), any())).thenReturn(false);

    assertThatThrownBy(
            () ->
                dataSourceUseCase.deleteDataSource(USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, false))
        .isInstanceOf(AnalyticsException.class);
  }
}
