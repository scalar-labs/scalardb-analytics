/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.role;

import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "grant", description = "Assign a role to a user")
public class Grant implements Callable<Integer> {
  @Option(
      names = {"-r", "--role"},
      description = "Role name",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String roleName;

  @Option(
      names = {"-u", "--user"},
      description = "Username",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String username;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private RoleCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      modules.client().role().grantRole(roleName, username);
      printer()
          .printSuccess(
              new Success(String.format("Role %s granted to user %s", roleName, username)));
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(
              new CommandError(
                  String.format("Failed to grant role %s to user %s", roleName, username), e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
