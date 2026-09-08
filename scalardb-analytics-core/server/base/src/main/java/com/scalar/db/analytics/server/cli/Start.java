/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.cli;

import com.scalar.db.analytics.server.ScalarDbAnalyticsServerApplication;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "start", description = "Start the ScalarDB Analytics server")
public class Start implements Callable<Integer> {

  @Option(
      names = {"-c", "--config"},
      description = "Path to the configuration file")
  @Nullable
  private Path configFile;

  @Override
  public Integer call() {
    List<String> args = new ArrayList<>();

    // If config file specified, pass to Spring Boot
    if (configFile != null) {
      args.add("--spring.config.location=file:" + configFile.toAbsolutePath());
    }

    // Spring Boot will handle:
    // 1. Loading from specified config file (if provided)
    // 2. Environment variables (SCALAR_DB_ANALYTICS_* automatically mapped)
    // 3. Default application.properties from classpath

    try {
      ScalarDbAnalyticsServerApplication.run(args.toArray(new String[0]));
      return 0;
    } catch (Exception e) {
      // Spring Boot already logs the error details to stderr:
      // - FailureAnalyzer handles specific errors (e.g., database connection)
      // - Other errors are logged with full stack trace by Spring Boot
      // We just need to ensure non-zero exit code for CLI
      return 1;
    }
  }
}
