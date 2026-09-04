/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.role;

import com.scalar.db.analytics.api.authz.Role;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ParentCommand;

@Command(name = "list", description = "List all roles")
public class List implements Callable<Integer> {
  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private RoleCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      java.util.List<Role> roles = modules.client().role().listRoles();
      printer().printSuccess(new Success(roles));
      return ExitCode.OK;
    } catch (Exception e) {
      printer().printError(new CommandError("Failed to list roles", e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
