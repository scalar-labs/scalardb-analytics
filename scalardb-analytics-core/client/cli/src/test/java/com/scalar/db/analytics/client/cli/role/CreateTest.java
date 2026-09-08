/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.authz.Role;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.authz.RoleClient;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class CreateTest extends CliTestBase {

  @Test
  void givenNoOptions_thenFails() {
    int exitCode = new CommandLine(cliRoot).execute(args("role", "create"));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    verifyNoInteractions(modules.client().role());
  }

  @Test
  void givenName_thenCallCreateRole() {
    RoleClient mock = modules.client().role();
    when(mock.createRole(any())).thenReturn(new Role("role-123", "admin", false));

    int exitCode = new CommandLine(cliRoot).execute(args("role", "create", "--role", "admin"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("Role created: admin (id: role-123)");
    verify(mock).createRole("admin");
  }

  @Test
  void givenShortOption_thenCallCreateRole() {
    RoleClient mock = modules.client().role();
    when(mock.createRole(any())).thenReturn(new Role("role-456", "viewer", false));

    int exitCode = new CommandLine(cliRoot).execute(args("role", "create", "-r", "viewer"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("Role created: viewer (id: role-456)");
    verify(mock).createRole("viewer");
  }

  @Test
  void givenExceptionThrown_thenReturnsSoftwareError() {
    RoleClient mock = modules.client().role();
    when(mock.createRole(any())).thenThrow(new RuntimeException("Creation failed"));

    int exitCode = new CommandLine(cliRoot).execute(args("role", "create", "-r", "admin"));

    assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
    assertThat(getStderr()).contains("java.lang.RuntimeException: Creation failed");
  }
}
