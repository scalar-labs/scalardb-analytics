/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli;

import com.scalar.db.analytics.client.cli.Formatter.JsonFormatter;
import com.scalar.db.analytics.client.cli.Printer.StringPrinter;
import com.scalar.db.analytics.client.module.Modules;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Objects;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class CliTestBase {
  @SuppressWarnings("NotNullFieldNotInitialized")
  protected Modules modules;

  @SuppressWarnings("NotNullFieldNotInitialized")
  protected StringPrinter printer;

  @SuppressWarnings("NotNullFieldNotInitialized")
  protected CliRoot cliRoot;

  @SuppressWarnings("NotNullFieldNotInitialized")
  private ByteArrayOutputStream stdout;

  @SuppressWarnings("NotNullFieldNotInitialized")
  private ByteArrayOutputStream stderr;

  @SuppressWarnings("NotNullFieldNotInitialized")
  private PrintStream originalStdout;

  @SuppressWarnings("NotNullFieldNotInitialized")
  private PrintStream originalStderr;

  @BeforeEach
  void setUp() {
    modules = MockModules.create();
    printer = new StringPrinter(new JsonFormatter(), new StringBuilder());
    cliRoot = new CliRoot(printer, modules);
    originalStdout = System.out;
    originalStderr = System.err;
    stdout = new ByteArrayOutputStream();
    stderr = new ByteArrayOutputStream();
    try {
      System.setOut(new PrintStream(stdout, true, StandardCharsets.UTF_8.name()));
      System.setErr(new PrintStream(stderr, true, StandardCharsets.UTF_8.name()));
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  @AfterEach
  void tearDown() {
    System.setOut(originalStdout);
    System.setErr(originalStderr);
  }

  protected String getConfigPath() {
    try {
      return Paths.get(
              Objects.requireNonNull(getClass().getClassLoader().getResource("client.properties"))
                  .toURI())
          .toAbsolutePath()
          .toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  protected String[] args(String... commandArgs) {
    String[] args = new String[commandArgs.length + 2];
    args[0] = "--config";
    args[1] = getConfigPath();
    System.arraycopy(commandArgs, 0, args, 2, commandArgs.length);
    return args;
  }

  protected String getStdout() {
    try {
      return stdout.toString(StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }

  protected String getStderr() {
    try {
      return stderr.toString(StandardCharsets.UTF_8.name());
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException(e);
    }
  }
}
