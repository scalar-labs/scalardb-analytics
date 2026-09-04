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
import com.scalar.db.analytics.client.module.Modules;
import java.nio.file.Path;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
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
      AppConfig config = loadConfig();
      modules = Modules.initialize(config);
    }
    return modules;
  }

  private AppConfig loadConfig() {
    if (configPath == null) {
      return AppConfig.load();
    } else {
      if (!configPath.toFile().exists()) {
        throw new RuntimeException("Configuration file not found: " + configPath.toAbsolutePath());
      }
      return AppConfig.load(configPath);
    }
  }
}
