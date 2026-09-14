/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.auth.UserDetail;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.auth.UserClient;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class DescribeTest extends CliTestBase {

  private static final UUID TARGET_USER_ID = UUID.randomUUID();

  @Test
  void givenUserExistsById_thenDescribesUser() {
    UserClient mock = modules.client().user();
    UserDetail detail =
        new UserDetail(
            TARGET_USER_ID.toString(),
            "alice",
            PasswordBackendType.INTERNAL,
            Arrays.asList("SUPERADMIN", "admin"),
            "alice");
    when(mock.describeUserById(TARGET_USER_ID)).thenReturn(Optional.of(detail));

    int exitCode =
        new CommandLine(cliRoot)
            .execute(args("user", "describe", "--user-id", TARGET_USER_ID.toString()));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("alice");
    assertThat(printer.getOutput()).contains("SUPERADMIN");
  }

  @Test
  void givenUserExistsByName_thenDescribesUser() {
    UserClient mock = modules.client().user();
    UserDetail detail =
        new UserDetail(
            TARGET_USER_ID.toString(),
            "alice",
            PasswordBackendType.INTERNAL,
            Arrays.asList("SUPERADMIN", "admin"),
            "alice");
    when(mock.describeUserByName("alice")).thenReturn(Optional.of(detail));

    int exitCode = new CommandLine(cliRoot).execute(args("user", "describe", "--user", "alice"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput()).contains("alice");
    assertThat(printer.getOutput()).contains("SUPERADMIN");
  }

  @Test
  void givenUserNotFoundById_thenReturnsUsageError() {
    UserClient mock = modules.client().user();
    when(mock.describeUserById(TARGET_USER_ID)).thenReturn(Optional.empty());

    int exitCode =
        new CommandLine(cliRoot)
            .execute(args("user", "describe", "--user-id", TARGET_USER_ID.toString()));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(printer.getOutput()).contains("User was not found: " + TARGET_USER_ID);
  }

  @Test
  void givenUserNotFoundByName_thenReturnsUsageError() {
    UserClient mock = modules.client().user();
    when(mock.describeUserByName("nonexistent")).thenReturn(Optional.empty());

    int exitCode =
        new CommandLine(cliRoot).execute(args("user", "describe", "--user", "nonexistent"));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    assertThat(printer.getOutput()).contains("User was not found: nonexistent");
  }

  @Test
  void givenExceptionThrown_thenReturnsSoftwareError() {
    UserClient mock = modules.client().user();
    when(mock.describeUserById(TARGET_USER_ID)).thenThrow(new RuntimeException("Describe failed"));

    int exitCode =
        new CommandLine(cliRoot)
            .execute(args("user", "describe", "--user-id", TARGET_USER_ID.toString()));

    assertThat(exitCode).isEqualTo(ExitCode.SOFTWARE);
    assertThat(getStderr()).contains("java.lang.RuntimeException: Describe failed");
  }

  @Test
  void givenNoOption_thenReturnsUsageError() {
    int exitCode = new CommandLine(cliRoot).execute(args("user", "describe"));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
  }

  @Test
  void givenBothOptions_thenReturnsUsageError() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(
                args(
                    "user", "describe", "--user-id", TARGET_USER_ID.toString(), "--user", "alice"));

    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
  }
}
