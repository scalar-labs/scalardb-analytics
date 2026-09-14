/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.authz.PermissionClient;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class GrantTest extends CliTestBase {

  @Test
  void givenNoSubcommand_thenShowsUsage() {
    int exitCode = new CommandLine(cliRoot).execute(args("permission", "grant"));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    verifyNoInteractions(modules.client().permission());
  }

  @Test
  void grantCatalog_withUser_thenCallsGrantCatalogPermission() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "grant",
                    "catalog",
                    "-p",
                    "read",
                    "--user",
                    "alice",
                    "--catalog",
                    "prod"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("Permission CATALOG_READ granted to USER alice");
    verify(modules.client().permission())
        .grantCatalogPermission("USER", "alice", "CATALOG_READ", "prod");
  }

  @Test
  void grantCatalog_withRole_thenCallsGrantCatalogPermission() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "grant",
                    "catalog",
                    "-p",
                    "admin",
                    "--role",
                    "editor",
                    "--catalog",
                    "prod"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("Permission CATALOG_ADMIN granted to ROLE editor");
    verify(modules.client().permission())
        .grantCatalogPermission("ROLE", "editor", "CATALOG_ADMIN", "prod");
  }

  @Test
  void grantDataSource_thenCallsGrantDataSourcePermission() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "grant",
                    "data-source",
                    "-p",
                    "admin",
                    "--role",
                    "editor",
                    "--catalog",
                    "prod",
                    "--data-source",
                    "ds1"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("Permission DATA_SOURCE_ADMIN granted to ROLE editor");
    verify(modules.client().permission())
        .grantDataSourcePermission("ROLE", "editor", "DATA_SOURCE_ADMIN", "prod", "ds1");
  }

  @Test
  void grantNamespace_thenCallsGrantNamespacePermission() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "grant",
                    "namespace",
                    "-p",
                    "read",
                    "--user",
                    "bob",
                    "--catalog",
                    "prod",
                    "--data-source",
                    "ds1",
                    "--namespace",
                    "ns1"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("Permission NAMESPACE_READ granted to USER bob");
    verify(modules.client().permission())
        .grantNamespacePermission(
            "USER", "bob", "NAMESPACE_READ", "prod", "ds1", Collections.singletonList("ns1"));
  }

  @Test
  void grantTable_thenCallsGrantTablePermission() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "grant",
                    "table",
                    "-p",
                    "read",
                    "--user",
                    "alice",
                    "--catalog",
                    "prod",
                    "--data-source",
                    "ds1",
                    "--namespace",
                    "ns1",
                    "--table",
                    "t1"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("Permission TABLE_READ granted to USER alice");
    verify(modules.client().permission())
        .grantTablePermission(
            "USER", "alice", "TABLE_READ", "prod", "ds1", Collections.singletonList("ns1"), "t1");
  }

  @Test
  void grantCatalog_givenAnalyticsException_thenReturnsUsageError() {
    PermissionClient mock = modules.client().permission();
    doThrow(new AnalyticsException(AnalyticsErrorCode.INVALID_ARGUMENT))
        .when(mock)
        .grantCatalogPermission("USER", "alice", "CATALOG_READ", "prod");

    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "grant",
                    "catalog",
                    "-p",
                    "read",
                    "--user",
                    "alice",
                    "--catalog",
                    "prod"));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(printer.getOutput()).contains(AnalyticsErrorCode.INVALID_ARGUMENT.getCode());
  }

  @Test
  void grantCatalog_givenRuntimeException_thenReturnsSoftwareError() {
    PermissionClient mock = modules.client().permission();
    doThrow(new RuntimeException("Server error"))
        .when(mock)
        .grantCatalogPermission("USER", "alice", "CATALOG_READ", "prod");

    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "grant",
                    "catalog",
                    "-p",
                    "read",
                    "--user",
                    "alice",
                    "--catalog",
                    "prod"));

    assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
    assertThat(getStderr()).contains("java.lang.RuntimeException: Server error");
  }
}
