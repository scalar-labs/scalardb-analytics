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
class AuthorizationServiceImplTest {

  @Mock private RoleRepository<RepositoryTransactionContext> roleRepository;
  @Mock private RoleAssignmentRepository<RepositoryTransactionContext> roleAssignmentRepository;
  @Mock private AccessControlEntryRepository<RepositoryTransactionContext> aceRepository;
  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> txManager;

  private AuthorizationServiceImpl<RepositoryTransactionContext> service;

  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID CATALOG_ID = UUID.randomUUID();
  private static final UUID DATA_SOURCE_ID = UUID.randomUUID();
  private static final UUID TABLE_ID = UUID.randomUUID();
  private static final UUID ROLE_ID = UUID.randomUUID();

  private static final Role SUPERADMIN_ROLE =
      new Role(UUID.randomUUID(), AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME, true);
  private static final Role REGULAR_ROLE = new Role(ROLE_ID, "analyst", false);

  @BeforeEach
  void setUp() {
    service =
        new AuthorizationServiceImpl<>(
            roleRepository, roleAssignmentRepository, aceRepository, txManager);
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

  // --- authorizeSuperAdmin ---

  @Test
  void authorizeSuperAdmin_withSuperAdminRole_shouldPass() throws Exception {
    stubTransactionPassthrough();
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(new RoleAssignment(USER_ID, SUPERADMIN_ROLE.id())));
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.of(SUPERADMIN_ROLE));

