/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.catalog;

import com.scalar.db.analytics.api.model.Catalog;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "create", description = "Create a new catalog")
public class Create implements Callable<Integer> {
  @Option(
      names = {"-c", "--catalog"},
      description = "Catalog name",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String name;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private CatalogCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      Catalog catalog = modules.client().catalog().createCatalog(name);
      printer().printSuccess(new Success(catalog));
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(new CommandError(String.format("Failed to create a catalog: %s", name), e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
