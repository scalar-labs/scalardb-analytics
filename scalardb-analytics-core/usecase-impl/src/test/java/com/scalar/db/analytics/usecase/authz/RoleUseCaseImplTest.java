/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import com.scalar.db.analytics.repository.RepositoryTransactionManager;
import com.scalar.db.analytics.repository.auth.AuthUserRepository;
import com.scalar.db.analytics.repository.authz.RoleAssignmentRepository;
import com.scalar.db.analytics.repository.authz.RoleRepository;
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
class RoleUseCaseImplTest {
  private static final UUID USER_ID = UUID.randomUUID();
  private static final UUID ROLE_ID = UUID.randomUUID();
  private static final UUID TARGET_USER_ID = UUID.randomUUID();

  @Mock private RoleRepository<RepositoryTransactionContext> roleRepository;
  @Mock private RoleAssignmentRepository<RepositoryTransactionContext> roleAssignmentRepository;

  @Mock private AuthUserRepository<RepositoryTransactionContext> authUserRepository;

  @Mock private RepositoryTransactionManager<RepositoryTransactionContext> txManager;
  @Mock private RepositoryTransactionContext txContext;
  @Mock private AuthorizationService authorizationService;

  private RoleUseCaseImpl<RepositoryTransactionContext> roleUseCase;

  @BeforeEach
  void setUp() throws Exception {
    roleUseCase =
        new RoleUseCaseImpl<>(
            roleRepository,
            roleAssignmentRepository,
            authUserRepository,
            txManager,
            authorizationService);

    // Default: SUPERADMIN authorized
    lenient().when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(true);
    lenient().when(txManager.single()).thenReturn(txContext);
    lenient()
        .when(txManager.withTransaction(any()))
        .thenAnswer(
            inv -> {
              var fn =
                  (com.scalar.db.analytics.lib.functional.ThrowableFunction<
                          RepositoryTransactionContext, ?, ?>)
                      inv.getArgument(0);
              return fn.apply(txContext);
            });
  }

  @Test
  void createRole_shouldCreateAndReturnRole() throws Exception {
    Role role = roleUseCase.createRole(USER_ID, "analyst");

    assertThat(role.name()).isEqualTo("analyst");
    assertThat(role.builtIn()).isFalse();
    verify(roleRepository).create(eq(txContext), any(Role.class));
  }

  @Test
  void createRole_shouldThrowAccessDeniedWhenNotSuperAdmin() {
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> roleUseCase.createRole(USER_ID, "analyst"))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void deleteRoleById_shouldDeleteRoleAndAssignments() throws Exception {
    Role role = new Role(ROLE_ID, "analyst", false);
    when(roleRepository.findById(txContext, ROLE_ID)).thenReturn(Optional.of(role));

    boolean deleted = roleUseCase.deleteRoleById(USER_ID, ROLE_ID);

    assertThat(deleted).isTrue();
    verify(roleAssignmentRepository).deleteByRoleId(txContext, ROLE_ID);
    verify(roleRepository).deleteById(txContext, ROLE_ID);
  }

  @Test
  void deleteRoleById_shouldReturnFalseWhenNotFound() throws Exception {
    when(roleRepository.findById(txContext, ROLE_ID)).thenReturn(Optional.empty());

    boolean deleted = roleUseCase.deleteRoleById(USER_ID, ROLE_ID);

    assertThat(deleted).isFalse();
  }

  @Test
  void deleteRoleById_shouldThrowWhenBuiltInRole() throws Exception {
    Role builtInRole = new Role(ROLE_ID, "SUPERADMIN", true);
    when(roleRepository.findById(txContext, ROLE_ID)).thenReturn(Optional.of(builtInRole));

    assertThatThrownBy(() -> roleUseCase.deleteRoleById(USER_ID, ROLE_ID))
        .isInstanceOf(AnalyticsException.class);
  }

  @Test
  void deleteRole_shouldDeleteByName() throws Exception {
    Role role = new Role(ROLE_ID, "analyst", false);
    when(roleRepository.findByName(txContext, "analyst")).thenReturn(Optional.of(role));

    boolean deleted = roleUseCase.deleteRole(USER_ID, "analyst");

    assertThat(deleted).isTrue();
    verify(roleAssignmentRepository).deleteByRoleId(txContext, ROLE_ID);
    verify(roleRepository).deleteById(txContext, ROLE_ID);
  }

  @Test
  void deleteRole_shouldReturnFalseWhenNameNotFound() throws Exception {
    when(roleRepository.findByName(txContext, "nonexistent")).thenReturn(Optional.empty());

    boolean deleted = roleUseCase.deleteRole(USER_ID, "nonexistent");

    assertThat(deleted).isFalse();
  }

