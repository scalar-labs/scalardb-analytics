/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.authz;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class RoleTest {

  @Test
  void createShouldGenerateUniqueId() {
    Role role = Role.create("test-role");

    assertThat(role.id()).isNotNull();
    assertThat(role.name()).isEqualTo("test-role");
    assertThat(role.builtIn()).isFalse();
  }

  @Test
  void createShouldGenerateDifferentIds() {
    Role role1 = Role.create("role1");
    Role role2 = Role.create("role2");

    assertThat(role1.id()).isNotEqualTo(role2.id());
  }

  // Passing null violates the @NullMarked contract, but the runtime guard exists for callers that
  // do not enforce it, so the test deliberately passes null.
  @SuppressWarnings("NullAway")
  @Test
  void createShouldRejectNullName() {
    assertThatNullPointerException().isThrownBy(() -> Role.create(null));
  }

  @Test
  void createShouldRejectBlankName() {
    assertThatThrownBy(() -> Role.create(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blank");

    assertThatThrownBy(() -> Role.create("   "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("blank");
  }
}
