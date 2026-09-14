/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.permission;

import com.scalar.db.analytics.api.authz.EffectivePermission;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ParentCommand;

@Command(name = "list", description = "List effective permissions for a user or a role")
public class List implements Callable<Integer> {

  @ArgGroup(exclusive = true, multiplicity = "1")
  private GranteeOptions grantee;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private PermissionCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      java.util.List<EffectivePermission> permissions =
          grantee.user != null
              ? modules.client().permission().listPermissions(grantee.user)
              : modules.client().permission().listPermissionsForRole(grantee.role);
      printer().printSuccess(new Success(permissions));
      return ExitCode.OK;
    } catch (Exception e) {
      printer().printError(new CommandError("Failed to list permissions", e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
