/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.tools.errorcodedoc;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import org.junit.jupiter.api.Test;

class ErrorCodeDocGeneratorTest {

  @Test
  void generate_ShouldContainSectionForEveryErrorCode() {
    String document = ErrorCodeDocGenerator.generate();

    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      assertThat(document).contains("### `" + code.getCode() + "`");
      assertThat(document).contains(code.getDescription().getMessage());
      assertThat(document).contains(code.getDescription().getCause());
      assertThat(document).contains(code.getDescription().getAction());
    }
  }

  @Test
  void generate_ShouldContainDocumentSkeleton() {
    String document = ErrorCodeDocGenerator.generate();

    assertThat(document).startsWith("---\n");
    assertThat(document).contains("# ScalarDB Analytics Error Codes");
    assertThat(document).contains("## Error code classes and descriptions");
  }

  @Test
  void generate_ShouldContainClassSectionForEveryErrorCodeClass() {
    String document = ErrorCodeDocGenerator.generate();

    for (AnalyticsErrorCode code : AnalyticsErrorCode.values()) {
      char classDigit = code.getCode().charAt("DB-ANALYTICS-".length());
      assertThat(document).contains("## `DB-ANALYTICS-" + classDigit + "xxxx` status codes");
    }
  }
}
