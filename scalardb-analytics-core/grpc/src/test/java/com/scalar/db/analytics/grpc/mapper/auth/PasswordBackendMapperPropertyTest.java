/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.grpc.generated.auth.v1.PasswordBackend;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;

class PasswordBackendMapperPropertyTest {

  private final PasswordBackendMapper mapper = PasswordBackendMapper.INSTANCE;

  @Property
  void domain_shouldRoundTripThroughProto(@ForAll PasswordBackendType domain) {
    assertThat(mapper.toDomain(mapper.toProto(domain))).isEqualTo(domain);
  }

  @Property
  void toProto_shouldMapEachConcreteValueToItsWireConstant() {
    assertThat(mapper.toProto(PasswordBackendType.INTERNAL))
        .isEqualTo(PasswordBackend.PASSWORD_BACKEND_INTERNAL);
    assertThat(mapper.toProto(PasswordBackendType.SCALARDB_CLUSTER))
        .isEqualTo(PasswordBackend.PASSWORD_BACKEND_SCALARDB_CLUSTER);
  }

  @Property
  void toDomain_shouldMapEachConcreteWireConstant() {
    assertThat(mapper.toDomain(PasswordBackend.PASSWORD_BACKEND_INTERNAL))
        .isEqualTo(PasswordBackendType.INTERNAL);
    assertThat(mapper.toDomain(PasswordBackend.PASSWORD_BACKEND_SCALARDB_CLUSTER))
        .isEqualTo(PasswordBackendType.SCALARDB_CLUSTER);
  }

  @Property
  void toDomain_unspecified_shouldThrow() {
    // proto3 zero-default placeholder: the client did not set a concrete backend. The mapper must
    // reject it (THROW_EXCEPTION), never silently map it to a null/default backend.
    assertThatThrownBy(() -> mapper.toDomain(PasswordBackend.PASSWORD_BACKEND_UNSPECIFIED))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Property
  void toDomain_unrecognized_shouldThrow() {
    // A value newer than this build knows (e.g. a future backend). Must be rejected, never
    // silently mapped to null.
    assertThatThrownBy(() -> mapper.toDomain(PasswordBackend.UNRECOGNIZED))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
