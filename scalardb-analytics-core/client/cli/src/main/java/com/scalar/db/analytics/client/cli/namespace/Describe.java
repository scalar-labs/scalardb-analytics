/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.namespace;

import com.scalar.db.analytics.api.model.Namespace;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Failure;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.cli.util.Either;
import com.scalar.db.analytics.client.module.Modules;
import java.util.List;
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
      "Describe a namespace",
      "Either --namespace-id or --catalog, --data-source and --namespace must be specified"
    },
    synopsisHeading = "",
    customSynopsis = {
      "Usage:",
      "  describe -i <namespace_id>",
      "  describe -c <catalog> -d <dataSource> -n <namespace>",
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

    @Option(
        names = {"-n", "--namespace"},
        description = "Namespace names (comma-separated list)",
        required = true,
        split = ",")
    @SuppressWarnings("NotNullFieldNotInitialized")
    List<String> namespaceNames;
  }

  static class IdOrNameOption {
    @Option(
        names = {"-i", "--namespace-id"},
        description = "Namespace ID")
    @Nullable UUID namespaceId;

    @ArgGroup(exclusive = false, multiplicity = "1")
    @Nullable NamesOption names;
  }

  @ArgGroup(exclusive = true, multiplicity = "1")
  @SuppressWarnings("NotNullFieldNotInitialized")
  private IdOrNameOption idOrName;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private NameSpaceCommand parent;

  @Override
  public Integer call() {
    Modules modules = parent.root().loadModules();

    DescribeParameter param = DescribeParameter.fromOption(idOrName);
    try {
      Either<String, Namespace> namespace;
      if (param instanceof DescribeParameter.ById) {
        DescribeParameter.ById byId = (DescribeParameter.ById) param;
        namespace =
            Either.fromOptional(
                describeNamespaceById(modules, byId),
                String.format("Namespace was not found: %s", byId.getNamespaceId()));
      } else if (param instanceof DescribeParameter.ByNames) {
        DescribeParameter.ByNames byNames = (DescribeParameter.ByNames) param;
        namespace =
            Either.fromOptional(
                describeNamespaceByNames(modules, byNames),
                String.format(
                    "Namespace was not found: %s.%s.%s",
                    byNames.getCatalog(),
                    byNames.getDataSource(),
                    String.join(".", byNames.getNamespaceNames())));
      } else {
        throw new IllegalStateException("Unknown DescribeParameter type: " + param.getClass());
      }

      if (namespace.isLeft()) {
        printer().printFailure(new Failure(Objects.requireNonNull(namespace.getLeft())));
        return ExitCode.USAGE;
      }
      printer().printSuccess(new Success(Objects.requireNonNull(namespace.getRight())));
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(
              new CommandError(
                  String.format("Failed to describe a namespace: %s", param.name()), e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }

  private interface DescribeParameter {
    String name();

    static DescribeParameter fromOption(IdOrNameOption option) {
      if (option.namespaceId != null) {
        return new ById(option.namespaceId);
      } else {
        Objects.requireNonNull(option.names);
        return new ByNames(
            option.names.catalog, option.names.dataSource, option.names.namespaceNames);
      }
    }

    @Value
    class ById implements DescribeParameter {
      UUID namespaceId;

      @Override
      public String name() {
        return namespaceId.toString();
      }
    }

    @Value
    class ByNames implements DescribeParameter {
      String catalog;
      String dataSource;
      List<String> namespaceNames;

      @Override
      public String name() {
        return String.format("%s.%s.%s", catalog, dataSource, String.join(".", namespaceNames));
      }
    }
  }

  private Optional<Namespace> describeNamespaceById(Modules modules, DescribeParameter.ById param) {
    return modules.client().namespace().findNamespaceById(param.getNamespaceId());
  }

  private Optional<Namespace> describeNamespaceByNames(
      Modules modules, DescribeParameter.ByNames param) {
    return modules
        .client()
        .namespace()
        .findNamespaceByName(param.getCatalog(), param.getDataSource(), param.getNamespaceNames());
  }
}
