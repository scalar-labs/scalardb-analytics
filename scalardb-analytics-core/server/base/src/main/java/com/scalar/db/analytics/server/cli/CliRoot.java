/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "scalardb-analytics-server",
    description = "ScalarDB Analytics Server CLI",
    subcommands = {Start.class})
public class CliRoot implements Runnable {

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }
}
