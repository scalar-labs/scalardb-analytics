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
import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.api.model.DataSource;
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
class CatalogUseCaseImplTest {
  private static final String CATALOG_NAME = "test-catalog";
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final UUID USER_ID = UUID.randomUUID();

  @Mock private CatalogRepository<RepositoryTransactionContext> catalogRepository;
  @Mock private DataSourceRepository<RepositoryTransactionContext> dataSourceRepository;
  @Mock private DataSourceQueryService<RepositoryTransactionContext> dataSourceQueryService;
  @Mock private NamespaceRepository<RepositoryTransactionContext> namespaceRepository;
  @Mock private TableRepository<RepositoryTransactionContext> tableRepository;
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

  private CatalogUseCaseImpl<RepositoryTransactionContext> catalogUseCase;
  private Catalog testCatalog;

  @BeforeEach
  void setUp() {
    catalogUseCase =
        new CatalogUseCaseImpl<>(
            catalogRepository,
            dataSourceRepository,
            dataSourceQueryService,
            namespaceRepository,
            tableRepository,
            transactionManager,
            dataSourceNamespaceQueryService,
            dataSourceNamespaceTableQueryService,
            aceRepository,
            authorizationService);

    testCatalog = Catalog.of(CATALOG_ID, CATALOG_NAME);

    org.mockito.Mockito.lenient()
        .when(authorizationService.authorize(any(), any()))
        .thenReturn(true);
    org.mockito.Mockito.lenient()
        .when(authorizationService.authorizeSuperAdmin(any()))
        .thenReturn(true);
  }

  // createCatalog

  @Test
  void createCatalog_shouldCreateAndReturnCatalog() throws Throwable {
    Catalog result = catalogUseCase.createCatalog(USER_ID, CATALOG_NAME);

    assertThat(result.getName()).isEqualTo(CATALOG_NAME);
    verify(authorizationService).authorizeSuperAdmin(USER_ID);
    verify(catalogRepository).create(any(), any());
  }

