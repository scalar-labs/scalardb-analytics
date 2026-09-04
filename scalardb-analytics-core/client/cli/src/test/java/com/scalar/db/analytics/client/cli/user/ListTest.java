/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.auth.UserInfo;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.auth.UserClient;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class ListTest extends CliTestBase {

  @Test
  void givenUsersExist_thenListsAll() {
    UserClient mock = modules.client().user();
    when(mock.listUsers())
        .thenReturn(
            Arrays.asList(
                new UserInfo("id-1", "alice", PasswordBackendType.INTERNAL, "alice"),
                new UserInfo("id-2", "bob", PasswordBackendType.SCALARDB_CLUSTER, "bob")));

    int exitCode = new CommandLine(cliRoot).execute(args("user", "list"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("alice");
    assertThat(printer.getOutput()).contains("bob");
  }

  @Test
  void givenNoUsers_thenReturnsEmptyList() {
    UserClient mock = modules.client().user();
    when(mock.listUsers()).thenReturn(Collections.emptyList());

    int exitCode = new CommandLine(cliRoot).execute(args("user", "list"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
  }

  @Test
  void givenExceptionThrown_thenReturnsSoftwareError() {
    UserClient mock = modules.client().user();
    when(mock.listUsers()).thenThrow(new RuntimeException("List failed"));

    int exitCode = new CommandLine(cliRoot).execute(args("user", "list"));

    assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
    assertThat(getStderr()).contains("java.lang.RuntimeException: List failed");
  }
}
