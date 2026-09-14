/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.catalog;

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

@Command(name = "delete", description = "Delete a catalog")
public class Delete implements Callable<Integer> {
  @ArgGroup(exclusive = true, multiplicity = "1")
  @SuppressWarnings("NotNullFieldNotInitialized")
  private DeleteParams params;

  @Option(
      names = {"--cascade"},
      description = "Cascade delete all data sources and their children",
      defaultValue = "false")
  private boolean cascade;

  static class DeleteParams {
    @Option(
        names = {"-c", "--catalog"},
        description = "Catalog name")
    @Nullable String catalogName;

    @Option(
        names = {"-i", "--catalog-id"},
        description = "Catalog ID")
    @Nullable UUID catalogId;
  }

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private CatalogCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      boolean deleted;

      if (params.catalogName != null) {
        deleted = modules.client().catalog().deleteCatalogByName(params.catalogName, cascade);
      } else {
        // The arg group is exclusive with multiplicity 1, so the catalog ID is set when the catalog
        // name is not.
        deleted =
            modules
                .client()
                .catalog()
                .deleteCatalogById(Objects.requireNonNull(params.catalogId), cascade);
      }

      if (!deleted) {
        printer()
            .printFailure(
                new Failure(
                    String.format(
                        "Catalog was not found: %s",
                        params.catalogName != null ? params.catalogName : params.catalogId)));
        return ExitCode.USAGE;
      }

      printer()
          .printSuccess(
              new Success(
                  String.format(
                      "Catalog was deleted: %s",
                      params.catalogName != null ? params.catalogName : params.catalogId)));
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(
              new CommandError(
                  params.catalogName != null
                      ? String.format("Failed to delete catalog: %s", params.catalogName)
                      : String.format("Failed to delete catalog: %s", params.catalogId),
                  e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
