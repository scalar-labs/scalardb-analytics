/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.user;

import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Failure;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.UUID;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "delete",
    description =
        "Delete an Analytics user (principal). Fails if dependents (backend users, role "
            + "assignments) still exist unless --cascade is given.")
public class Delete implements Callable<Integer> {

  @ArgGroup(multiplicity = "1")
  private Selector selector;

  static class Selector {
    @Option(
        names = {"-u", "--user"},
        description = "Analytics username")
    String username;

    @Option(
        names = {"-i", "--user-id"},
        description = "User ID (UUID)")
    UUID userId;
  }

  @Option(
      names = {"--cascade"},
      description = "Remove linked backend users, identities, and role assignments")
  private boolean cascade;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private UserCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      boolean deleted;
      String label;
      if (selector.username != null) {
        deleted = modules.client().user().deleteUser(selector.username, cascade);
        label = selector.username;
      } else {
        deleted = modules.client().user().deleteUserById(selector.userId, cascade);
        label = selector.userId.toString();
      }
      if (deleted) {
        printer().printSuccess(new Success(String.format("User deleted: %s", label)));
        return ExitCode.OK;
      }
      // Not-found is reported as a failure (non-zero exit) so callers can distinguish a real delete
      // from a no-op, consistent with `data-source unregister`. Idempotent "ensure-absent" callers
      // can opt in via a future --if-exists flag rather than this being the default.
      printer().printFailure(new Failure(String.format("User not found: %s", label)));
      return ExitCode.USAGE;
    } catch (Exception e) {
      printer().printError(new CommandError("Failed to delete user", e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
