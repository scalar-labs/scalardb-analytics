/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.permission;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "grant",
    description = "Grant a permission on a resource to a user or role",
    subcommands = {
      GrantCatalog.class,
      GrantDataSource.class,
      GrantNamespace.class,
      GrantTable.class
    })
public class GrantCommand implements Runnable {
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
