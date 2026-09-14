/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.internalbackend;

import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Failure;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "delete",
    description =
        "Delete a user from the internal authentication backend. Fails if it is still "
            + "linked to a principal unless --cascade is given.")
public class Delete implements Callable<Integer> {

  @Option(
      names = {"-u", "--user"},
      description = "Internal-backend username",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String username;

  @Option(
      names = {"--cascade"},
      description = "Unlink from any principal before deleting")
  private boolean cascade;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private InternalBackendCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      boolean deleted =
          modules.client().internalUserDirectory().deleteInternalBackendUser(username, cascade);
      if (deleted) {
        printer()
            .printSuccess(
                new Success(String.format("Internal-backend user deleted: %s", username)));
        return ExitCode.OK;
      }
      // Not-found is reported as a failure (non-zero exit) so callers can distinguish a real delete
      // from a no-op, consistent with `data-source unregister`. Idempotent "ensure-absent" callers
      // can opt in via a future --if-exists flag rather than this being the default.
      printer()
          .printFailure(
              new Failure(String.format("Internal-backend user not found: %s", username)));
      return ExitCode.USAGE;
    } catch (Exception e) {
      printer()
          .printError(
              new CommandError(
                  String.format("Failed to delete internal-backend user: %s", username), e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
