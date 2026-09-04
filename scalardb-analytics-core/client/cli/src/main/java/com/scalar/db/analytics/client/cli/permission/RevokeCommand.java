/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.permission;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "revoke",
    description = "Revoke a permission on a resource from a user or role",
    subcommands = {
      RevokeCatalog.class,
      RevokeDataSource.class,
      RevokeNamespace.class,
      RevokeTable.class
    })
public class RevokeCommand implements Runnable {
  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private PermissionCommand parent;

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }

  PermissionCommand parent() {
    return parent;
  }
}
