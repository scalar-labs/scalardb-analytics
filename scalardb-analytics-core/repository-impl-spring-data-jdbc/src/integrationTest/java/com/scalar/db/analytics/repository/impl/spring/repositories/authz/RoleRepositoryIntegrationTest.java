/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.repositories.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.repository.impl.spring.repository.authz.RoleRepositoryImpl;
import com.scalar.db.analytics.repository.impl.spring.support.AbstractScalarDbIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class RoleRepositoryIntegrationTest extends AbstractScalarDbIntegrationTest {

  @Autowired private RoleRepositoryImpl roleRepository;

  @Test
  void createShouldPersistRole() {
    Role role = Role.create("analyst");

    roleRepository.create(ctx, role);

    var found = roleRepository.findById(ctx, role.id());
    assertThat(found).isPresent();
    assertThat(found.get().name()).isEqualTo("analyst");
    assertThat(found.get().builtIn()).isFalse();
  }

  @Test
  void findByNameShouldReturnRole() {
    Role role = Role.create("data-engineer");
    roleRepository.create(ctx, role);

    var found = roleRepository.findByName(ctx, "data-engineer");

    assertThat(found).isPresent();
    assertThat(found.get().id()).isEqualTo(role.id());
  }

  @Test
  void findByNameShouldReturnEmptyWhenNotFound() {
    assertThat(roleRepository.findByName(ctx, "nonexistent")).isEmpty();
  }

  @Test
  void findByIdShouldReturnEmptyWhenMissing() {
    assertThat(roleRepository.findById(ctx, UUID.randomUUID())).isEmpty();
  }

  @Test
  void findAllShouldReturnAllRoles() {
    Role role1 = Role.create("role-a");
    Role role2 = Role.create("role-b");
    roleRepository.create(ctx, role1);
    roleRepository.create(ctx, role2);

    var roles = roleRepository.findAll(ctx);

    assertThat(roles).extracting(Role::name).containsExactlyInAnyOrder("role-a", "role-b");
  }

  @Test
  void createDuplicateNameShouldThrowEntityAlreadyExists() {
    Role role = Role.create("unique-role");
    roleRepository.create(ctx, role);

    Role duplicate = Role.create("unique-role");

    assertThatThrownBy(() -> roleRepository.create(ctx, duplicate))
        .isInstanceOf(AnalyticsException.class)
        .extracting(ex -> ((AnalyticsException) ex).getErrorCode())
        .isEqualTo(AnalyticsErrorCode.ROLE_ALREADY_EXISTS);
  }

  @Test
  void deleteByIdShouldRemoveRole() {
    Role role = Role.create("to-delete");
    roleRepository.create(ctx, role);

    roleRepository.deleteById(ctx, role.id());

    assertThat(roleRepository.findById(ctx, role.id())).isEmpty();
  }

  @Test
  void deleteByIdShouldNotThrowWhenMissing() {
    assertThatCode(() -> roleRepository.deleteById(ctx, UUID.randomUUID()))
        .doesNotThrowAnyException();
  }
}
