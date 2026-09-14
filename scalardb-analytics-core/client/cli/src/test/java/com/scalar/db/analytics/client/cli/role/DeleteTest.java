/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.authz.RoleClient;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class DeleteTest extends CliTestBase {

  @Test
  void givenNoOptions_thenFails() {
    int exitCode = new CommandLine(cliRoot).execute(args("role", "delete"));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    verifyNoInteractions(modules.client().role());
  }

  @Test
  void givenRoleName_thenCallDeleteRole() {
    RoleClient mock = modules.client().role();
    when(mock.deleteRole("analyst")).thenReturn(true);

    int exitCode = new CommandLine(cliRoot).execute(args("role", "delete", "--role", "analyst"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("Role deleted: analyst");
    verify(mock).deleteRole("analyst");
  }

  @Test
  void givenNotFound_thenReturnsSoftwareError() {
    RoleClient mock = modules.client().role();
    when(mock.deleteRole("analyst")).thenReturn(false);

    int exitCode = new CommandLine(cliRoot).execute(args("role", "delete", "-r", "analyst"));

    assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
    assertThat(printer.getOutput()).contains("Role was not found: analyst");
  }
}
