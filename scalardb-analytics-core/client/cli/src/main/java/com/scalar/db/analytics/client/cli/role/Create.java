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
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "create", description = "Create a new role")
public class Create implements Callable<Integer> {
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
      Role role = modules.client().role().createRole(roleName);
      printer()
          .printSuccess(
              new Success(
                  String.format("Role created: %s (id: %s)", roleName, role.getId()), role));
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(new CommandError(String.format("Failed to create role: %s", roleName), e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
