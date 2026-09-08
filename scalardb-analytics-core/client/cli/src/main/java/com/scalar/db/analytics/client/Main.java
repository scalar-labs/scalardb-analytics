/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client;

import com.scalar.db.analytics.client.cli.CliRoot;
import picocli.CommandLine;

public class Main {
  public static void main(String[] args) {
    int exitCode = new CommandLine(new CliRoot()).execute(args);
    System.exit(exitCode);
  }
}