    assertThat(service.authorizeSuperAdmin(USER_ID)).isTrue();
  }

  @Test
  void authorizeSuperAdmin_withoutSuperAdminRole_shouldReturnFalse() throws Exception {
    stubTransactionPassthrough();
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());

    assertThat(service.authorizeSuperAdmin(USER_ID)).isFalse();
  }

  @Test
  void authorizeSuperAdmin_withRegularRoleOnly_shouldReturnFalse() throws Exception {
    stubTransactionPassthrough();
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(new RoleAssignment(USER_ID, REGULAR_ROLE.id())));
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.of(SUPERADMIN_ROLE));

    assertThat(service.authorizeSuperAdmin(USER_ID)).isFalse();
  }

  // --- authorize: direct user ACE ---

  @Test
  void authorize_withDirectUserAce_shouldPass() throws Exception {
    stubTransactionPassthrough();

    // Not a superadmin
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());

    // Direct ACE on catalog
    AccessControlEntry ace =
        new AccessControlEntry(GranteeType.USER, USER_ID, CATALOG_ID, Permission.CATALOG_READ);
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of(ace));

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(
                    Permission.CATALOG_READ, Permission.CATALOG_WRITE, Permission.CATALOG_ADMIN)));

    assertThat(service.authorize(USER_ID, requirements)).isTrue();
  }

  // --- authorize: role-based ACE ---

  @Test
  void authorize_withRoleBasedAce_shouldPass() throws Exception {
    stubTransactionPassthrough();

    // User has a role
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(new RoleAssignment(USER_ID, ROLE_ID)));
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.of(SUPERADMIN_ROLE));

    // No direct ACEs
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of());

    // Role has ACE on catalog
    AccessControlEntry roleAce =
        new AccessControlEntry(GranteeType.ROLE, ROLE_ID, CATALOG_ID, Permission.CATALOG_READ);
    when(aceRepository.findByGrantee(any(), eq(GranteeType.ROLE), eq(ROLE_ID)))
        .thenReturn(List.of(roleAce));

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(Permission.CATALOG_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isTrue();
  }

  // --- authorize: hierarchy traversal ---

  @Test
  void authorize_withAceOnParentCatalog_shouldPassForChildTable() throws Exception {
    stubTransactionPassthrough();

    // Not a superadmin
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());

    // Direct ACE on catalog (parent resource)
    AccessControlEntry ace =
        new AccessControlEntry(GranteeType.USER, USER_ID, CATALOG_ID, Permission.CATALOG_READ);
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of(ace));

    // DescribeTable requirements: multiple hierarchy levels (OR semantics)
    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.TABLE, TABLE_ID), Set.of(Permission.TABLE_READ)),
            new PermissionRequirement(
                new ResourceRef(ResourceType.DATA_SOURCE, DATA_SOURCE_ID),
                Set.of(Permission.DATA_SOURCE_READ, Permission.DATA_SOURCE_ADMIN)),
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(
                    Permission.CATALOG_READ, Permission.CATALOG_WRITE, Permission.CATALOG_ADMIN)));

    // ACE on CATALOG_ID with CATALOG_READ matches the third requirement
    assertThat(service.authorize(USER_ID, requirements)).isTrue();
  }

  // --- authorize: SUPERADMIN bypass ---

  @Test
  void authorize_withSuperAdminRole_shouldBypassAceCheck() throws Exception {
    stubTransactionPassthrough();

    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(new RoleAssignment(USER_ID, SUPERADMIN_ROLE.id())));
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.of(SUPERADMIN_ROLE));

    // No ACEs set up — SUPERADMIN bypasses
    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.TABLE, TABLE_ID), Set.of(Permission.TABLE_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isTrue();
  }

  // --- authorize: denial ---

  @Test
  void authorize_withNoMatchingAce_shouldReturnFalse() throws Exception {
    stubTransactionPassthrough();

    // Not a superadmin
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());

    // No ACEs at all
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of());

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(Permission.CATALOG_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isFalse();
  }

  @Test
  void authorize_withAceOnWrongResource_shouldReturnFalse() throws Exception {
    stubTransactionPassthrough();

    // Not a superadmin
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());

    // ACE exists but on a different catalog
    UUID otherCatalogId = UUID.randomUUID();
    AccessControlEntry ace =
        new AccessControlEntry(GranteeType.USER, USER_ID, otherCatalogId, Permission.CATALOG_READ);
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of(ace));

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(Permission.CATALOG_READ)));

    assertThat(service.authorize(USER_ID, requirements)).isFalse();
  }

  // --- authorize: empty requirements ---

  @Test
  void authorize_withEmptyRequirements_shouldReturnTrue() {
    // No txManager interaction expected
    assertThat(service.authorize(USER_ID, List.of())).isTrue();
  }

  // --- authorizePermissionManagement ---

  @Test
  void authorizePermissionManagement_withSuperAdmin_shouldPass() throws Exception {
    stubTransactionPassthrough();
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(new RoleAssignment(USER_ID, SUPERADMIN_ROLE.id())));
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.of(SUPERADMIN_ROLE));

    assertThat(service.authorizePermissionManagement(USER_ID, CATALOG_ID)).isTrue();
  }

  @Test
  void authorizePermissionManagement_withCatalogAdmin_shouldPass() throws Exception {
    stubTransactionPassthrough();

    // Not a superadmin
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());

    // Direct CATALOG_ADMIN ACE on the target catalog
    AccessControlEntry ace =
        new AccessControlEntry(GranteeType.USER, USER_ID, CATALOG_ID, Permission.CATALOG_ADMIN);
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of(ace));

    assertThat(service.authorizePermissionManagement(USER_ID, CATALOG_ID)).isTrue();
  }

  @Test
  void authorizePermissionManagement_withCatalogAdminViaRole_shouldPass() throws Exception {
    stubTransactionPassthrough();

    // User has a regular role (not superadmin)
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(new RoleAssignment(USER_ID, ROLE_ID)));
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.of(SUPERADMIN_ROLE));

    // No direct ACEs
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of());

    // Role has CATALOG_ADMIN ACE on the target catalog
    AccessControlEntry roleAce =
        new AccessControlEntry(GranteeType.ROLE, ROLE_ID, CATALOG_ID, Permission.CATALOG_ADMIN);
    when(aceRepository.findByGrantee(any(), eq(GranteeType.ROLE), eq(ROLE_ID)))
        .thenReturn(List.of(roleAce));

    assertThat(service.authorizePermissionManagement(USER_ID, CATALOG_ID)).isTrue();
  }

  @Test
  void authorizePermissionManagement_withoutPermission_shouldReturnFalse() throws Exception {
    stubTransactionPassthrough();

    // Not a superadmin, no ACEs
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of());

    assertThat(service.authorizePermissionManagement(USER_ID, CATALOG_ID)).isFalse();
  }

  @Test
  void authorizePermissionManagement_withCatalogAdminOnDifferentCatalog_shouldReturnFalse()
      throws Exception {
    stubTransactionPassthrough();

    // Not a superadmin
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());

    // CATALOG_ADMIN ACE on a different catalog
    UUID otherCatalogId = UUID.randomUUID();
    AccessControlEntry ace =
        new AccessControlEntry(GranteeType.USER, USER_ID, otherCatalogId, Permission.CATALOG_ADMIN);
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of(ace));

    assertThat(service.authorizePermissionManagement(USER_ID, CATALOG_ID)).isFalse();
  }

  @Test
  void authorizePermissionManagement_withCatalogReadOnly_shouldReturnFalse() throws Exception {
    stubTransactionPassthrough();

    // Not a superadmin
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());

    // CATALOG_READ on the target catalog (not CATALOG_ADMIN)
    AccessControlEntry ace =
        new AccessControlEntry(GranteeType.USER, USER_ID, CATALOG_ID, Permission.CATALOG_READ);
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of(ace));

    assertThat(service.authorizePermissionManagement(USER_ID, CATALOG_ID)).isFalse();
  }

  // --- authorize: repository exception wrapping ---

  @Test
  @SuppressWarnings("unchecked")
  void authorize_withAnalyticsException_shouldPropagate() throws Exception {
    AnalyticsException analyticsException =
        new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
    when(txManager.withTransaction(any())).thenThrow(analyticsException);

    List<PermissionRequirement> requirements =
        List.of(
            new PermissionRequirement(
                new ResourceRef(ResourceType.CATALOG, CATALOG_ID),
                Set.of(Permission.CATALOG_READ)));

    assertThatThrownBy(() -> service.authorize(USER_ID, requirements))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(analyticsException);
  }

  @Test
  @SuppressWarnings("unchecked")
  void authorizePermissionManagement_withAnalyticsException_shouldPropagate() throws Exception {
    AnalyticsException analyticsException =
        new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
    when(txManager.withTransaction(any())).thenThrow(analyticsException);

    assertThatThrownBy(() -> service.authorizePermissionManagement(USER_ID, CATALOG_ID))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(analyticsException);
  }

  // --- filterAuthorized ---

  @Test
  void filterAuthorized_withEmptyList_shouldReturnEmpty() {
    List<UUID> result = service.filterAuthorized(USER_ID, List.of(), id -> List.of());

    assertThat(result).isEmpty();
  }

  @Test
  void filterAuthorized_withSuperAdmin_shouldReturnAllItems() throws Exception {
    stubTransactionPassthrough();
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID)))
        .thenReturn(List.of(new RoleAssignment(USER_ID, SUPERADMIN_ROLE.id())));
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.of(SUPERADMIN_ROLE));

    List<UUID> items = List.of(CATALOG_ID, DATA_SOURCE_ID, TABLE_ID);

    List<UUID> result =
        service.filterAuthorized(
            USER_ID,
            items,
            id ->
                List.of(
                    new PermissionRequirement(
                        new ResourceRef(ResourceType.CATALOG, id),
                        Set.of(Permission.CATALOG_READ))));

    assertThat(result).containsExactlyElementsOf(items);
  }

  @Test
  void filterAuthorized_withMatchingAces_shouldFilterCorrectly() throws Exception {
    stubTransactionPassthrough();

    // Not a superadmin
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());

    // ACE on CATALOG_ID only
    AccessControlEntry ace =
        new AccessControlEntry(GranteeType.USER, USER_ID, CATALOG_ID, Permission.CATALOG_READ);
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of(ace));

    UUID otherCatalogId = UUID.randomUUID();
    List<UUID> items = List.of(CATALOG_ID, otherCatalogId);

    List<UUID> result =
        service.filterAuthorized(
            USER_ID,
            items,
            id ->
                List.of(
                    new PermissionRequirement(
                        new ResourceRef(ResourceType.CATALOG, id),
                        Set.of(Permission.CATALOG_READ))));

    assertThat(result).containsExactly(CATALOG_ID);
  }

  @Test
  void filterAuthorized_withNoAces_shouldReturnEmpty() throws Exception {
    stubTransactionPassthrough();

    // Not a superadmin
    when(roleAssignmentRepository.findByUserId(any(), eq(USER_ID))).thenReturn(List.of());
    when(roleRepository.findByName(any(), eq(AuthorizationServiceImpl.SUPERADMIN_ROLE_NAME)))
        .thenReturn(Optional.empty());

    // No ACEs
    when(aceRepository.findByGrantee(any(), eq(GranteeType.USER), eq(USER_ID)))
        .thenReturn(List.of());

    List<UUID> items = List.of(CATALOG_ID, DATA_SOURCE_ID);

    List<UUID> result =
        service.filterAuthorized(
            USER_ID,
            items,
            id ->
                List.of(
                    new PermissionRequirement(
                        new ResourceRef(ResourceType.CATALOG, id),
                        Set.of(Permission.CATALOG_READ))));

    assertThat(result).isEmpty();
  }

  @Test
  @SuppressWarnings("unchecked")
  void filterAuthorized_withAnalyticsException_shouldPropagate() throws Exception {
    AnalyticsException analyticsException =
        new AnalyticsException(AnalyticsErrorCode.ANALYTICS_DB_OPERATION_FAILED);
    when(txManager.withTransaction(any())).thenThrow(analyticsException);

    List<UUID> items = List.of(CATALOG_ID);

    assertThatThrownBy(
            () ->
                service.filterAuthorized(
                    USER_ID,
                    items,
                    id ->
                        List.of(
                            new PermissionRequirement(
                                new ResourceRef(ResourceType.CATALOG, id),
                                Set.of(Permission.CATALOG_READ)))))
        .isInstanceOf(AnalyticsException.class)
        .isSameAs(analyticsException);
  }
}
