/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.assertj.core.api.Assertions.entry;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.grpc.generated.auth.v1.GranteeType;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RequestFieldsTest {

  @Test
  void uuid_shouldParseUuid() {
    UUID id = UUID.randomUUID();

    assertThat(RequestFields.uuid("role_id", id.toString())).isEqualTo(id);
  }

  @Test
  void uuid_withMalformedValue_shouldThrowInvalidArgumentKeepingCause() {
    AnalyticsException e =
        catchThrowableOfType(AnalyticsException.class, () -> RequestFields.uuid("role_id", "x"));

    assertThat(e.getErrorCode()).isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT);
    assertThat(e.getMetadata())
        .containsOnly(entry("field", "role_id"), entry("reason", "malformed_uuid"));
    assertThat(e.getCause()).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void specified_shouldReturnConcreteValue() {
    assertThat(RequestFields.specified("grantee_type", GranteeType.GRANTEE_TYPE_ROLE))
        .isEqualTo(GranteeType.GRANTEE_TYPE_ROLE);
  }

  @Test
  void specified_withUnspecified_shouldThrowInvalidArgument() {
    AnalyticsException e =
        catchThrowableOfType(
            AnalyticsException.class,
            () -> RequestFields.specified("grantee_type", GranteeType.GRANTEE_TYPE_UNSPECIFIED));

    assertThat(e.getErrorCode()).isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT);
    assertThat(e.getMetadata())
        .containsOnly(entry("field", "grantee_type"), entry("reason", "unspecified"));
  }

  @Test
  void specified_withUnrecognized_shouldThrowInvalidArgument() {
    AnalyticsException e =
        catchThrowableOfType(
            AnalyticsException.class,
            () -> RequestFields.specified("grantee_type", GranteeType.UNRECOGNIZED));

    assertThat(e.getMetadata())
        .containsOnly(entry("field", "grantee_type"), entry("reason", "unrecognized"));
  }
}
