/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.tools.errorcodedoc;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.ErrorDescription;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates the user-facing error code documentation (MDX) from {@link AnalyticsErrorCode}.
 *
 * <p>The output mirrors the structure of the ScalarDB Cluster error codes page (an error code class
 * table followed by one section per code), extended with the Cause and Action text that {@link
 * ErrorDescription} colocates with each code.
 */
public final class ErrorCodeDocGenerator {

  private static final String CODE_PREFIX = "DB-ANALYTICS-";

  private ErrorCodeDocGenerator() {}

  /** Returns the full MDX document listing every {@link AnalyticsErrorCode}. */
  public static String generate() {
    Map<Character, List<AnalyticsErrorCode>> codesByClass =
        Arrays.stream(AnalyticsErrorCode.values())
            .sorted(Comparator.comparing(AnalyticsErrorCode::getCode))
            .collect(
                Collectors.groupingBy(
                    code -> classDigit(code.getCode()), LinkedHashMap::new, Collectors.toList()));

    StringBuilder sb = new StringBuilder();
    sb.append("---\n")
        .append("tags:\n")
        .append("  - Enterprise Option\n")
        .append("displayed_sidebar: docsEnglish\n")
        .append("---\n\n")
        .append("# ScalarDB Analytics Error Codes\n\n")
        .append("This page provides a list of error codes in ScalarDB Analytics.\n\n")
        .append("## Error code classes and descriptions\n\n")
        .append("| Class                | Description                            |\n")
        .append("|:---------------------|:---------------------------------------|\n");
    for (char classDigit : codesByClass.keySet()) {
      sb.append("| `")
          .append(CODE_PREFIX)
          .append(classDigit)
          .append("xxxx` | Errors for the ")
          .append(classDescription(classDigit))
          .append(" |\n");
    }
    sb.append('\n');

    for (Map.Entry<Character, List<AnalyticsErrorCode>> entry : codesByClass.entrySet()) {
      sb.append("## `")
          .append(CODE_PREFIX)
          .append(entry.getKey())
          .append("xxxx` status codes\n\n")
          .append("The following are status codes and messages for the ")
          .append(classDescription(entry.getKey()))
          .append(".\n\n");
      for (AnalyticsErrorCode code : entry.getValue()) {
        ErrorDescription description = code.getDescription();
        sb.append("### `")
            .append(code.getCode())
            .append("`\n\n")
            .append("**Message**\n\n")
            .append("```markdown\n")
            .append(description.getMessage())
            .append("\n```\n\n")
            .append("**Cause**\n\n")
            .append(description.getCause())
            .append("\n\n")
            .append("**Action**\n\n")
            .append(description.getAction())
            .append("\n\n");
      }
    }
    return sb.toString();
  }

  private static char classDigit(String code) {
    if (!code.startsWith(CODE_PREFIX) || code.length() <= CODE_PREFIX.length()) {
      throw new IllegalStateException("Unexpected error code format: " + code);
    }
    return code.charAt(CODE_PREFIX.length());
  }

  private static String classDescription(char classDigit) {
    return switch (classDigit) {
      case '1' -> "user error category";
      case '3' -> "internal error category";
      case '4' -> "client error category";
      // Fail loudly so that introducing a new error code class forces this mapping (and the
      // generated document) to be updated.
      default ->
          throw new IllegalStateException("Unknown error code class: " + classDigit + "xxxx");
    };
  }
}
