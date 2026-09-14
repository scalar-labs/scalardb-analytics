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

/**
 * E2E test for cascade delete behavior: deleting a catalog with dependent data sources should fail
 * without --cascade and succeed with --cascade.
 */
@TestMethodOrder(OrderAnnotation.class)
class CatalogCascadeDeleteE2ETest extends CliE2ETestBase {

  private static final String CATALOG_NAME = "cascade_delete_test";
  private static final String DATA_SOURCE_NAME = "cascade_ds";

  @AfterAll
  static void cleanUp() {
    executeStatic("catalog", "delete", "--catalog", CATALOG_NAME, "--cascade");
  }

  @Test
  @Order(1)
  void setUpCatalogAndDataSource() {
    int createCatalog = execute("catalog", "create", "--catalog", CATALOG_NAME);
    assertThat(createCatalog).isEqualTo(ExitCode.OK);

    String providerJson = buildPostgresProviderJson();
    int registerDs =
        execute(
            "data-source",
            "register",
            "--catalog",
            CATALOG_NAME,
            "--data-source",
            DATA_SOURCE_NAME,
            "--provider-json",
            providerJson);
    assertThat(registerDs).isEqualTo(ExitCode.OK);
  }

  @Test
  @Order(2)
  void deleteWithoutCascadeFails() {
    int exitCode = execute("catalog", "delete", "--catalog", CATALOG_NAME);

    assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
  }

  @Test
  @Order(3)
  void deleteWithCascadeSucceeds() {
    int exitCode = execute("catalog", "delete", "--catalog", CATALOG_NAME, "--cascade");

    assertThat(exitCode).isEqualTo(ExitCode.OK);
  }
}
