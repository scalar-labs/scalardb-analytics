/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PasswordBackendTypeTest {

  @Test
  void fromString_shouldAcceptLowercaseInternal() {
    assertThat(PasswordBackendType.fromString("internal")).isEqualTo(PasswordBackendType.INTERNAL);
  }

  @Test
  void fromString_shouldAcceptUppercaseInternal() {
    assertThat(PasswordBackendType.fromString("INTERNAL")).isEqualTo(PasswordBackendType.INTERNAL);
  }

  @Test
  void fromString_shouldAcceptMixedCaseInternal() {
    assertThat(PasswordBackendType.fromString("Internal")).isEqualTo(PasswordBackendType.INTERNAL);
  }

  @Test
  void fromString_shouldAcceptLowercaseScalarDbCluster() {
    assertThat(PasswordBackendType.fromString("scalardb_cluster"))
        .isEqualTo(PasswordBackendType.SCALARDB_CLUSTER);
  }

  @Test
  void fromString_shouldAcceptMixedCaseScalarDbCluster() {
    assertThat(PasswordBackendType.fromString("ScalarDB_Cluster"))
        .isEqualTo(PasswordBackendType.SCALARDB_CLUSTER);
  }

  // Passing null violates the @NullMarked contract, but the runtime guard exists for callers that
  // do not enforce it, so the test deliberately passes null.
  @SuppressWarnings("NullAway")
  @Test
  void fromString_shouldRejectNull() {
    assertThatThrownBy(() -> PasswordBackendType.fromString(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("internal")
        .hasMessageContaining("scalardb_cluster");
  }

  @Test
  void fromString_shouldRejectEmpty() {
    assertThatThrownBy(() -> PasswordBackendType.fromString(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("internal")
        .hasMessageContaining("scalardb_cluster");
  }

  @Test
  void fromString_shouldRejectUnknownValue() {
    assertThatThrownBy(() -> PasswordBackendType.fromString("bogus"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("bogus")
        .hasMessageContaining("internal")
        .hasMessageContaining("scalardb_cluster");
  }
}
