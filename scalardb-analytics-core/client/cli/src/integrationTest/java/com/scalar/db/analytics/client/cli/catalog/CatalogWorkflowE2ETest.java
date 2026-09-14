/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.client.cli.support.CliE2ETestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import picocli.CommandLine.ExitCode;

/** E2E test for the full catalog lifecycle: create, list, describe, duplicate, delete, verify. */
@TestMethodOrder(OrderAnnotation.class)
class CatalogWorkflowE2ETest extends CliE2ETestBase {

  private static final String CATALOG_NAME = "catalog_workflow_test";

  @AfterAll
  static void cleanUp() {
    executeStatic("catalog", "delete", "--catalog", CATALOG_NAME, "--cascade");
  }

  @Test
  @Order(1)
  void createCatalog() {
    int exitCode = execute("catalog", "create", "--catalog", CATALOG_NAME);

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).contains(CATALOG_NAME);
  }

  @Test
  @Order(2)
  void listCatalogShowsCreated() {
    int exitCode = execute("catalog", "list");

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).contains(CATALOG_NAME);
  }

  @Test
  @Order(3)
  void describeCatalog() {
    int exitCode = execute("catalog", "describe", "--catalog", CATALOG_NAME);

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).contains(CATALOG_NAME);
  }

  @Test
  @Order(4)
  void createDuplicateCatalogFails() {
    int exitCode = execute("catalog", "create", "--catalog", CATALOG_NAME);

    assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
  }

  @Test
  @Order(5)
  void deleteCatalog() {
    int exitCode = execute("catalog", "delete", "--catalog", CATALOG_NAME);

    assertThat(exitCode).isEqualTo(ExitCode.OK);
  }

  @Test
  @Order(6)
  void listCatalogAfterDelete() {
    int exitCode = execute("catalog", "list");

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).doesNotContain(CATALOG_NAME);
  }
}
