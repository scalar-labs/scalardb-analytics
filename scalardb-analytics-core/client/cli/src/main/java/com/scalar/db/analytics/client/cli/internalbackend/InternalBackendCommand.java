/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.internalbackend;

import com.scalar.db.analytics.client.cli.CliRoot;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.ScopeType;

@Command(
    name = "internal-backend",
    description = "Manage users of the internal authentication backend (login credentials)",
    scope = ScopeType.INHERIT,
    subcommands = {Create.class, Delete.class})
public class InternalBackendCommand implements Runnable {
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
