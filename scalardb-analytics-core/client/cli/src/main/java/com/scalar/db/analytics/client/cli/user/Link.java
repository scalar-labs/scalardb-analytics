/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.user;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.concurrent.Callable;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(name = "link", description = "Link an existing Analytics user to an existing backend user")
public class Link implements Callable<Integer> {

  @Option(
      names = {"-u", "--user"},
      description = "Analytics username (principal)",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String username;

  @Option(
      names = {"--backend-user"},
      description = "Backend username to link",
      required = true)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private String backendUsername;

  @Option(
      names = {"--backend"},
      description = "User-directory backend that holds the backend user (default: internal)",
      defaultValue = "internal",
      converter = PasswordBackendTypeConverter.class)
  @SuppressWarnings("NotNullFieldNotInitialized")
  private PasswordBackendType backend;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private UserCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      modules.client().user().linkBackendUser(username, backendUsername, backend);
      printer()
          .printSuccess(
              new Success(
                  String.format(
                      "Linked user '%s' to backend user '%s' (backend=%s)",
                      username, backendUsername, backend.name())));
      return ExitCode.OK;
    } catch (Exception e) {
      printer()
          .printError(
              new CommandError(
                  String.format(
                      "Failed to link user '%s' to backend user '%s'", username, backendUsername),
                  e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
