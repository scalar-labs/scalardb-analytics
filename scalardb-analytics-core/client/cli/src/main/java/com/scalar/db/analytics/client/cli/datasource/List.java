/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.datasource;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "list", description = "List data sources")
public class List implements Callable<Integer> {
  @Option(
      names = {"-c", "--catalog"},
      description = "Catalog name",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String catalog;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private DataSourceCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();

      java.util.List<DataSource> dataSources =
          modules.client().dataSource().listDataSourcesByCatalog(catalog);
      printer().printSuccess(new Success(dataSources));
      return ExitCode.OK;
    } catch (Exception e) {
      printer().printError(new CommandError("Failed to list data sources", e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
