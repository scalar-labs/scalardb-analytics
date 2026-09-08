/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.catalog;

import com.scalar.db.analytics.api.model.Catalog;
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
    description = {"Describe a catalog", "Either --catalog-id or --catalog must be specified"},
    synopsisHeading = "",
    customSynopsis = {
      "Usage:",
      "  describe -i <catalog_id>",
      "  describe -c <catalog>",
    })
public class Describe implements Callable<Integer> {
  static class NamesOption {
    @Option(
        names = {"-c", "--catalog"},
        description = "Catalog name",
        required = true)
    @SuppressWarnings("NotNullFieldNotInitialized")
    String catalog;
  }

  static class IdOrNameOption {
    @Option(
        names = {"-i", "--catalog-id"},
        description = "Catalog ID")
    @Nullable UUID catalogId;

    @ArgGroup(exclusive = false, multiplicity = "1")
    @Nullable NamesOption names;
  }

  @ArgGroup(exclusive = true, multiplicity = "1")
  @SuppressWarnings("NotNullFieldNotInitialized")
  private IdOrNameOption idOrName;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private CatalogCommand parent;

  @Override
  public Integer call() {
    Modules modules = parent.root().loadModules();

    DescribeParameter param = DescribeParameter.fromOption(idOrName);
    try {
      Either<String, Catalog> catalog;
      if (param instanceof DescribeParameter.ById) {
        DescribeParameter.ById byId = (DescribeParameter.ById) param;
        catalog =
            Either.fromOptional(
                describeCatalogById(modules, byId),
                String.format("Catalog was not found: %s", byId.getCatalogId()));
      } else if (param instanceof DescribeParameter.ByNames) {
        DescribeParameter.ByNames byNames = (DescribeParameter.ByNames) param;
        catalog =
            Either.fromOptional(
                describeCatalogByNames(modules, byNames),
                String.format("Catalog was not found: %s", byNames.getCatalog()));
      } else {
        throw new IllegalStateException("Unknown DescribeParameter type: " + param.getClass());
      }

      if (catalog.isLeft()) {
        printer().printFailure(new Failure(Objects.requireNonNull(catalog.getLeft())));
        return ExitCode.USAGE;
      }
      printer().printSuccess(new Success(Objects.requireNonNull(catalog.getRight())));
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(
              new CommandError(String.format("Failed to describe a catalog: %s", param.name()), e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }

  private interface DescribeParameter {
    String name();

    static DescribeParameter fromOption(IdOrNameOption option) {
      if (option.catalogId != null) {
        return new ById(option.catalogId);
      } else {
        Objects.requireNonNull(option.names);
        return new ByNames(option.names.catalog);
      }
    }

    @Value
    class ById implements DescribeParameter {
      UUID catalogId;

      @Override
      public String name() {
        return catalogId.toString();
      }
    }

    @Value
    class ByNames implements DescribeParameter {
      String catalog;

      @Override
      public String name() {
        return catalog;
      }
    }
  }

  private Optional<Catalog> describeCatalogById(Modules modules, DescribeParameter.ById param) {
    return modules.client().catalog().findCatalogById(param.getCatalogId());
  }

  private Optional<Catalog> describeCatalogByNames(
      Modules modules, DescribeParameter.ByNames param) {
    return modules.client().catalog().findCatalogByName(param.getCatalog());
  }
}
