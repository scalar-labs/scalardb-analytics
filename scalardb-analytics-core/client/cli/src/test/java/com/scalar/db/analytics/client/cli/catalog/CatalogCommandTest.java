/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.client.cli.CliTestBase;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class CatalogCommandTest extends CliTestBase {
  @Test
  void givenNoArgs_thenPrintUsage() {
    String[] args = {"catalog"};
    int exitCode = new CommandLine(cliRoot).execute(args);

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).contains("Usage:");
  }
}
