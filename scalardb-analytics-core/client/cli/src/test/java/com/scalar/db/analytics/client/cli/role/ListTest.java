/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.authz.Role;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.authz.RoleClient;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class ListTest extends CliTestBase {

  @Test
  void givenRolesExist_thenListsAll() {
    RoleClient mock = modules.client().role();
    when(mock.listRoles())
        .thenReturn(
            Arrays.asList(new Role("id-1", "admin", false), new Role("id-2", "SUPERADMIN", true)));

    int exitCode = new CommandLine(cliRoot).execute(args("role", "list"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("admin");
    assertThat(printer.getOutput()).contains("SUPERADMIN");
  }

  @Test
  void givenNoRoles_thenReturnsEmptyList() {
    RoleClient mock = modules.client().role();
    when(mock.listRoles()).thenReturn(Collections.emptyList());

    int exitCode = new CommandLine(cliRoot).execute(args("role", "list"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
  }

  @Test
  void givenExceptionThrown_thenReturnsSoftwareError() {
    RoleClient mock = modules.client().role();
    when(mock.listRoles()).thenThrow(new RuntimeException("List failed"));

    int exitCode = new CommandLine(cliRoot).execute(args("role", "list"));

    assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
    assertThat(getStderr()).contains("java.lang.RuntimeException: List failed");
  }
}
