/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.datasource;

import com.scalar.db.analytics.client.cli.CliRoot;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Failure;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "unregister", description = "Unregister a data source from its catalog")
public class Unregister implements Callable<Integer> {
  @ArgGroup(exclusive = true, multiplicity = "1")
  @SuppressWarnings("NotNullFieldNotInitialized")
  private UnregisterParams params;

  @Option(
      names = {"--cascade"},
      description = "Cascade delete all namespaces, tables and columns",
      defaultValue = "false")
  private boolean cascade;

  static class UnregisterParams {
    @ArgGroup(exclusive = false, multiplicity = "1")
    @Nullable ByName byName;

    @Option(
        names = {"-i", "--data-source-id"},
        description = "Data source ID")
    @Nullable UUID dataSourceId;
  }

  static class ByName {
    @Option(
        names = {"-c", "--catalog"},
        description = CliRoot.CATALOG_OPTION_DESCRIPTION)
    @Nullable String catalogName;

    @Option(
        names = {"-d", "--data-source"},
        description = "Data source name",
        required = true)
    @SuppressWarnings("NotNullFieldNotInitialized")
    String dataSourceName;
  }

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private DataSourceCommand parent;

  @Spec
  @SuppressWarnings("NotNullFieldNotInitialized")
  private CommandSpec spec;

  @Override
  public Integer call() {
    ByName byName = params.byName;
    @Nullable String catalogName =
        byName == null ? null : parent.root().resolveCatalog(byName.catalogName, spec);
    String target =
        byName == null
            ? String.valueOf(params.dataSourceId)
            : String.format("%s.%s", catalogName, byName.dataSourceName);
    try {
      Modules modules = parent.root().loadModules();
      boolean unregistered;

      if (byName != null) {
        unregistered =
            modules
                .client()
                .dataSource()
                .deleteDataSourceByName(
                    Objects.requireNonNull(catalogName), byName.dataSourceName, cascade);
      } else {
        unregistered =
            modules
                .client()
                .dataSource()
                .deleteDataSourceById(Objects.requireNonNull(params.dataSourceId), cascade);
      }

      if (!unregistered) {
        printer().printFailure(new Failure("Data source was not found: " + target));
        return ExitCode.USAGE;
      }

      printer().printSuccess(new Success("Data source unregistered: " + target));
      return ExitCode.OK;
    } catch (Exception e) {
      printer().printError(new CommandError("Failed to unregister data source: " + target, e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
