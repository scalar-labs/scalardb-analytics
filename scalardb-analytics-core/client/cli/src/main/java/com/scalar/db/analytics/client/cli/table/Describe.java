/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.table;

import com.scalar.db.analytics.api.model.DataSourceNamespaceTableDetail;
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
      "Describe a table detail",
      "Either --table-id or --catalog, --data-source, --namespace and --table must be specified"
    },
    synopsisHeading = "",
    customSynopsis = {
      "Usage:",
      "  describe -i <table_id>",
      "  describe -c <catalog> -d <dataSource> -n <namespace> -t <table>",
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
        description = "Data source name.",
        required = true)
    @SuppressWarnings("NotNullFieldNotInitialized")
    String dataSource;

    @Option(
        names = {"-n", "--namespace"},
        description = {"Namespace name", "For nested namespaces, use '.' as a separator."},
        paramLabel = "<namespace>",
        split = "\\.",
        required = true)
    @SuppressWarnings("NotNullFieldNotInitialized")
    java.util.List<String> namespaceNames;

    @Option(
        names = {"-t", "--table"},
        description = "Table name",
        required = true)
    @SuppressWarnings("NotNullFieldNotInitialized")
    String table;
  }

  static class IdOrNameOption {
    @Option(
        names = {"-i", "--table-id"},
        description = "Table ID")
    @Nullable UUID tableId;

    @ArgGroup(exclusive = false, multiplicity = "1")
    @Nullable NamesOption names;
  }

  @ArgGroup(exclusive = true, multiplicity = "1")
  @SuppressWarnings("NotNullFieldNotInitialized")
  private IdOrNameOption idOrName;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private TableCommand parent;

  @Override
  public Integer call() {
    Modules modules = parent.root().loadModules();

    DescribeParameter param = DescribeParameter.fromOption(idOrName);
    try {
      Either<String, DataSourceNamespaceTableDetail> table;
      if (param instanceof DescribeParameter.ById) {
        DescribeParameter.ById byId = (DescribeParameter.ById) param;
        table =
            Either.fromOptional(
                describeTableById(modules, byId),
                String.format("Table was not found: %s", byId.getTableId()));
      } else if (param instanceof DescribeParameter.ByNames) {
        DescribeParameter.ByNames byNames = (DescribeParameter.ByNames) param;
        table =
            Either.fromOptional(
                describeTableByNames(modules, byNames),
                String.format(
                    "Table was not found: %s.%s.%s.%s",
                    byNames.getCatalog(),
                    byNames.getDataSource(),
                    String.join(".", byNames.getNamespaceNames()),
                    byNames.getTable()));
      } else {
        throw new IllegalStateException("Unknown DescribeParameter type: " + param.getClass());
      }

      if (table.isLeft()) {
        printer().printFailure(new Failure(Objects.requireNonNull(table.getLeft())));
        return ExitCode.USAGE;
      }
      printer().printSuccess(new Success(Objects.requireNonNull(table.getRight())));
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(
              new CommandError(
                  String.format("Failed to describe a table detail: %s", param.name()), e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }

  private interface DescribeParameter {
    String name();

    static DescribeParameter fromOption(IdOrNameOption option) {
      if (option.tableId != null) {
        return new ById(option.tableId);
      } else {
        Objects.requireNonNull(option.names);
        return new ByNames(
            option.names.catalog,
            option.names.dataSource,
            option.names.namespaceNames,
            option.names.table);
      }
    }

    @Value
    class ById implements DescribeParameter {
      UUID tableId;

      @Override
      public String name() {
        return tableId.toString();
      }
    }

    @Value
    class ByNames implements DescribeParameter {
      String catalog;
      String dataSource;
      java.util.List<String> namespaceNames;
      String table;

      @Override
      public String name() {
        return String.format(
            "%s.%s.%s.%s", catalog, dataSource, String.join(".", namespaceNames), table);
      }
    }
  }

  private Optional<DataSourceNamespaceTableDetail> describeTableById(
      Modules modules, DescribeParameter.ById param) {
    return modules.client().table().describeTableById(param.getTableId());
  }

  private Optional<DataSourceNamespaceTableDetail> describeTableByNames(
      Modules modules, DescribeParameter.ByNames param) {
    return modules
        .client()
        .table()
        .describeTableByName(
            param.getCatalog(), param.getDataSource(), param.getNamespaceNames(), param.getTable());
  }
}
