/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.authz.EffectivePermission;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.authz.PermissionClient;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class ListTest extends CliTestBase {

  @Test
  void givenNoOptions_thenFails() {
    int exitCode = new CommandLine(cliRoot).execute(args("permission", "list"));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    verifyNoInteractions(modules.client().permission());
  }

  @Test
  void givenUser_thenCallListPermissions() {
    PermissionClient mock = modules.client().permission();
    when(mock.listPermissions("alice"))
        .thenReturn(
            Arrays.asList(
                new EffectivePermission("CATALOG_READ", "res-1", "DIRECT", null, null),
                new EffectivePermission("TABLE_READ", "res-2", "VIA_ROLE", "r-1", "admin")));

    int exitCode = new CommandLine(cliRoot).execute(args("permission", "list", "--user", "alice"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("CATALOG_READ");
    assertThat(printer.getOutput()).contains("TABLE_READ");
    verify(mock).listPermissions("alice");
  }

  @Test
  void givenNoPermissions_thenReturnsEmptyList() {
    PermissionClient mock = modules.client().permission();
    when(mock.listPermissions("bob")).thenReturn(Collections.emptyList());

    int exitCode = new CommandLine(cliRoot).execute(args("permission", "list", "-u", "bob"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    verify(mock).listPermissions("bob");
  }

  @Test
  void givenRole_thenCallListPermissionsForRole() {
    PermissionClient mock = modules.client().permission();
    when(mock.listPermissionsForRole("admin"))
        .thenReturn(
            Arrays.asList(
                new EffectivePermission("CATALOG_READ", "res-1", "DIRECT", null, null),
                new EffectivePermission("TABLE_READ", "res-2", "DIRECT", null, null)));

    int exitCode = new CommandLine(cliRoot).execute(args("permission", "list", "--role", "admin"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("CATALOG_READ");
    assertThat(printer.getOutput()).contains("TABLE_READ");
    verify(mock).listPermissionsForRole("admin");
  }

  @Test
  void givenBothUserAndRole_thenFails() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(args("permission", "list", "--user", "alice", "--role", "admin"));

    // --user / --role form a mutually exclusive (exclusive=true) ArgGroup, so supplying both is a
    // usage error and the command must not reach the client.
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    verifyNoInteractions(modules.client().permission());
  }
}
