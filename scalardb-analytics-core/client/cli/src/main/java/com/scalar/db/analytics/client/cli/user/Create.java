/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.user;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.api.auth.UserInfo;
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
        "Create an Analytics user (principal). With --backend-user, also creates an "
            + "internal-backend user and links them in one atomic operation.")
public class Create implements Callable<Integer> {

  @Option(
      names = {"-u", "--user"},
      description = "Analytics username (principal)",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String username;

  @ArgGroup(exclusive = false)
  private BackendUserOptions backendUserOptions;

  static class BackendUserOptions {
    @Option(
        names = {"--backend-user"},
        description = "Internal-backend username to create and link atomically with the principal",
        required = true)
    @SuppressWarnings("NotNullFieldNotInitialized")
    String backendUsername;

    @ArgGroup(exclusive = true, multiplicity = "1")
    PasswordSource password;
  }

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
  private UserCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      if (backendUserOptions == null) {
        String userId = modules.client().user().createUser(username);
        // Emit the created principal as a structured result (consistent with `user list` /
        // `user describe`) so the server-assigned id is a machine-readable field rather than text
        // embedded in the message. A principal-only create has no backend identity, so the
        // backend fields are null and omitted from the output (see UserInfo @JsonInclude).
        printer()
            .printSuccess(new Success("User created", new UserInfo(userId, null, null, username)));
      } else {
        String password =
            backendUserOptions.password.fromStdin
                ? PasswordReader.readFromStdin()
                : backendUserOptions.password.prompted;
        String userId =
            modules
                .client()
                .user()
                .createUserWithBackendUser(username, backendUserOptions.backendUsername, password);
        printer()
            .printSuccess(
                new Success(
                    "User created with backend user",
                    new UserInfo(
                        userId,
                        backendUserOptions.backendUsername,
                        PasswordBackendType.INTERNAL,
                        username)));
      }
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(new CommandError(String.format("Failed to create user: %s", username), e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
