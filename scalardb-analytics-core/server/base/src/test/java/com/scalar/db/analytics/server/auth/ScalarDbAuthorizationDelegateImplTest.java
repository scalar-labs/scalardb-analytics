/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.api.model.Table;
import com.scalar.db.analytics.api.model.TableInfo;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.domain.auth.PasswordIdentity;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import com.scalar.db.analytics.repository.NamespaceRepository;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.TableRepository;
import com.scalar.db.analytics.repository.auth.PasswordIdentityRepository;
import com.scalar.db.analytics.service.authz.PermissionRequirement;
import com.scalar.db.analytics.service.authz.ScalarDbPrivilegeClient;
import com.scalar.db.analytics.usecase.auth.BackendAuthResult;
import com.scalar.db.analytics.usecase.auth.BackendTokenStore;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ScalarDbAuthorizationDelegateImplTest {

  @Mock private NamespaceRepository<RepositoryTransactionContext> namespaceRepository;
  @Mock private TableRepository<RepositoryTransactionContext> tableRepository;
  @Mock private PasswordIdentityRepository<RepositoryTransactionContext> passwordIdentityRepository;
  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> txManager;
  @Mock private ScalarDbPrivilegeClient privilegeClient;
  @Mock private BackendTokenStore backendTokenStore;

  private ScalarDbAuthorizationDelegateImpl<RepositoryTransactionContext> delegate;

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID DATA_SOURCE_ID = UUID.randomUUID();
  private static final UUID NAMESPACE_ID = UUID.randomUUID();
  private static final UUID TABLE_ID = UUID.randomUUID();
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final String SCALARDB_USERNAME = "alice";
  private static final String NAMESPACE_NAME = "my_namespace";
  private static final String TABLE_NAME = "my_table";
  private static final String BACKEND_TOKEN = "backend-auth-token";

  private static final PasswordIdentity SCALARDB_IDENTITY =
      new PasswordIdentity(
          UUID.randomUUID(), USER_ID, PasswordBackendType.SCALARDB_CLUSTER, SCALARDB_USERNAME);
  private static final PasswordIdentity INTERNAL_IDENTITY =
      new PasswordIdentity(
          UUID.randomUUID(), USER_ID, PasswordBackendType.INTERNAL, "internal-user");

  private static final Namespace NAMESPACE =
      new Namespace(NAMESPACE_ID, DATA_SOURCE_ID, List.of(NAMESPACE_NAME));
  private static final Table TABLE = new Table(new TableInfo(TABLE_ID, NAMESPACE_ID, TABLE_NAME));

  @BeforeEach
  void setUp() {
    delegate =
        new ScalarDbAuthorizationDelegateImpl<>(
            namespaceRepository,
            tableRepository,
            passwordIdentityRepository,
            txManager,
            privilegeClient,
            backendTokenStore);
  }

  @SuppressWarnings("unchecked")
  private void stubTransactionPassthrough() throws Exception {
    when(txManager.withTransaction(any()))
        .thenAnswer(
            invocation -> {
              var function =
                  (ThrowableFunction<RepositoryTransactionContext, Object, Throwable>)
                      invocation.getArgument(0);
              RepositoryTransactionContext ctx = mock(RepositoryTransactionContext.class);
              return function.apply(ctx);
            });
  }

  private static ResourceRef scalarDbNamespaceRef() {
    return new ResourceRef(ResourceType.NAMESPACE, NAMESPACE_ID, ScalarDbProvider.TYPE);
  }

  private static ResourceRef scalarDbTableRef() {
    return new ResourceRef(ResourceType.TABLE, TABLE_ID, ScalarDbProvider.TYPE);
  }

  private static ResourceRef mysqlNamespaceRef() {
    return new ResourceRef(ResourceType.NAMESPACE, NAMESPACE_ID, "mysql");
  }

  // --- Namespace permission checks ---

  @Test
  void authorize_withScalarDbNamespace_andPrivilegeGranted_shouldReturnTrue() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(SCALARDB_IDENTITY));
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID))).thenReturn(Optional.of(NAMESPACE));
    when(backendTokenStore.get(USER_ID))
        .thenReturn(Optional.of(new BackendAuthResult(SCALARDB_USERNAME, BACKEND_TOKEN)));
    when(privilegeClient.hasSelectPrivilege(BACKEND_TOKEN, SCALARDB_USERNAME, NAMESPACE_NAME, null))
        .thenReturn(true);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(scalarDbNamespaceRef(), Set.of(Permission.NAMESPACE_READ)));

    assertThat(delegate.authorize(USER_ID, requirements)).isTrue();
  }

  @Test
  void authorize_withScalarDbNamespace_andPrivilegeDenied_shouldReturnFalse() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(SCALARDB_IDENTITY));
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID))).thenReturn(Optional.of(NAMESPACE));
    when(backendTokenStore.get(USER_ID))
        .thenReturn(Optional.of(new BackendAuthResult(SCALARDB_USERNAME, BACKEND_TOKEN)));
    when(privilegeClient.hasSelectPrivilege(BACKEND_TOKEN, SCALARDB_USERNAME, NAMESPACE_NAME, null))
        .thenReturn(false);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(scalarDbNamespaceRef(), Set.of(Permission.NAMESPACE_READ)));

    assertThat(delegate.authorize(USER_ID, requirements)).isFalse();
  }

  // --- Table permission checks ---

  @Test
  void authorize_withScalarDbTable_andPrivilegeGranted_shouldReturnTrue() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(SCALARDB_IDENTITY));
    when(tableRepository.findById(any(), eq(TABLE_ID))).thenReturn(Optional.of(TABLE));
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID))).thenReturn(Optional.of(NAMESPACE));
    when(backendTokenStore.get(USER_ID))
        .thenReturn(Optional.of(new BackendAuthResult(SCALARDB_USERNAME, BACKEND_TOKEN)));
    when(privilegeClient.hasSelectPrivilege(
            BACKEND_TOKEN, SCALARDB_USERNAME, NAMESPACE_NAME, TABLE_NAME))
        .thenReturn(true);

    List<PermissionRequirement> requirements =
        List.of(new PermissionRequirement(scalarDbTableRef(), Set.of(Permission.TABLE_READ)));

    assertThat(delegate.authorize(USER_ID, requirements)).isTrue();
  }

  @Test
  void authorize_withScalarDbTable_andPrivilegeDenied_shouldReturnFalse() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(SCALARDB_IDENTITY));
    when(tableRepository.findById(any(), eq(TABLE_ID))).thenReturn(Optional.of(TABLE));
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID))).thenReturn(Optional.of(NAMESPACE));
    when(backendTokenStore.get(USER_ID))
        .thenReturn(Optional.of(new BackendAuthResult(SCALARDB_USERNAME, BACKEND_TOKEN)));
    when(privilegeClient.hasSelectPrivilege(
            BACKEND_TOKEN, SCALARDB_USERNAME, NAMESPACE_NAME, TABLE_NAME))
        .thenReturn(false);

    List<PermissionRequirement> requirements =
        List.of(new PermissionRequirement(scalarDbTableRef(), Set.of(Permission.TABLE_READ)));

    assertThat(delegate.authorize(USER_ID, requirements)).isFalse();
  }

  // --- Skip scenarios ---

  @Test
  void authorize_withNoScalarDbIdentity_shouldDeny() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(INTERNAL_IDENTITY));

    List<PermissionRequirement> requirements =
        List.of(new PermissionRequirement(scalarDbTableRef(), Set.of(Permission.TABLE_READ)));

    assertThat(delegate.authorize(USER_ID, requirements)).isFalse();
    verify(privilegeClient, never()).hasSelectPrivilege(any(), any(), any(), any());
  }

  @Test
  void authorize_withNonScalarDbDataSource_shouldSkip() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(SCALARDB_IDENTITY));

    List<PermissionRequirement> requirements =
        List.of(new PermissionRequirement(mysqlNamespaceRef(), Set.of(Permission.NAMESPACE_READ)));

    assertThat(delegate.authorize(USER_ID, requirements)).isTrue();
    verify(privilegeClient, never()).hasSelectPrivilege(any(), any(), any(), any());
  }

  @Test
  void authorize_withCatalogResource_shouldSkip() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(SCALARDB_IDENTITY));

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(Permission.CATALOG_READ)));

    assertThat(delegate.authorize(USER_ID, requirements)).isTrue();
    verify(privilegeClient, never()).hasSelectPrivilege(any(), any(), any(), any());
  }

  @Test
  void authorize_withNullProviderType_shouldSkip() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(SCALARDB_IDENTITY));

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.NAMESPACE, NAMESPACE_ID),
                Set.of(Permission.NAMESPACE_READ)));

    assertThat(delegate.authorize(USER_ID, requirements)).isTrue();
    verify(privilegeClient, never()).hasSelectPrivilege(any(), any(), any(), any());
  }

  @Test
  void authorize_withMissingNamespace_shouldSkip() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(SCALARDB_IDENTITY));
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID))).thenReturn(Optional.empty());

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(scalarDbNamespaceRef(), Set.of(Permission.NAMESPACE_READ)));

    assertThat(delegate.authorize(USER_ID, requirements)).isTrue();
    verify(privilegeClient, never()).hasSelectPrivilege(any(), any(), any(), any());
  }

  @Test
  void authorize_withNoBackendToken_shouldThrowInvalidCredentials() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(SCALARDB_IDENTITY));
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID))).thenReturn(Optional.of(NAMESPACE));
    when(backendTokenStore.get(USER_ID)).thenReturn(Optional.empty());

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(scalarDbNamespaceRef(), Set.of(Permission.NAMESPACE_READ)));

    assertThatThrownBy(() -> delegate.authorize(USER_ID, requirements))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.SCALARDB_BACKEND_TOKEN_EXPIRED);
            });
    verify(privilegeClient, never()).hasSelectPrivilege(any(), any(), any(), any());
  }

  // --- Error scenario ---

  @Test
  void authorize_withClientError_shouldPropagateException() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(SCALARDB_IDENTITY));
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID))).thenReturn(Optional.of(NAMESPACE));
    when(backendTokenStore.get(USER_ID))
        .thenReturn(Optional.of(new BackendAuthResult(SCALARDB_USERNAME, BACKEND_TOKEN)));
    when(privilegeClient.hasSelectPrivilege(BACKEND_TOKEN, SCALARDB_USERNAME, NAMESPACE_NAME, null))
        .thenThrow(
            new AnalyticsException(
                AnalyticsErrorCode.SCALARDB_PRIVILEGE_CHECK_FAILURE, new RuntimeException()));

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(scalarDbNamespaceRef(), Set.of(Permission.NAMESPACE_READ)));

    assertThatThrownBy(() -> delegate.authorize(USER_ID, requirements))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.SCALARDB_PRIVILEGE_CHECK_FAILURE);
            });
  }

  @Test
  void authorize_withExpiredBackendToken_shouldThrowInvalidCredentials() throws Exception {
    stubTransactionPassthrough();
    when(passwordIdentityRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(SCALARDB_IDENTITY));
    when(namespaceRepository.findById(any(), eq(NAMESPACE_ID))).thenReturn(Optional.of(NAMESPACE));
    when(backendTokenStore.get(USER_ID))
        .thenReturn(Optional.of(new BackendAuthResult(SCALARDB_USERNAME, BACKEND_TOKEN)));
    when(privilegeClient.hasSelectPrivilege(BACKEND_TOKEN, SCALARDB_USERNAME, NAMESPACE_NAME, null))
        .thenThrow(
            new AnalyticsException(
                AnalyticsErrorCode.SCALARDB_BACKEND_TOKEN_EXPIRED,
                new StatusRuntimeException(Status.UNAUTHENTICATED)));

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(scalarDbNamespaceRef(), Set.of(Permission.NAMESPACE_READ)));

    assertThatThrownBy(() -> delegate.authorize(USER_ID, requirements))
        .isInstanceOf(AnalyticsException.class)
        .satisfies(
            e -> {
              AnalyticsException ae = (AnalyticsException) e;
              assertThat(ae.getErrorCode())
                  .isEqualTo(AnalyticsErrorCode.SCALARDB_BACKEND_TOKEN_EXPIRED);
            });

    // Verify expired token was removed from store
    verify(backendTokenStore).remove(USER_ID);
  }
}
