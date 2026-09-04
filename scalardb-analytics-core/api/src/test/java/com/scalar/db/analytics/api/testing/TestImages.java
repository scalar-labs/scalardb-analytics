/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.testing;

import java.time.Duration;
import org.testcontainers.utility.DockerImageName;

public class TestImages {
  public static final DockerImageName MYSQL_80 = DockerImageName.parse("mysql:8.0.36");

  public static final DockerImageName POSTGRESQL_16 = DockerImageName.parse("postgres:16.4");

  public static final DockerImageName SQL_SERVER_2019 =
      DockerImageName.parse("mcr.microsoft.com/mssql/server:2019-latest");

  /**
   * We must use this image because testcontainer-oracle requires the images to have a particular
   * configuration, and this is the only one that works as far as I know.
   */
  public static final DockerImageName ORACLE_23 =
      DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart");

  public static final DockerImageName DYNAMODB_LOCAL =
      DockerImageName.parse("amazon/dynamodb-local:2.5.4");

  // We need to longer startup timeout to avoid flaky tests on CI, especially on GitHub
  // Actions, from the default 60 seconds.
  // Note that we need to specify the timeout for each container creation by calling
  // `.withStartupTimeout(TestImages.ORACLE_STARTUP_TIMEOUT)`.
  public static final Duration ORACLE_STARTUP_TIMEOUT = Duration.ofMinutes(2);
}
