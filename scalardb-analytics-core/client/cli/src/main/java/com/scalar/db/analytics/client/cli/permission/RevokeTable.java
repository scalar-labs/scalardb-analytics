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

@Command(name = "table", description = "Revoke a permission on a table")
public class RevokeTable implements Callable<Integer> {
  @Option(
      names = {"-p", "--permission"},
      description = "Permission type: read",
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

  @Option(
      names = {"-n", "--namespace"},
      description = "Namespace names (comma-separated)",
      required = true,
      split = ",")
  @SuppressWarnings("NotNullFieldNotInitialized")
  private java.util.List<String> namespaceNames;

  @Option(
      names = {"-t", "--table"},
      description = "Table name",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String tableName;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private RevokeCommand parent;

  @Spec
  @SuppressWarnings("NotNullFieldNotInitialized")
  private CommandSpec spec;

  @Override
  public Integer call() {
    String catalog = parent.parent().root().resolveCatalog(catalogName, spec);
    try {
      Modules modules = parent.parent().root().loadModules();
      String fullPermission = "TABLE_" + permission.toUpperCase(Locale.ROOT);
      modules
          .client()
          .permission()
          .revokeTablePermission(
              grantee.granteeType(),
              grantee.granteeName(),
              fullPermission,
              catalog,
              dataSourceName,
              namespaceNames,
              tableName);
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
      printer().printError(new CommandError("Failed to revoke permission on table", e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.parent().root().printer();
  }
}
