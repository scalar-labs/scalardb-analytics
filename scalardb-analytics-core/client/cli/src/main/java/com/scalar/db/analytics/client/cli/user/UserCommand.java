/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.user;

import com.scalar.db.analytics.client.cli.CliRoot;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.ScopeType;

@Command(
    name = "user",
    description = "Manage users",
    scope = ScopeType.INHERIT,
    subcommands = {
      Create.class,
      Delete.class,
      List.class,
      Describe.class,
      Link.class,
      Unlink.class
    })
public class UserCommand implements Runnable {
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
