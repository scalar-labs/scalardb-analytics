/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.user;

import com.scalar.db.analytics.api.auth.UserDetail;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Failure;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParentCommand;

@Command(
    name = "describe",
    description = {
      "Describe a user with assigned roles",
      "Either --user-id or --user must be specified"
    },
    synopsisHeading = "",
    customSynopsis = {
      "Usage:",
      "  describe -i <user_id>",
      "  describe -u <user>",
    })
public class Describe implements Callable<Integer> {

  static class IdOrNameOption {
    @Option(
        names = {"-i", "--user-id"},
        description = "User ID")
    @Nullable UUID userId;

    @Option(
        names = {"-u", "--user"},
        description = "Username")
    @Nullable String username;
  }

  @ArgGroup(exclusive = true, multiplicity = "1")
  @SuppressWarnings("NotNullFieldNotInitialized")
  private IdOrNameOption idOrName;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private UserCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      Optional<UserDetail> detail;
      String identifier;

      if (idOrName.userId != null) {
        identifier = idOrName.userId.toString();
        detail = modules.client().user().describeUserById(idOrName.userId);
      } else {
        // The arg group is exclusive with multiplicity 1, so the username is set when the user ID
        // is
        // not.
        String username = Objects.requireNonNull(idOrName.username);
        identifier = username;
        detail = modules.client().user().describeUserByName(username);
      }

      if (!detail.isPresent()) {
        printer().printFailure(new Failure(String.format("User was not found: %s", identifier)));
        return ExitCode.USAGE;
      }

      printer().printSuccess(new Success(detail.get()));
      return ExitCode.OK;
    } catch (Exception e) {
      printer().printError(new CommandError("Failed to describe user", e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }
}
