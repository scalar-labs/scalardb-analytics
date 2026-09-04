/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.auth.AuthUser;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.domain.authz.RoleAssignment;
import com.scalar.db.analytics.repository.impl.spring.repository.auth.AuthUserRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.authz.RoleAssignmentRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.repository.authz.RoleRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoleAssignmentRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  @Autowired private AuthUserRepositoryImpl authUserRepository;
  @Autowired private RoleRepositoryImpl roleRepository;
  @Autowired private RoleAssignmentRepositoryImpl roleAssignmentRepository;

  private AuthUser testUser;
  private Role testRole;

  @BeforeEach
  void setUp() {
    testUser = AuthUser.create("testuser");
    authUserRepository.create(ctx, testUser);
    testRole = Role.create("test-role");
    roleRepository.create(ctx, testRole);
  }

  @Test
  void createShouldPersistAssignment() {
    RoleAssignment assignment = new RoleAssignment(testUser.getUserId(), testRole.id());

    roleAssignmentRepository.create(ctx, assignment);

    var found = roleAssignmentRepository.findByUserId(ctx, testUser.getUserId());
    assertThat(found).hasSize(1);
    assertThat(found.get(0).userId()).isEqualTo(testUser.getUserId());
    assertThat(found.get(0).roleId()).isEqualTo(testRole.id());
  }

  @Test
  void createDuplicateAssignmentShouldThrowEntityAlreadyExists() {
    RoleAssignment assignment = new RoleAssignment(testUser.getUserId(), testRole.id());
    roleAssignmentRepository.create(ctx, assignment);

    RoleAssignment duplicate = new RoleAssignment(testUser.getUserId(), testRole.id());

    assertThatThrownBy(() -> roleAssignmentRepository.create(ctx, duplicate))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.ROLE_ALREADY_ASSIGNED);
  }

  @Test
  void findByUserIdShouldReturnAllAssignmentsForUser() {
    Role role2 = Role.create("role-2");
    roleRepository.create(ctx, role2);
    roleAssignmentRepository.create(ctx, new RoleAssignment(testUser.getUserId(), testRole.id()));
    roleAssignmentRepository.create(ctx, new RoleAssignment(testUser.getUserId(), role2.id()));

    var assignments = roleAssignmentRepository.findByUserId(ctx, testUser.getUserId());

    assertThat(assignments).hasSize(2);
    assertThat(assignments)
        .extracting(RoleAssignment::roleId)
        .containsExactlyInAnyOrder(testRole.id(), role2.id());
  }

  @Test
  void findByUserIdShouldReturnEmptyWhenNoAssignments() {
    assertThat(roleAssignmentRepository.findByUserId(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void deleteShouldRemoveSpecificAssignment() {
    roleAssignmentRepository.create(ctx, new RoleAssignment(testUser.getUserId(), testRole.id()));

    roleAssignmentRepository.delete(ctx, testUser.getUserId(), testRole.id());

    assertThat(roleAssignmentRepository.findByUserId(ctx, testUser.getUserId())).isEmpty();
  }

  @Test
  void deleteShouldNotThrowWhenMissing() {
    assertThatCode(() -> roleAssignmentRepository.delete(ctx, UUID.randomUUID(), UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void deleteByUserIdShouldRemoveAllAssignmentsForUser() {
    Role role2 = Role.create("role-3");
    roleRepository.create(ctx, role2);
    roleAssignmentRepository.create(ctx, new RoleAssignment(testUser.getUserId(), testRole.id()));
    roleAssignmentRepository.create(ctx, new RoleAssignment(testUser.getUserId(), role2.id()));

    roleAssignmentRepository.deleteByUserId(ctx, testUser.getUserId());

    assertThat(roleAssignmentRepository.findByUserId(ctx, testUser.getUserId())).isEmpty();
  }

  @Test
  void deleteByUserIdShouldNotThrowWhenNoAssignments() {
    assertThatCode(() -> roleAssignmentRepository.deleteByUserId(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }

  @Test
  void findByRoleIdShouldReturnAllAssignmentsForRole() {
    AuthUser user2 = AuthUser.create("user2");
    authUserRepository.create(ctx, user2);
    roleAssignmentRepository.create(ctx, new RoleAssignment(testUser.getUserId(), testRole.id()));
    roleAssignmentRepository.create(ctx, new RoleAssignment(user2.getUserId(), testRole.id()));

    var assignments = roleAssignmentRepository.findByRoleId(ctx, testRole.id());

    assertThat(assignments).hasSize(2);
    assertThat(assignments)
        .extracting(RoleAssignment::userId)
        .containsExactlyInAnyOrder(testUser.getUserId(), user2.getUserId());
  }

  @Test
  void findByRoleIdShouldReturnEmptyWhenNoAssignments() {
    assertThat(roleAssignmentRepository.findByRoleId(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void deleteByRoleIdShouldRemoveAllAssignmentsForRole() {
    AuthUser user2 = AuthUser.create("user2");
    authUserRepository.create(ctx, user2);
    roleAssignmentRepository.create(ctx, new RoleAssignment(testUser.getUserId(), testRole.id()));
    roleAssignmentRepository.create(ctx, new RoleAssignment(user2.getUserId(), testRole.id()));

    roleAssignmentRepository.deleteByRoleId(ctx, testRole.id());

    assertThat(roleAssignmentRepository.findByRoleId(ctx, testRole.id())).isEmpty();
  }

  @Test
  void deleteByRoleIdShouldNotThrowWhenNoAssignments() {
    assertThatCode(() -> roleAssignmentRepository.deleteByRoleId(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }
}
