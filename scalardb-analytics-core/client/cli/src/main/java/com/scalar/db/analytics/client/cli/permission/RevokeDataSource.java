/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.permission;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Failure;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.Locale;
import java.util.concurrent.Callable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "data-source", description = "Revoke a permission on a data source")
public class RevokeDataSource implements Callable<Integer> {
  @Option(
      names = {"-p", "--permission"},
      description = "Permission type: read, admin",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String permission;

  @ArgGroup(exclusive = true, multiplicity = "1")
  @SuppressWarnings("NotNullFieldNotInitialized")
  private GranteeOptions grantee;

  @Option(
      names = {"-c", "--catalog"},
      description = "Catalog name",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String catalogName;

  @Option(
      names = {"-d", "--data-source"},
      description = "Data source name",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String dataSourceName;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private RevokeCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.parent().root().loadModules();
      String fullPermission = "DATA_SOURCE_" + permission.toUpperCase(Locale.ROOT);
      modules
          .client()
          .permission()
          .revokeDataSourcePermission(
              grantee.granteeType(),
              grantee.granteeName(),
              fullPermission,
              catalogName,
              dataSourceName);
      printer()
          .printSuccess(
              new Success(
                  String.format(
                      "Permission %s revoked from %s %s",
                      fullPermission, grantee.granteeType(), grantee.granteeName())));
      return ExitCode.OK;
    } catch (AnalyticsException e) {
      printer().printFailure(new Failure(e.getMessage()));
      return ExitCode.USAGE;
    } catch (Exception e) {
      printer().printError(new CommandError("Failed to revoke permission on data source", e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.parent().root().printer();
  }
}
