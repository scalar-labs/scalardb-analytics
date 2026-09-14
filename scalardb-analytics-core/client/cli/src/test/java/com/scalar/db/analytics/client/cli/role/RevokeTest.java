/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.authz.RoleClient;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class RevokeTest extends CliTestBase {

  @Test
  void givenNoOptions_thenFails() {
    int exitCode = new CommandLine(cliRoot).execute(args("role", "revoke"));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    verifyNoInteractions(modules.client().role());
  }

  @Test
  void givenRoleAndUser_thenCallRevokeRole() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(args("role", "revoke", "--role", "editor", "--user", "alice"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("Role editor revoked from user alice");
    RoleClient mock = modules.client().role();
    verify(mock).revokeRole("editor", "alice");
  }
}