  @Test
  void grantRole_shouldResolveNamesAndCreateAssignment() throws Exception {
    Role role = new Role(ROLE_ID, "editor", false);
    AuthUser authUser = new AuthUser(TARGET_USER_ID, "alice");
    when(roleRepository.findByName(txContext, "editor")).thenReturn(Optional.of(role));
    when(authUserRepository.findByUsername(txContext, "alice")).thenReturn(Optional.of(authUser));

    roleUseCase.grantRole(USER_ID, "editor", "alice");

    verify(roleAssignmentRepository)
        .create(eq(txContext), eq(new RoleAssignment(TARGET_USER_ID, ROLE_ID)));
  }

  @Test
  void grantRole_shouldThrowWhenRoleNameNotFound() throws Exception {
    when(roleRepository.findByName(txContext, "nonexistent")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleUseCase.grantRole(USER_ID, "nonexistent", "alice"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Role not found");
  }

  @Test
  void grantRole_shouldThrowWhenUsernameNotFound() throws Exception {
    Role role = new Role(ROLE_ID, "editor", false);
    when(roleRepository.findByName(txContext, "editor")).thenReturn(Optional.of(role));
    when(authUserRepository.findByUsername(txContext, "nonexistent")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleUseCase.grantRole(USER_ID, "editor", "nonexistent"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("User not found");
  }

  @Test
  void revokeRole_shouldResolveNamesAndDeleteAssignment() throws Exception {
    Role role = new Role(ROLE_ID, "editor", false);
    AuthUser authUser = new AuthUser(TARGET_USER_ID, "alice");
    when(roleRepository.findByName(txContext, "editor")).thenReturn(Optional.of(role));
    when(authUserRepository.findByUsername(txContext, "alice")).thenReturn(Optional.of(authUser));

    roleUseCase.revokeRole(USER_ID, "editor", "alice");

    verify(roleAssignmentRepository).delete(txContext, TARGET_USER_ID, ROLE_ID);
  }

  @Test
  void listRoles_shouldReturnAllRoles() throws Exception {
    List<Role> roles = List.of(new Role(ROLE_ID, "analyst", false));
    when(roleRepository.findAll(txContext)).thenReturn(roles);

    List<Role> result = roleUseCase.listRoles(USER_ID);

    assertThat(result).isEqualTo(roles);
  }

  @Test
  void grantRoleById_shouldCreateAssignment() throws Exception {
    Role role = new Role(ROLE_ID, "analyst", false);
    when(roleRepository.findById(txContext, ROLE_ID)).thenReturn(Optional.of(role));

    roleUseCase.grantRoleById(USER_ID, ROLE_ID, TARGET_USER_ID);

    verify(roleAssignmentRepository)
        .create(eq(txContext), eq(new RoleAssignment(TARGET_USER_ID, ROLE_ID)));
  }

  @Test
  void grantRoleById_shouldThrowWhenRoleNotFound() throws Exception {
    when(roleRepository.findById(txContext, ROLE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleUseCase.grantRoleById(USER_ID, ROLE_ID, TARGET_USER_ID))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void revokeRoleById_shouldDeleteAssignment() throws Exception {
    Role role = new Role(ROLE_ID, "analyst", false);
    when(roleRepository.findById(txContext, ROLE_ID)).thenReturn(Optional.of(role));

    roleUseCase.revokeRoleById(USER_ID, ROLE_ID, TARGET_USER_ID);

    verify(roleAssignmentRepository).delete(txContext, TARGET_USER_ID, ROLE_ID);
  }

  @Test
  void revokeRoleById_shouldThrowWhenRoleNotFound() throws Exception {
    when(roleRepository.findById(txContext, ROLE_ID)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> roleUseCase.revokeRoleById(USER_ID, ROLE_ID, TARGET_USER_ID))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void allOperations_shouldThrowAccessDeniedWhenNotSuperAdmin() {
    when(authorizationService.authorizeSuperAdmin(USER_ID)).thenReturn(false);

    assertThatThrownBy(() -> roleUseCase.createRole(USER_ID, "x"))
        .isInstanceOf(AnalyticsException.class);
    assertThatThrownBy(() -> roleUseCase.deleteRole(USER_ID, "x"))
        .isInstanceOf(AnalyticsException.class);
    assertThatThrownBy(() -> roleUseCase.deleteRoleById(USER_ID, ROLE_ID))
        .isInstanceOf(AnalyticsException.class);
    assertThatThrownBy(() -> roleUseCase.listRoles(USER_ID)).isInstanceOf(AnalyticsException.class);
    assertThatThrownBy(() -> roleUseCase.grantRole(USER_ID, "x", "y"))
        .isInstanceOf(AnalyticsException.class);
    assertThatThrownBy(() -> roleUseCase.grantRoleById(USER_ID, ROLE_ID, TARGET_USER_ID))
        .isInstanceOf(AnalyticsException.class);
    assertThatThrownBy(() -> roleUseCase.revokeRole(USER_ID, "x", "y"))
        .isInstanceOf(AnalyticsException.class);
    assertThatThrownBy(() -> roleUseCase.revokeRoleById(USER_ID, ROLE_ID, TARGET_USER_ID))
        .isInstanceOf(AnalyticsException.class);
  }
}
