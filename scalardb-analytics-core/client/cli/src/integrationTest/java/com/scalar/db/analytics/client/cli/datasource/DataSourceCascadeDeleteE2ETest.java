/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.client.cli.support.CliE2ETestBase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import picocli.CommandLine.ExitCode;

/**
 * E2E test for data source cascade delete behavior: deleting a data source with dependent
 * namespaces should fail without --cascade and succeed with --cascade.
 */
@TestMethodOrder(OrderAnnotation.class)
class DataSourceCascadeDeleteE2ETest extends CliE2ETestBase {

  private static final String CATALOG_NAME = "ds_cascade_delete_test";
  private static final String DATA_SOURCE_NAME = "ds_cascade_ds";

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
    int exitCode =
        execute(
            "data-source",
            "unregister",
            "--catalog",
            CATALOG_NAME,
            "--data-source",
            DATA_SOURCE_NAME);

    assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
  }

  @Test
  @Order(3)
  void deleteWithCascadeSucceeds() {
    int exitCode =
        execute(
            "data-source",
            "unregister",
            "--catalog",
            CATALOG_NAME,
            "--data-source",
            DATA_SOURCE_NAME,
            "--cascade");

    assertThat(exitCode).isEqualTo(ExitCode.OK);
  }
}
