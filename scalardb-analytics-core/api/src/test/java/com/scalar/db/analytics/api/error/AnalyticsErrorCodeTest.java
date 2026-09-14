/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class AnalyticsErrorCodeTest {

  @ParameterizedTest
  @EnumSource(AnalyticsErrorCode.class)
  void fromCode_shouldReturnMatchingEnum(AnalyticsErrorCode code) {
    assertThat(AnalyticsErrorCode.fromCode(code.getCode())).hasValue(code);
  }

  @Test
  void fromCode_shouldReturnEmptyForUnknownCode() {
    assertThat(AnalyticsErrorCode.fromCode("DB-ANALYTICS-99999")).isEmpty();
    assertThat(AnalyticsErrorCode.fromCode("INVALID")).isEmpty();
  }

  @Test
  void allCodes_shouldBeUnique() {
    Set<String> codes = new HashSet<>();
    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      assertThat(codes.add(code.getCode()))
          .as("Duplicate code: %s for %s", code.getCode(), code.name())
          .isTrue();
    }
  }

  @ParameterizedTest
  @EnumSource(AnalyticsErrorCode.class)
  void allCodes_shouldHaveDbAnalyticsPrefix(AnalyticsErrorCode code) {
    assertThat(code.getCode()).startsWith("DB-ANALYTICS-");
  }

  @ParameterizedTest
  @EnumSource(AnalyticsErrorCode.class)
  void allCodes_shouldHaveNonNullFields(AnalyticsErrorCode code) {
    assertThat(code.getCode()).isNotNull();
    assertThat(code.getErrorCategory()).isNotNull();
    assertThat(code.getDescription()).isNotNull();
  }

  @ParameterizedTest
  @EnumSource(AnalyticsErrorCode.class)
  void allCodes_shouldHaveNonBlankDescriptionFields(AnalyticsErrorCode code) {
    assertThat(code.getDescription().getMessage())
        .as("%s should have a non-blank message", code.name())
        .isNotBlank();
    assertThat(code.getDescription().getCause())
        .as("%s should have a non-blank cause", code.name())
        .isNotBlank();
    assertThat(code.getDescription().getAction())
        .as("%s should have a non-blank action", code.name())
        .isNotBlank();
  }

  @Test
  void userErrors_shouldBeInRange1xxxx() {
    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      String numericPart = code.getCode().replace("DB-ANALYTICS-", "");
      int number = Integer.parseInt(numericPart);
      if (code.isUserError()) {
        assertThat(number).as("%s should be in 1xxxx range", code.name()).isBetween(10000, 19999);
      }
    }
  }

  @Test
  void retryableErrors_shouldBeInRange300xx() {
    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      if (code.isRetryable()) {
        String numericPart = code.getCode().replace("DB-ANALYTICS-", "");
        int number = Integer.parseInt(numericPart);
        assertThat(number).as("%s should be in 300xx range", code.name()).isBetween(30000, 30099);
      }
    }
  }

  @Test
  void nonRetryableServerErrors_shouldBeInRange301xx() {
    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      if (code.getErrorCategory() == AnalyticsErrorCode.ErrorCategory.NON_RETRYABLE_SERVER_ERROR) {
        String numericPart = code.getCode().replace("DB-ANALYTICS-", "");
        int number = Integer.parseInt(numericPart);
        assertThat(number).as("%s should be in 301xx range", code.name()).isBetween(30100, 30199);
      }
    }
  }

  @Test
  void clientErrors_shouldBeInRange4xxxx() {
    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      if (code.getErrorCategory() == AnalyticsErrorCode.ErrorCategory.CLIENT_ERROR) {
        String numericPart = code.getCode().replace("DB-ANALYTICS-", "");
        int number = Integer.parseInt(numericPart);
        assertThat(number).as("%s should be in 4xxxx range", code.name()).isBetween(40000, 49999);
      }
    }
  }

  @Test
  void clientErrors_shouldNotBeRetryable() {
    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      if (code.getErrorCategory() == AnalyticsErrorCode.ErrorCategory.CLIENT_ERROR) {
        assertThat(code.isRetryable()).as("%s should not be retryable", code.name()).isFalse();
      }
    }
  }

  @Test
  void requiresReauthentication_shouldBeTrueOnlyForTokenExpiredCodes() {
    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      if (code == AnalyticsErrorCode.TOKEN_EXPIRED
          || code == AnalyticsErrorCode.SCALARDB_BACKEND_TOKEN_EXPIRED) {
        assertThat(code.requiresReauthentication())
            .as("%s should require reauthentication", code.name())
            .isTrue();
      } else {
        assertThat(code.requiresReauthentication())
            .as("%s should not require reauthentication", code.name())
            .isFalse();
      }
    }
  }
}
