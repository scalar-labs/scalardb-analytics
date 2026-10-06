/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli;

import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Failure;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import picocli.CommandLine.Help.Ansi;
import picocli.CommandLine.Help.Ansi.Style;

/** Defines an interface for printing the results of a command execution. */
public interface Printer {
  void printSuccess(Success result);

  void printFailure(Failure error);

  void printError(CommandError error);

  /**
   * Prints a warning about a command that succeeded. Warnings are kept apart from the result, so
   * that a caller that parses the result is not affected by them.
   */
  void printWarning(String message);

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

    @Override
    public void printWarning(String message) {
      // The message can carry names read from a data source, so it is wrapped in style codes
      // rather than passed through picocli markup, which would misread a name containing "|@".
      if (Ansi.AUTO.enabled()) {
        System.err.println(Style.fg_yellow.on() + message + Style.fg_yellow.off());
      } else {
        System.err.println(message);
      }
    }
  }

  /**
   * A {@link Printer} that prints the results of a command execution to a {@link StringBuilder}.
   * For {@link CommandError}, an exception is rethrown. This is used for testing.
   */
  class StringPrinter implements Printer {
    private final Formatter formatter;
    private final StringBuilder sb;
    private final List<String> warnings = new ArrayList<>();

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

    /** Records the warning apart from the output, as the console printer writes it to stderr. */
    @Override
    public void printWarning(String message) {
      warnings.add(message);
    }

    public String getOutput() {
      return sb.toString();
    }

    public List<String> getWarnings() {
      return Collections.unmodifiableList(warnings);
    }

    @Override
    public String toString() {
      return sb.toString();
    }
  }
}
