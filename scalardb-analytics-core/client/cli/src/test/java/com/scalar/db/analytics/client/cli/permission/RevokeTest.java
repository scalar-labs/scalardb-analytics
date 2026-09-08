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

class RevokeTest extends CliTestBase {

  @Test
  void givenNoSubcommand_thenShowsUsage() {
    int exitCode = new CommandLine(cliRoot).execute(args("permission", "revoke"));
    assertThat(exitCode).isEqualTo(ExitCode.OK);
    verifyNoInteractions(modules.client().permission());
  }

  @Test
  void revokeCatalog_withUser_thenCallsRevokeCatalogPermission() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "revoke",
                    "catalog",
                    "-p",
                    "read",
                    "--user",
                    "alice",
                    "--catalog",
                    "prod"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("Permission CATALOG_READ revoked from USER alice");
    verify(modules.client().permission())
        .revokeCatalogPermission("USER", "alice", "CATALOG_READ", "prod");
  }

  @Test
  void revokeDataSource_withRole_thenCallsRevokeDataSourcePermission() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "revoke",
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
    assertThat(printer.getOutput())
        .contains("Permission DATA_SOURCE_ADMIN revoked from ROLE editor");
    verify(modules.client().permission())
        .revokeDataSourcePermission("ROLE", "editor", "DATA_SOURCE_ADMIN", "prod", "ds1");
  }

  @Test
  void revokeNamespace_thenCallsRevokeNamespacePermission() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "revoke",
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
    assertThat(printer.getOutput()).contains("Permission NAMESPACE_READ revoked from USER bob");
    verify(modules.client().permission())
        .revokeNamespacePermission(
            "USER", "bob", "NAMESPACE_READ", "prod", "ds1", Collections.singletonList("ns1"));
  }

  @Test
  void revokeTable_thenCallsRevokeTablePermission() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "revoke",
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
    assertThat(printer.getOutput()).contains("Permission TABLE_READ revoked from USER alice");
    verify(modules.client().permission())
        .revokeTablePermission(
            "USER", "alice", "TABLE_READ", "prod", "ds1", Collections.singletonList("ns1"), "t1");
  }

  @Test
  void revokeCatalog_givenAnalyticsException_thenReturnsUsageError() {
    PermissionClient mock = modules.client().permission();
    doThrow(new AnalyticsException(AnalyticsErrorCode.INVALID_ARGUMENT))
        .when(mock)
        .revokeCatalogPermission("USER", "alice", "CATALOG_BAD", "prod");

    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "permission",
                    "revoke",
                    "catalog",
                    "-p",
                    "BAD",
                    "--user",
                    "alice",
                    "--catalog",
                    "prod"));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(printer.getOutput()).contains(AnalyticsErrorCode.INVALID_ARGUMENT.getCode());
  }
}
