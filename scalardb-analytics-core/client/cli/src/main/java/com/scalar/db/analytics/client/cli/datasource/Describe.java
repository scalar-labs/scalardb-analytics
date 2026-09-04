/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.datasource;

import com.scalar.db.analytics.api.model.DataSource;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Failure;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.cli.util.Either;
import com.scalar.db.analytics.client.module.Modules;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import lombok.Value;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "describe",
    description = {
      "Describe a data source",
      "Either --data-source-id or --catalog and --data-source must be specified"
    },
    synopsisHeading = "",
    customSynopsis = {
      "Usage:",
      "  describe -i <data_source_id>",
      "  describe -c <catalog> -d <dataSource>",
    })
public class Describe implements Callable<Integer> {
  static class NamesOption {
    @Option(
        names = {"-c", "--catalog"},
        description = "Catalog name",
        required = true)
    @SuppressWarnings("NotNullFieldNotInitialized")
    String catalog;

    @Option(
        names = {"-d", "--data-source"},
        description = "Data source name",
        required = true)
    @SuppressWarnings("NotNullFieldNotInitialized")
    String dataSource;
  }

  static class IdOrNameOption {
    @Option(
        names = {"-i", "--data-source-id"},
        description = "Data source ID")
    @Nullable UUID dataSourceId;

    @ArgGroup(exclusive = false, multiplicity = "1")
    @Nullable NamesOption names;
  }

  @ArgGroup(exclusive = true, multiplicity = "1")
  @SuppressWarnings("NotNullFieldNotInitialized")
  private IdOrNameOption idOrName;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private DataSourceCommand parent;

  @Override
  public Integer call() {
    Modules modules = parent.root().loadModules();

    DescribeParameter param = DescribeParameter.fromOption(idOrName);
    try {
      Either<String, DataSource> dataSource;
      if (param instanceof DescribeParameter.ById) {
        DescribeParameter.ById byId = (DescribeParameter.ById) param;
        dataSource =
            Either.fromOptional(
                describeDataSourceById(modules, byId),
                String.format("Data source was not found: %s", byId.getDataSourceId()));
      } else if (param instanceof DescribeParameter.ByNames) {
        DescribeParameter.ByNames byNames = (DescribeParameter.ByNames) param;
        dataSource =
            Either.fromOptional(
                describeDataSourceByNames(modules, byNames),
                String.format(
                    "Data source was not found: %s.%s",
                    byNames.getCatalog(), byNames.getDataSource()));
      } else {
        throw new IllegalStateException("Unknown DescribeParameter type: " + param.getClass());
      }

      if (dataSource.isLeft()) {
        printer().printFailure(new Failure(Objects.requireNonNull(dataSource.getLeft())));
        return ExitCode.USAGE;
      }
      printer().printSuccess(new Success(Objects.requireNonNull(dataSource.getRight())));
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(
              new CommandError(
                  String.format("Failed to describe a data source: %s", param.name()), e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }

  private interface DescribeParameter {
    String name();

    static DescribeParameter fromOption(IdOrNameOption option) {
      if (option.dataSourceId != null) {
        return new ById(option.dataSourceId);
      } else {
        Objects.requireNonNull(option.names);
        return new ByNames(option.names.catalog, option.names.dataSource);
      }
    }

    @Value
    class ById implements DescribeParameter {
      UUID dataSourceId;

      @Override
      public String name() {
        return dataSourceId.toString();
      }
    }

    @Value
    class ByNames implements DescribeParameter {
      String catalog;
      String dataSource;

      @Override
      public String name() {
        return String.format("%s.%s", catalog, dataSource);
      }
    }
  }

  private Optional<DataSource> describeDataSourceById(
      Modules modules, DescribeParameter.ById param) {
    return modules.client().dataSource().findDataSourceById(param.getDataSourceId());
  }

  private Optional<DataSource> describeDataSourceByNames(
      Modules modules, DescribeParameter.ByNames param) {
    return modules
        .client()
        .dataSource()
        .findDataSourceByName(param.getCatalog(), param.getDataSource());
  }
}
