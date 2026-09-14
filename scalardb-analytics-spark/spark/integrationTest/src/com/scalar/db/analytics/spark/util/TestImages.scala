/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.util

import org.testcontainers.utility.DockerImageName

import java.time.Duration

object TestImages {
  val MYSQL_80: DockerImageName = DockerImageName.parse("mysql:8.0.36")
  val MYSQL_84: DockerImageName = DockerImageName.parse("mysql:8.4.3")

  val POSTGRESQL_16: DockerImageName = DockerImageName.parse("postgres:16.4")
  val POSTGRESQL_17: DockerImageName = DockerImageName.parse("postgres:17.2")
  val POSTGRESQL_18: DockerImageName = DockerImageName.parse("postgres:18.1")

  val SQL_SERVER_2017: DockerImageName =
    DockerImageName.parse("mcr.microsoft.com/mssql/server:2017-latest")
  val SQL_SERVER_2019: DockerImageName =
    DockerImageName.parse("mcr.microsoft.com/mssql/server:2019-latest")
  val SQL_SERVER_2022: DockerImageName =
    DockerImageName.parse("mcr.microsoft.com/mssql/server:2022-latest")

  /** Oracle Database prebuilt images from Scalar Labs.
    *
    * Note: Oracle 19c/21c do not support BOOLEAN data type in SQL (introduced in 23c).
    */
  val ORACLE_19: DockerImageName =
    DockerImageName.parse("ghcr.io/scalar-labs/oracle/db-prebuilt:19")

  val ORACLE_21: DockerImageName =
    DockerImageName.parse("ghcr.io/scalar-labs/oracle/db-prebuilt:21")

  val ORACLE_23: DockerImageName =
    DockerImageName.parse("ghcr.io/scalar-labs/oracle/db-prebuilt:23")

  val DYNAMODB_LOCAL: DockerImageName =
    DockerImageName.parse("amazon/dynamodb-local:2.5.4")

  // We need to longer startup timeout to avoid flaky tests on CI, especially on GitHub
  // Actions, from the default 60 seconds.
  // Oracle images are very large (several GB) and can take a long time to pull.
  // Note that we need to specify the timeout for each container creation by calling
  // `.withStartupTimeout(TestImages.ORACLE_STARTUP_TIMEOUT)`.
  val ORACLE_STARTUP_TIMEOUT: Duration = Duration.ofMinutes(10)
}
