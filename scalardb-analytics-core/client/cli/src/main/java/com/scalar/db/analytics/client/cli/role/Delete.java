/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.role;

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

@Command(name = "delete", description = "Delete a role")
public class Delete implements Callable<Integer> {
  @Option(
      names = {"-r", "--role"},
      description = "Role name",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String roleName;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private RoleCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      boolean deleted = modules.client().role().deleteRole(roleName);

      if (!deleted) {
        printer().printFailure(new Failure(String.format("Role was not found: %s", roleName)));
        return ExitCode.SOFTWARE;
      }

      printer().printSuccess(new Success(String.format("Role deleted: %s", roleName)));
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(new CommandError(String.format("Failed to delete role: %s", roleName), e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
