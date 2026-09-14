/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.client.cli.support.CliE2ETestBase;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.ExitCode;

/** E2E test for CLI error handling against a real server. */
class CliErrorHandlingE2ETest extends CliE2ETestBase {

  @Nested
  class CatalogErrors {

    @Test
    void describeNonExistentCatalog() {
      int exitCode = execute("catalog", "describe", "--catalog", "non_existent_catalog");

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    }
  }

  @Nested
  class DataSourceErrors {

    @Test
    void registerWithInvalidProviderJson() {
      int exitCode =
          execute(
              "data-source",
              "register",
              "--catalog",
              "any_catalog",
              "--data-source",
              "any_ds",
              "--provider-json",
              "not-json");

      assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    }

    @Test
    void registerToNonExistentCatalog() {
      String providerJson = buildPostgresProviderJson();
      int exitCode =
          execute(
              "data-source",
              "register",
              "--catalog",
              "non_existent_catalog",
              "--data-source",
              "some_ds",
              "--provider-json",
              providerJson);

      assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
    }
  }
}
