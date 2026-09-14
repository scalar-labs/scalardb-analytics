/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.databricks

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks
import com.zaxxer.hikari.HikariDataSource

/** Databricks integration test environment configuration.
  *
  * Reads Databricks connection settings from environment variables and creates a Databricks
  * provider and HikariDataSource for testing.
  *
  * All fields are lazy vals to defer environment variable access until actually needed.
  */
class DatabricksEnv {
  lazy val host: String = DatabricksEnv.getEnvironmentVariable(
    DatabricksEnv.PREFIX + "HOST",
    None
  )
  lazy val port: String = DatabricksEnv.getEnvironmentVariable(
    DatabricksEnv.PREFIX + "PORT",
    Some("443")
  )
  lazy val httpPath: String = DatabricksEnv.getEnvironmentVariable(
    DatabricksEnv.PREFIX + "HTTP_PATH",
    None
  )
  lazy val oAuthClientId: String = DatabricksEnv.getEnvironmentVariable(
    DatabricksEnv.PREFIX + "O_AUTH_CLIENT_ID",
    None
  )
  lazy val oAuthClientSecret: String = DatabricksEnv.getEnvironmentVariable(
    DatabricksEnv.PREFIX + "O_AUTH_SECRET",
    None
  )
  lazy val catalogSuffix: String = DatabricksEnv.getEnvironmentVariable(
    DatabricksEnv.PREFIX + "CATALOG_SUFFIX",
    Some(System.currentTimeMillis().toString)
  )
  lazy val catalog: Option[String] = sys.env.get(DatabricksEnv.PREFIX + "CATALOG")

  lazy val databricks: Databricks = new Databricks(
    host,
    Integer.valueOf(port),
    httpPath,
    oAuthClientId,
    oAuthClientSecret,
    catalog.orNull
  )

  lazy val dataSource: HikariDataSource = {
    val ds = new HikariDataSource()
    ds.setDriverClassName(databricks.getDriverClassName)
    ds.setJdbcUrl(databricks.getUrl)
    ds.setDataSourceProperties(databricks.getProperties)
    ds
  }
}

object DatabricksEnv {
  val ANALYTICS_INT_TEST_ENV = "ANALYTICS_INT_TEST_ENV"
  private val PREFIX         = "ANALYTICS_INT_TEST_ENV_DATABRICKS_"

  /** Gets an environment variable.
    *
    * @param key
    *   environment variable key
    * @param defaultValue
    *   default value if the environment variable is not set
    * @return
    *   environment variable value or default value
    * @throws IllegalArgumentException
    *   if the value is None or empty
    */
  private def getEnvironmentVariable(key: String, defaultValue: Option[String]): String =
    sys.env
      .get(key)
      .orElse(defaultValue)
      .filter(_.nonEmpty)
      .getOrElse(
        throw new IllegalArgumentException(
          s"Databricks integration test environment variable '$key' is not set"
        )
      )
}
