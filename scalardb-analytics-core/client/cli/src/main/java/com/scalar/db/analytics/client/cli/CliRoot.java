/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.db.analytics.client.cli.Formatter.JsonFormatter;
import com.scalar.db.analytics.client.cli.Printer.ConsolePrinter;
import com.scalar.db.analytics.client.cli.catalog.CatalogCommand;
import com.scalar.db.analytics.client.cli.datasource.DataSourceCommand;
import com.scalar.db.analytics.client.cli.internalbackend.InternalBackendCommand;
import com.scalar.db.analytics.client.cli.namespace.NameSpaceCommand;
import com.scalar.db.analytics.client.cli.permission.PermissionCommand;
import com.scalar.db.analytics.client.cli.role.RoleCommand;
import com.scalar.db.analytics.client.cli.table.TableCommand;
import com.scalar.db.analytics.client.cli.user.UserCommand;
import com.scalar.db.analytics.client.config.AppConfig;
import com.scalar.db.analytics.client.config.UnvalidatedConfig;
import com.scalar.db.analytics.client.module.Modules;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ScopeType;

@Command(
    name = "scalardb-analytics",
    description = "Command line interface for Scalar DB Analytics",
    usageHelpWidth = 200,
    mixinStandardHelpOptions = true,
    scope = ScopeType.INHERIT,
    subcommands = {
      CatalogCommand.class,
      DataSourceCommand.class,
      NameSpaceCommand.class,
      TableCommand.class,
      UserCommand.class,
      InternalBackendCommand.class,
      RoleCommand.class,
      PermissionCommand.class
    })
public class CliRoot implements Runnable {
  /** How the {@code --catalog} option of the commands that operate in a catalog defaults. */
  public static final String CATALOG_OPTION_DEFAULT =
      "Defaults to "
          + UnvalidatedConfig.DEFAULT_CATALOG_KEY
          + " in the configuration file or SCALAR_DB_ANALYTICS_CLIENT_DEFAULT_CATALOG";

  /** The description of the {@code --catalog} option of the commands that operate in a catalog. */
  public static final String CATALOG_OPTION_DESCRIPTION = "Catalog name. " + CATALOG_OPTION_DEFAULT;

  @Option(
      names = {"-c", "--config"},
      description = {
        "Path to the configuration file (properties format).",
        "Default: ${XDG_CONFIG_HOME}/scalardb-analytics/client.properties.",
        "Can be overridden by ${SCALAR_DB_ANALYTICS_CONFIG_PATH}."
      })
  @Nullable
  private Path configPath;

  private final Printer printer;

  @Nullable private Modules modules;
  @Nullable private AppConfig config;

  public CliRoot() {
    Formatter formatter = new JsonFormatter();
    this.printer = new ConsolePrinter(formatter);
  }

  @VisibleForTesting
  CliRoot(Printer printer, Modules modules) {
    this.printer = printer;
    this.modules = modules;
  }

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }

  public Printer printer() {
    return printer;
  }

  public Modules loadModules() {
    if (modules == null) {
      modules = Modules.initialize(loadConfig());
    }
    return modules;
  }

  /**
   * Returns the catalog a command operates in: the {@code --catalog} option if given, otherwise the
   * configured default catalog ({@value UnvalidatedConfig#DEFAULT_CATALOG_KEY}).
   *
   * @param catalog the value of the command's {@code --catalog} option, or {@code null} if not
   *     given
   * @param spec the spec of the command, used to report a missing catalog as a usage error
   * @return the catalog name
   * @throws ParameterException if neither the option nor a default catalog is set, with the message
   *     picocli gives for a missing required option
   */
  public String resolveCatalog(@Nullable String catalog, CommandSpec spec) {
    if (catalog != null) {
      return catalog;
    }
    String defaultCatalog;
    try {
      defaultCatalog = loadConfig().defaultCatalog();
    } catch (RuntimeException e) {
      // Still a usage error, as when --catalog was required, but with the reason the default could
      // not be read, so that a configuration error is not mistaken for an unset default.
      throw new ParameterException(
          spec.commandLine(),
          missingCatalogMessage(spec)
              + " (the default catalog could not be read: "
              + e.getMessage()
              + ")",
          e);
    }
    if (defaultCatalog != null) {
      return defaultCatalog;
    }
    throw new ParameterException(spec.commandLine(), missingCatalogMessage(spec));
  }

  // The messages picocli gives when a required --catalog is missing, as an option of the command or
  // as a member of an argument group.
  private static String missingCatalogMessage(CommandSpec spec) {
    OptionSpec option = spec.findOption("--catalog");
    String name =
        option == null ? "--catalog=<catalog>" : option.longestName() + "=" + option.paramLabel();
    return option != null && option.group() != null
        ? "Error: Missing required argument(s): " + name
        : "Missing required option: '" + name + "'";
  }

  private AppConfig loadConfig() {
    if (config == null) {
      if (configPath == null) {
        config = AppConfig.load();
      } else {
        if (!configPath.toFile().exists()) {
          throw new RuntimeException(
              "Configuration file not found: " + configPath.toAbsolutePath());
        }
        config = AppConfig.load(configPath);
      }
    }
    return config;
  }
}
