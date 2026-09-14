/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.internalbackend;

import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.PasswordReader;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "create",
    description =
        "Create a user (credential) in the internal authentication backend. Does not create an "
            + "Analytics user (principal); use `user create --backend-user` for that.")
public class Create implements Callable<Integer> {

  @Option(
      names = {"-u", "--user"},
      description = "Internal-backend username (the credential's name)",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String username;

  @ArgGroup(exclusive = true, multiplicity = "1")
  private PasswordSource passwordSource;

  static class PasswordSource {
    @Option(
        names = {"-p", "--password"},
        description = "Prompt interactively for the password (no value)",
        interactive = true,
        arity = "0")
    String prompted;

    @Option(
        names = {"--password-stdin"},
        description = "Read the password from standard input")
    boolean fromStdin;
  }

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private InternalBackendCommand parent;

  @Override
  public Integer call() {
    try {
      String password =
          passwordSource.fromStdin ? PasswordReader.readFromStdin() : passwordSource.prompted;
      Modules modules = parent.root().loadModules();
      modules.client().internalUserDirectory().createInternalBackendUser(username, password);
      printer()
          .printSuccess(new Success(String.format("Internal-backend user created: %s", username)));
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(
              new CommandError(
                  String.format("Failed to create internal-backend user: %s", username), e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
