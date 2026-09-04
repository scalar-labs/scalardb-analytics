/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.permission;

import com.scalar.db.analytics.client.cli.CliRoot;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.ScopeType;

@Command(
    name = "permission",
    description = "Manage permissions",
    scope = ScopeType.INHERIT,
    subcommands = {GrantCommand.class, RevokeCommand.class, List.class})
public class PermissionCommand implements Runnable {
  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private CliRoot root;

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }

  public CliRoot root() {
    return root;
  }
}
