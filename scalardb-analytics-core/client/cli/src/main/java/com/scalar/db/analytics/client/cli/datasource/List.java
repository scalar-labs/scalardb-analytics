/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.datasource;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.client.cli.CliRoot;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;
import picocli.CommandLine.Spec;

@Command(name = "list", description = "List data sources")
public class List implements Callable<Integer> {
  @Option(
      names = {"-c", "--catalog"},
      description = CliRoot.CATALOG_OPTION_DESCRIPTION)
  @Nullable
  private String catalog;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private DataSourceCommand parent;

  @Spec
  @SuppressWarnings("NotNullFieldNotInitialized")
  private CommandSpec spec;

  @Override
  public Integer call() {
    String catalogName = parent.root().resolveCatalog(catalog, spec);
    try {
      Modules modules = parent.root().loadModules();

      java.util.List<DataSource> dataSources =
          modules.client().dataSource().listDataSourcesByCatalog(catalogName);
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
