/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.datasource;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.client.cli.support.CliE2ETestBase;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import picocli.CommandLine.ExitCode;

/**
 * E2E test for the data source workflow: register, list, describe, namespace/table introspection,
 * delete, and re-register from file.
 *
 * <p>Test execution order:
 *
 * <ol>
 *   <li>Catalog setup
 *   <li>Data source: register, list, describe
 *   <li>Namespace: list, describe (auto-resolved from PostgreSQL)
 *   <li>Table: list, describe (auto-resolved from PostgreSQL)
 *   <li>Data source: delete, re-register from file
 * </ol>
 */
@TestMethodOrder(OrderAnnotation.class)
class DataSourceWorkflowE2ETest extends CliE2ETestBase {

  private static final String CATALOG_NAME = "ds_workflow_catalog";
  private static final String DATA_SOURCE_NAME = "ds_workflow_ds";
  private static final String TEST_SCHEMA = "test_schema";
  private static final String TEST_TABLE = "test_table";

  @BeforeAll
  static void setUpCatalogAndTestData() throws Exception {
    executeSql("CREATE SCHEMA IF NOT EXISTS " + TEST_SCHEMA);
    executeSql(
        "CREATE TABLE IF NOT EXISTS "
            + TEST_SCHEMA
            + "."
            + TEST_TABLE
            + " (id SERIAL PRIMARY KEY, name VARCHAR(255) NOT NULL)");
  }

  @AfterAll
  static void cleanUp() {
    executeStatic("catalog", "delete", "--catalog", CATALOG_NAME, "--cascade");
  }

  // -- Catalog setup --

  @Test
  @Order(1)
  void catalogCreate() {
    int exitCode = execute("catalog", "create", "--catalog", CATALOG_NAME);
    assertThat(exitCode).isEqualTo(ExitCode.OK);
  }

  // -- Data source operations --

  @Test
  @Order(2)
  void dataSourceRegisterWithInlineJson() {
    String providerJson = buildPostgresProviderJson();
    int exitCode =
        execute(
            "data-source",
            "register",
            "--catalog",
            CATALOG_NAME,
            "--data-source",
            DATA_SOURCE_NAME,
            "--provider-json",
            providerJson);

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).contains(DATA_SOURCE_NAME);
  }

  @Test
  @Order(3)
  void dataSourceList() {
    int exitCode = execute("data-source", "list", "--catalog", CATALOG_NAME);

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).contains(DATA_SOURCE_NAME);
  }

  @Test
  @Order(4)
  void dataSourceDescribe() {
    int exitCode =
        execute(
            "data-source",
            "describe",
            "--catalog",
            CATALOG_NAME,
            "--data-source",
            DATA_SOURCE_NAME);

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).contains(DATA_SOURCE_NAME);
  }

  // -- Namespace introspection (auto-resolved from PostgreSQL) --

  @Test
  @Order(5)
  void namespaceList() {
    int exitCode = execute("namespace", "list", "--catalog", CATALOG_NAME);

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).contains(TEST_SCHEMA);
  }

  @Test
  @Order(6)
  void namespaceDescribe() {
    int exitCode =
        execute(
            "namespace",
            "describe",
            "--catalog",
            CATALOG_NAME,
            "--data-source",
            DATA_SOURCE_NAME,
            "--namespace",
            TEST_SCHEMA);

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).contains(TEST_SCHEMA);
  }

  // -- Table introspection (auto-resolved from PostgreSQL) --

  @Test
  @Order(7)
  void tableList() {
    int exitCode = execute("table", "list", "--catalog", CATALOG_NAME);

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).contains(TEST_TABLE);
  }

  @Test
  @Order(8)
  void tableDescribe() {
    int exitCode =
        execute(
            "table",
            "describe",
            "--catalog",
            CATALOG_NAME,
            "--data-source",
            DATA_SOURCE_NAME,
            "--namespace",
            TEST_SCHEMA,
            "--table",
            TEST_TABLE);

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    String output = getStdout();
    assertThat(output).contains("id");
    assertThat(output).contains("name");
  }

  // -- Delete and re-register --

  @Test
  @Order(9)
  void dataSourceDelete() {
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

  @Test
  @Order(10)
  void dataSourceRegisterFromFile() throws Exception {
    Path providerFile = Files.createTempFile("provider-", ".json");
    providerFile.toFile().deleteOnExit();
    Files.write(providerFile, buildPostgresProviderJson().getBytes(StandardCharsets.UTF_8));

    int exitCode =
        execute(
            "data-source",
            "register",
            "--catalog",
            CATALOG_NAME,
            "--data-source",
            DATA_SOURCE_NAME,
            "--provider-file",
            providerFile.toAbsolutePath().toString());

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(getStdout()).contains(DATA_SOURCE_NAME);
  }
}
