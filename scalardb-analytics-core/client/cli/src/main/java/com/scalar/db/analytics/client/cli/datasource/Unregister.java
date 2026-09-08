/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.datasource;

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
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

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
        description = "Catalog name",
        required = true)
    @SuppressWarnings("NotNullFieldNotInitialized")
    String catalogName;

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

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      boolean unregistered;

      if (params.byName != null) {
        unregistered =
            modules
                .client()
                .dataSource()
                .deleteDataSourceByName(
                    params.byName.catalogName, params.byName.dataSourceName, cascade);
      } else {
        unregistered =
            modules
                .client()
                .dataSource()
                .deleteDataSourceById(Objects.requireNonNull(params.dataSourceId), cascade);
      }

      if (!unregistered) {
        printer()
            .printFailure(
                new Failure(
                    params.byName != null
                        ? String.format(
                            "Data source was not found: %s.%s",
                            params.byName.catalogName, params.byName.dataSourceName)
                        : String.format("Data source was not found: %s", params.dataSourceId)));
        return ExitCode.USAGE;
      }

      String msg =
          params.byName != null
              ? String.format(
                  "Data source unregistered: %s.%s",
                  params.byName.catalogName, params.byName.dataSourceName)
              : String.format("Data source unregistered: %s", params.dataSourceId);
      printer().printSuccess(new Success(msg));
      return ExitCode.OK;
    } catch (Exception e) {
      String msg =
          params.byName != null
              ? String.format(
                  "Failed to unregister data source: %s.%s",
                  params.byName.catalogName, params.byName.dataSourceName)
              : String.format("Failed to unregister data source: %s", params.dataSourceId);
      printer().printError(new CommandError(msg, e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
