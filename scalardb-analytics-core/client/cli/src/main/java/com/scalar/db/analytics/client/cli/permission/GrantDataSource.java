/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.permission;

import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.client.cli.CliRoot;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Failure;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.Locale;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "data-source", description = "Grant a permission on a data source")
public class GrantDataSource implements Callable<Integer> {
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
      description = CliRoot.CATALOG_OPTION_DESCRIPTION)
  @Nullable
  private String catalogName;

  @Option(
      names = {"-d", "--data-source"},
      description = "Data source name",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String dataSourceName;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private GrantCommand parent;

  @Spec
  @SuppressWarnings("NotNullFieldNotInitialized")
  private CommandSpec spec;

  @Override
  public Integer call() {
    String catalog = parent.parent().root().resolveCatalog(catalogName, spec);
    try {
      Modules modules = parent.parent().root().loadModules();
      String fullPermission = "DATA_SOURCE_" + permission.toUpperCase(Locale.ROOT);
      modules
          .client()
          .permission()
          .grantDataSourcePermission(
              grantee.granteeType(),
              grantee.granteeName(),
              fullPermission,
              catalog,
              dataSourceName);
      printer()
          .printSuccess(
              new Success(
                  String.format(
                      "Permission %s granted to %s %s",
                      fullPermission, grantee.granteeType(), grantee.granteeName())));
      return ExitCode.OK;
    } catch (AnalyticsException e) {
      printer().printFailure(new Failure(e.getMessage()));
      return ExitCode.USAGE;
    } catch (Exception e) {
      printer().printError(new CommandError("Failed to grant permission on data source", e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.parent().root().printer();
  }
}