  @Test
  void createCatalog_whenNotSuperAdmin_shouldThrowException() throws Throwable {
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> catalogUseCase.createCatalog(USER_ID, CATALOG_NAME))
        .isInstanceOf(AnalyticsException.class)
        .extracting(e -> ((AnalyticsException) e).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.ACCESS_DENIED);
    verify(catalogRepository, never()).create(any(), any());
  }

  // findCatalog

  @Test
  void findCatalog_shouldAuthorizeAndReturnCatalog() throws Throwable {
    when(catalogRepository.findByName(any(), eq(CATALOG_NAME)))
        .thenReturn(Optional.of(testCatalog));

    Optional<Catalog> result = catalogUseCase.findCatalog(USER_ID, CATALOG_NAME);

    assertThat(result).isPresent().contains(testCatalog);
    verify(authorizationService).authorize(eq(USER_ID), any());
  }

  @Test
  void findCatalog_whenAccessDenied_shouldThrowException() throws Throwable {
    when(catalogRepository.findByName(any(), eq(CATALOG_NAME)))
        .thenReturn(Optional.of(testCatalog));
    when(authorizationService.authorize(eq(USER_ID), any())).thenReturn(false);

    assertThatThrownBy(() -> catalogUseCase.findCatalog(USER_ID, CATALOG_NAME))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void findCatalog_whenNotFound_shouldReturnEmpty() throws Throwable {
    when(catalogRepository.findByName(any(), eq(CATALOG_NAME))).thenReturn(Optional.empty());

    Optional<Catalog> result = catalogUseCase.findCatalog(USER_ID, CATALOG_NAME);

    assertThat(result).isEmpty();
    verify(authorizationService, never()).authorize(any(), any());
  }

  // describeCatalogById

  @Test
  void describeCatalogById_shouldAuthorizeAndReturnCatalog() throws Throwable {
    when(catalogRepository.findById(any(), eq(CATALOG_ID))).thenReturn(Optional.of(testCatalog));

    Optional<Catalog> result = catalogUseCase.describeCatalogById(USER_ID, CATALOG_ID);

    assertThat(result).isPresent().contains(testCatalog);
    verify(authorizationService).authorize(eq(USER_ID), any());
  }

  @Test
  void describeCatalogById_whenAccessDenied_shouldThrowException() throws Throwable {
    when(catalogRepository.findById(any(), eq(CATALOG_ID))).thenReturn(Optional.of(testCatalog));
    when(authorizationService.authorize(eq(USER_ID), any())).thenReturn(false);

    assertThatThrownBy(() -> catalogUseCase.describeCatalogById(USER_ID, CATALOG_ID))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void describeCatalogById_whenNotFound_shouldReturnEmpty() throws Throwable {
    when(catalogRepository.findById(any(), eq(CATALOG_ID))).thenReturn(Optional.empty());

    Optional<Catalog> result = catalogUseCase.describeCatalogById(USER_ID, CATALOG_ID);

    assertThat(result).isEmpty();
    verify(authorizationService, never()).authorize(any(), any());
  }

  // listAllCatalogs

  @Test
  void listAllCatalogs_shouldFilterUnauthorizedCatalogs() throws Throwable {
    Catalog c2 = Catalog.of(UUID.randomUUID(), "other-catalog");
    when(catalogRepository.list(any())).thenReturn(List.of(testCatalog, c2));
    when(authorizationService.filterAuthorized(eq(USER_ID), any(), any())).thenReturn(List.of(c2));

    List<Catalog> result = catalogUseCase.listAllCatalogs(USER_ID);

    assertThat(result).containsExactly(c2);
  }

  // initializeCatalog

  @Test
  void initializeCatalog_shouldCreateCatalogAndReturnIt() throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Optional<Catalog>, Exception>
                  function = invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(catalogRepository.findByName(transactionContext, CATALOG_NAME))
        .thenReturn(Optional.empty());

    Optional<Catalog> result =
        catalogUseCase.initializeCatalog(USER_ID, CATALOG_NAME, Collections.emptyList());

    assertThat(result).isPresent();
    assertThat(result.get().getName()).isEqualTo(CATALOG_NAME);
    verify(authorizationService).authorizeSuperAdmin(USER_ID);
    verify(catalogRepository).create(eq(transactionContext), any());
  }

  @Test
  void initializeCatalog_whenNotSuperAdmin_shouldThrowException() throws Throwable {
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(false);

    assertThatThrownBy(
            () -> catalogUseCase.initializeCatalog(USER_ID, CATALOG_NAME, Collections.emptyList()))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void initializeCatalog_whenAlreadyExists_shouldReturnEmpty() throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Optional<Catalog>, Exception>
                  function = invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(catalogRepository.findByName(transactionContext, CATALOG_NAME))
        .thenReturn(Optional.of(testCatalog));

    Optional<Catalog> result =
        catalogUseCase.initializeCatalog(USER_ID, CATALOG_NAME, Collections.emptyList());

    assertThat(result).isEmpty();
    verify(catalogRepository, never()).create(any(), any());
  }

  // deleteCatalog

  @Test
  void deleteCatalog_withNoDataSourcesAndCascadeFalse_shouldDelete() throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(catalogRepository.findByName(transactionContext, CATALOG_NAME))
        .thenReturn(Optional.of(testCatalog));
    when(dataSourceQueryService.listByCatalogName(transactionContext, CATALOG_NAME))
        .thenReturn(Collections.emptyList());

    boolean result = catalogUseCase.deleteCatalog(USER_ID, CATALOG_NAME, false);

    assertThat(result).isTrue();
    verify(authorizationService).authorize(eq(USER_ID), any());
    verify(aceRepository).deleteByResourceId(transactionContext, CATALOG_ID);
    verify(catalogRepository).deleteByName(transactionContext, CATALOG_NAME);
  }

  @Test
  void deleteCatalog_whenAccessDenied_shouldThrowException() throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(catalogRepository.findByName(transactionContext, CATALOG_NAME))
        .thenReturn(Optional.of(testCatalog));
    when(authorizationService.authorize(eq(USER_ID), any())).thenReturn(false);

    assertThatThrownBy(() -> catalogUseCase.deleteCatalog(USER_ID, CATALOG_NAME, false))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void deleteCatalog_whenNotFound_shouldReturnFalse() throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(catalogRepository.findByName(transactionContext, CATALOG_NAME))
        .thenReturn(Optional.empty());

    boolean result = catalogUseCase.deleteCatalog(USER_ID, CATALOG_NAME, false);

    assertThat(result).isFalse();
    verify(catalogRepository, never()).deleteByName(any(), any());
  }

  @Test
  void deleteCatalog_withDataSourcesAndCascadeFalse_shouldThrowException() throws Throwable {
    DataSourceProvider provider = new PostgreSql("localhost", 5432, "user", "pass", "db");
    DataSource ds = new DataSource(UUID.randomUUID(), CATALOG_ID, "ds", provider);

    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(catalogRepository.findByName(transactionContext, CATALOG_NAME))
        .thenReturn(Optional.of(testCatalog));
    when(dataSourceQueryService.listByCatalogName(transactionContext, CATALOG_NAME))
        .thenReturn(List.of(ds));

    assertThatThrownBy(() -> catalogUseCase.deleteCatalog(USER_ID, CATALOG_NAME, false))
        .isInstanceOf(AnalyticsException.class)
        .hasMessageContaining(CATALOG_NAME);
  }

  // deleteCatalogById

  @Test
  void deleteCatalogById_withNoDataSourcesAndCascadeFalse_shouldDelete() throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(catalogRepository.findById(transactionContext, CATALOG_ID))
        .thenReturn(Optional.of(testCatalog));
    when(dataSourceQueryService.listByCatalogName(transactionContext, CATALOG_NAME))
        .thenReturn(Collections.emptyList());

    boolean result = catalogUseCase.deleteCatalogById(USER_ID, CATALOG_ID, false);

    assertThat(result).isTrue();
    verify(authorizationService).authorize(eq(USER_ID), any());
    verify(aceRepository).deleteByResourceId(transactionContext, CATALOG_ID);
    verify(catalogRepository).deleteByName(transactionContext, CATALOG_NAME);
  }

  @Test
  void deleteCatalogById_whenAccessDenied_shouldThrowException() throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(catalogRepository.findById(transactionContext, CATALOG_ID))
        .thenReturn(Optional.of(testCatalog));
    when(authorizationService.authorize(eq(USER_ID), any())).thenReturn(false);

    assertThatThrownBy(() -> catalogUseCase.deleteCatalogById(USER_ID, CATALOG_ID, false))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void deleteCatalogById_whenNotFound_shouldReturnFalse() throws Throwable {
    when(transactionManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              ThrowableFunction<RepositoryTransactionContext, Boolean, Exception> function =
                  invocation.getArgument(0);
              return function.apply(transactionContext);
            });
    when(catalogRepository.findById(transactionContext, CATALOG_ID)).thenReturn(Optional.empty());

    boolean result = catalogUseCase.deleteCatalogById(USER_ID, CATALOG_ID, false);

    assertThat(result).isFalse();
    verify(catalogRepository, never()).deleteByName(any(), any());
  }

  // RepositoryException wrapping

  @Test
  void findCatalog_whenRepositoryThrowsException_shouldPropagateAnalyticsException()
      throws Throwable {
    AnalyticsException analyticsException =
        new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
    when(catalogRepository.findByName(any(), eq(CATALOG_NAME))).thenThrow(analyticsException);

    assertThatThrownBy(() -> catalogUseCase.findCatalog(USER_ID, CATALOG_NAME))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(analyticsException);
  }

  @Test
  void deleteCatalog_whenRepositoryThrowsException_shouldPropagateAnalyticsException()
      throws Throwable {
    AnalyticsException analyticsException =
        new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
    when(transactionManager.withTransaction(any())).thenThrow(analyticsException);

    assertThatThrownBy(() -> catalogUseCase.deleteCatalog(USER_ID, CATALOG_NAME, false))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(analyticsException);
  }
}
