/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceRef;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.lib.functional.ThrowableFunction;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
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
class AuthorizationServiceImplDelegationTest {

  @Mock private RoleRepository<RepositoryTransactionContext> roleRepository;
  @Mock private RoleAssignmentRepository<RepositoryTransactionContext> roleAssignmentRepository;
  @Mock private AccessControlEntryRepository<RepositoryTransactionContext> aceRepository;
  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> txManager;
  @Mock private ScalarDbAuthorizationDelegate scalarDbDelegate;

  private AuthorizationServiceImpl<RepositoryTransactionContext> service;

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID TABLE_ID = UUID.randomUUID();
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final String SCALARDB_PROVIDER = "scalardb";

  private static final Role SUPERADMIN_ROLE =
      new Role(UUID.randomUUID(), AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME, true);

  @BeforeEach
  void setUp() {
    service =
        new AuthorizationServiceImpl<>(
            roleRepository, roleAssignmentRepository, aceRepository, txManager, scalarDbDelegate);
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

  private void stubNonSuperAdmin() throws Exception {
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());
  }

  private void stubNoAces() throws Exception {
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of());
  }

  private void stubDirectAce(UUID resourceId, Permission permission) throws Exception {
    AccessControlEntry ace =
        new AccessControlEntry(GranteeType.USER, USER_ID, resourceId, permission);
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of(ace));
  }

  // --- ScalarDB delegation (per-resource routing) ---

  @Test
  void authorize_withScalarDbResource_andDelegationPermits_shouldReturnTrue() throws Exception {
    stubTransactionPassthrough();
    stubNonSuperAdmin();
    when(scalarDbDelegate.authorize(eq(USER_ID), any())).thenReturn(true);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.TABLE, TABLE_ID, SCALARDB_PROVIDER),
                Set.of(Permission.TABLE_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isTrue();
  }

  @Test
  void authorize_withScalarDbResourceOnly_andDelegationDenies_shouldReturnFalse() throws Exception {
    stubTransactionPassthrough();
    stubNonSuperAdmin();
    when(scalarDbDelegate.authorize(eq(USER_ID), any())).thenReturn(false);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.TABLE, TABLE_ID, SCALARDB_PROVIDER),
                Set.of(Permission.TABLE_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isFalse();
  }

  // --- Non-ScalarDB resource uses ACL only ---

  @Test
  void authorize_withNonScalarDbResource_shouldUseAclOnly() throws Exception {
    stubTransactionPassthrough();
    stubNonSuperAdmin();
    stubDirectAce(CATALOG_ID, Permission.CATALOG_READ);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(Permission.CATALOG_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isTrue();
    verify(scalarDbDelegate, never()).authorize(any(), any());
  }

  @Test
  void authorize_withNonScalarDbResource_andAclDenied_shouldReturnFalse() throws Exception {
    stubTransactionPassthrough();
    stubNonSuperAdmin();
    stubNoAces();

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(Permission.CATALOG_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isFalse();
  }

  // --- Mixed requirements: OR semantics across delegation and ACL ---

  @Test
  void authorize_withMixedRequirements_delegationPermits_shouldReturnTrue() throws Exception {
    stubTransactionPassthrough();
    stubNonSuperAdmin();
    // Delegation permits — ACL is not needed (OR semantics)
    when(scalarDbDelegate.authorize(eq(USER_ID), any())).thenReturn(true);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.TABLE, TABLE_ID, SCALARDB_PROVIDER),
                Set.of(Permission.TABLE_READ)),
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(Permission.CATALOG_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isTrue();
  }

  @Test
  void authorize_withMixedRequirements_delegationDenies_aclPermits_shouldReturnTrue()
      throws Exception {
    stubTransactionPassthrough();
    stubNonSuperAdmin();
    stubDirectAce(CATALOG_ID, Permission.CATALOG_READ);
    when(scalarDbDelegate.authorize(eq(USER_ID), any())).thenReturn(false);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.TABLE, TABLE_ID, SCALARDB_PROVIDER),
                Set.of(Permission.TABLE_READ)),
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(Permission.CATALOG_READ)));

    // Delegation denies TABLE_READ, but ACL permits CATALOG_READ — allow
    assertThat(service.authorize(USER_ID, requirements)).isTrue();
  }

  @Test
  void authorize_withMixedRequirements_bothDeny_shouldReturnFalse() throws Exception {
    stubTransactionPassthrough();
    stubNonSuperAdmin();
    stubNoAces();
    when(scalarDbDelegate.authorize(eq(USER_ID), any())).thenReturn(false);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.TABLE, TABLE_ID, SCALARDB_PROVIDER),
                Set.of(Permission.TABLE_READ)),
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(Permission.CATALOG_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isFalse();
  }

  // --- SUPERADMIN bypasses all ---

  @Test
  void authorize_withSuperAdmin_shouldBypassAll() throws Exception {
    stubTransactionPassthrough();
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(new RoleAssignment(USER_ID, SUPERADMIN_ROLE.id())));
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.of(SUPERADMIN_ROLE));

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.TABLE, TABLE_ID, SCALARDB_PROVIDER),
                Set.of(Permission.TABLE_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isTrue();
    verify(scalarDbDelegate, never()).authorize(any(), any());
  }

  // --- Exception propagation ---

  @Test
  void authorize_withDelegateThrowingException_shouldPropagate() throws Exception {
    stubTransactionPassthrough();
    stubNonSuperAdmin();

    AnalyticsException ex =
        new AnalyticsException(
            AnalyticsErrorCode.SCALARDB_PRIVILEGE_CHECK_FAILURE, new RuntimeException());
    when(scalarDbDelegate.authorize(eq(USER_ID), any())).thenThrow(ex);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.TABLE, TABLE_ID, SCALARDB_PROVIDER),
                Set.of(Permission.TABLE_READ)));

    assertThatThrownBy(() -> service.authorize(USER_ID, requirements))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(ex);
  }

  // --- Non-ScalarDB provider type should use ACL, not delegation ---

  @Test
  void authorize_withNonScalarDbProviderType_shouldUseAclNotDelegation() throws Exception {
    stubTransactionPassthrough();
    stubNonSuperAdmin();
    stubDirectAce(TABLE_ID, Permission.TABLE_READ);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.TABLE, TABLE_ID, "mysql"),
                Set.of(Permission.TABLE_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isTrue();
    verify(scalarDbDelegate, never()).authorize(any(), any());
  }

  // --- Null delegate fallback ---

  @Test
  void authorize_withNullDelegate_shouldUseAclForAllResources() throws Exception {
    service =
        new AuthorizationServiceImpl<>(
            roleRepository, roleAssignmentRepository, aceRepository, txManager);

    stubTransactionPassthrough();
    stubNonSuperAdmin();
    stubDirectAce(CATALOG_ID, Permission.CATALOG_READ);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(Permission.CATALOG_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isTrue();
  }
}
