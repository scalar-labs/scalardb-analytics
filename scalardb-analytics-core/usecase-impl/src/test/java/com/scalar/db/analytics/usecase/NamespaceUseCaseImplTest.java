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
import com.scalar.db.analytics.repository.DataSourceNamespaceQueryService;
import com.scalar.db.analytics.repository.DataSourceRepository;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.service.authz.AuthorizationService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NamespaceUseCaseImplTest {
  private static final String CATALOG_NAME = "test-catalog";
  private static final String DATA_SOURCE_NAME = "test-datasource";
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final UUID DATA_SOURCE_ID = UUID.randomUUID();
  private static final UUID NAMESPACE_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();
  private static final List<String> NAMESPACE_NAMES = List.of("public");

  @Mock private DataSourceNamespaceQueryService<RepositoryTransactionContext> namespaceQueryService;

  @Mock private NamespaceRepository<RepositoryTransactionContext> namespaceRepository;
  @Mock private DataSourceRepository<RepositoryTransactionContext> dataSourceRepository;
  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> transactionManager;
  @Mock private AuthorizationService authorizationService;

  private NamespaceUseCaseImpl<RepositoryTransactionContext> namespaceUseCase;
  private DataSource testDataSource;
  private Namespace testNamespace;
  private DataSourceNamespace testDataSourceNamespace;

  @BeforeEach
  void setUp() {
    namespaceUseCase =
        new NamespaceUseCaseImpl<>(
            namespaceQueryService,
            namespaceRepository,
            dataSourceRepository,
            transactionManager,
            authorizationService);

    DataSourceProvider provider = new PostgreSql("localhost", 5432, "user", "password", "test");
    testDataSource = new DataSource(DATA_SOURCE_ID, CATALOG_ID, DATA_SOURCE_NAME, provider);
    testNamespace = new Namespace(NAMESPACE_ID, DATA_SOURCE_ID, NAMESPACE_NAMES);
    testDataSourceNamespace = new DataSourceNamespace(testDataSource, testNamespace);

    org.mockito.Mockito.lenient()
        .when(authorizationService.authorize(any(), any()))
        .thenReturn(true);
  }

  // listNamespaces

  @Test
  void listNamespaces_shouldFilterUnauthorizedNamespaces() throws Throwable {
    Namespace ns2 = new Namespace(UUID.randomUUID(), DATA_SOURCE_ID, List.of("schema2"));
    DataSourceNamespace dsNs2 = new DataSourceNamespace(testDataSource, ns2);
    when(namespaceQueryService.listByCatalogName(any(), eq(CATALOG_NAME)))
        .thenReturn(List.of(testDataSourceNamespace, dsNs2));
    when(authorizationService.filterAuthorized(eq(USER_ID), any(), any()))
        .thenReturn(List.of(dsNs2));

    List<DataSourceNamespace> result = namespaceUseCase.listNamespaces(USER_ID, CATALOG_NAME);

    assertThat(result).containsExactly(dsNs2);
  }

  // describeNamespace

  @Test
  void describeNamespace_shouldAuthorizeAndReturnNamespace() throws Throwable {
    when(namespaceQueryService.findByCatalogAndDataSourceAndNames(
            any(), eq(CATALOG_NAME), eq(DATA_SOURCE_NAME), eq(NAMESPACE_NAMES)))
        .thenReturn(Optional.of(testDataSourceNamespace));

    Optional<Namespace> result =
        namespaceUseCase.describeNamespace(
            USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES);

    assertThat(result).isPresent().contains(testNamespace);
    verify(authorizationService).authorize(eq(USER_ID), any());
  }

  @Test
  void describeNamespace_whenAccessDenied_shouldThrowException() throws Throwable {
    when(namespaceQueryService.findByCatalogAndDataSourceAndNames(
            any(), eq(CATALOG_NAME), eq(DATA_SOURCE_NAME), eq(NAMESPACE_NAMES)))
        .thenReturn(Optional.of(testDataSourceNamespace));
    when(authorizationService.authorize(eq(USER_ID), any())).thenReturn(false);

    assertThatThrownBy(
            () ->
                namespaceUseCase.describeNamespace(
                    USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void describeNamespace_whenNotFound_shouldReturnEmpty() throws Throwable {
    when(namespaceQueryService.findByCatalogAndDataSourceAndNames(
            any(), eq(CATALOG_NAME), eq(DATA_SOURCE_NAME), eq(NAMESPACE_NAMES)))
        .thenReturn(Optional.empty());

    Optional<Namespace> result =
        namespaceUseCase.describeNamespace(
            USER_ID, CATALOG_NAME, DATA_SOURCE_NAME, NAMESPACE_NAMES);

    assertThat(result).isEmpty();
    verify(authorizationService, never()).authorize(any(), any());
  }

  // describeNamespaceById

  @Test
  void describeNamespaceById_shouldAuthorizeAndReturnNamespace() throws Throwable {
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID)))
        .thenReturn(Optional.of(testNamespace));
    when(dataSourceRepository.findById(any(), eq(DATA_SOURCE_ID)))
        .thenReturn(Optional.of(testDataSource));

    Optional<Namespace> result = namespaceUseCase.describeNamespaceById(USER_ID, NAMESPACE_ID);

    assertThat(result).isPresent().contains(testNamespace);
    verify(authorizationService).authorize(eq(USER_ID), any());
  }

  @Test
  void describeNamespaceById_whenAccessDenied_shouldThrowException() throws Throwable {
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID)))
        .thenReturn(Optional.of(testNamespace));
    when(dataSourceRepository.findById(any(), eq(DATA_SOURCE_ID)))
        .thenReturn(Optional.of(testDataSource));
    when(authorizationService.authorize(eq(USER_ID), any())).thenReturn(false);

    assertThatThrownBy(() -> namespaceUseCase.describeNamespaceById(USER_ID, NAMESPACE_ID))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void describeNamespaceById_whenNotFound_shouldReturnEmpty() throws Throwable {
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID))).thenReturn(Optional.empty());

    Optional<Namespace> result = namespaceUseCase.describeNamespaceById(USER_ID, NAMESPACE_ID);

    assertThat(result).isEmpty();
    verify(authorizationService, never()).authorize(any(), any());
  }

  @Test
  void describeNamespaceById_whenDataSourceMissing_shouldThrowIllegalStateException()
      throws Throwable {
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID)))
        .thenReturn(Optional.of(testNamespace));
    when(dataSourceRepository.findById(any(), eq(DATA_SOURCE_ID))).thenReturn(Optional.empty());

    assertThatThrownBy(() -> namespaceUseCase.describeNamespaceById(USER_ID, NAMESPACE_ID))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("Data integrity error");
  }

  // RepositoryException wrapping

  @Test
  void listNamespaces_whenRepositoryThrowsException_shouldPropagateAnalyticsException()
      throws Throwable {
    AnalyticsException analyticsException =
        new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
    when(namespaceQueryService.listByCatalogName(any(), eq(CATALOG_NAME)))
        .thenThrow(analyticsException);

    assertThatThrownBy(() -> namespaceUseCase.listNamespaces(USER_ID, CATALOG_NAME))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(analyticsException);
  }

  @Test
  void describeNamespaceById_whenRepositoryThrowsException_shouldPropagateAnalyticsException()
      throws Throwable {
    AnalyticsException analyticsException =
        new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID))).thenThrow(analyticsException);

    assertThatThrownBy(() -> namespaceUseCase.describeNamespaceById(USER_ID, NAMESPACE_ID))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(analyticsException);
  }
}
