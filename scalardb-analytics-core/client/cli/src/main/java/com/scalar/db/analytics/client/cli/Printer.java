/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli;

import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Failure;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import picocli.CommandLine.Help.Ansi;

/** Defines an interface for printing the results of a command execution. */
public interface Printer {
  void printSuccess(Success result);

  void printFailure(Failure error);

  void printError(CommandError error);

  /** A {@link Printer} that prints the results of a command execution to the console. */
  class ConsolePrinter implements Printer {
    private final Formatter formatter;

    public ConsolePrinter(Formatter formatter) {
      this.formatter = formatter;
    }

    public Formatter formatter() {
      return formatter;
    }

    @Override
    public void printSuccess(CommandResult.Success result) {
      System.out.println(formatter.format(result));
    }

    @Override
    public void printFailure(Failure error) {
      String msg = Ansi.AUTO.string(String.format("@|bold,red, %s|@", error.message()));
      System.err.println(msg);
    }

    @Override
    public void printError(CommandError error) {
      String msg = Ansi.AUTO.string(String.format("@|bold,red, %s|@", error.message()));
      System.err.println(msg);
      error.exception().printStackTrace(System.err);
    }
  }

  /**
   * A {@link Printer} that prints the results of a command execution to a {@link StringBuilder}.
   * For {@link CommandError}, an exception is rethrown. This is used for testing.
   */
  class StringPrinter implements Printer {
    private final Formatter formatter;
    private final StringBuilder sb;

    public StringPrinter(Formatter formatter, StringBuilder sb) {
      this.formatter = formatter;
      this.sb = sb;
    }

    public Formatter formatter() {
      return formatter;
    }

    public StringBuilder sb() {
      return sb;
    }

    @Override
    public void printSuccess(CommandResult.Success result) {
      sb.append(formatter.format(result));
    }

    @Override
    public void printFailure(Failure error) {
      sb.append(error.message());
    }

    @Override
    public void printError(CommandError error) {
      throw new RuntimeException(error.exception());
    }

    public String getOutput() {
      return sb.toString();
    }

    @Override
    public String toString() {
      return sb.toString();
    }
  }
}
