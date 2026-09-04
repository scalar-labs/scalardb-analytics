/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.snowflake

import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake
import com.zaxxer.hikari.HikariDataSource

/** Snowflake integration test environment configuration.
  *
  * Reads Snowflake connection settings from environment variables and creates a Snowflake provider
  * and HikariDataSource for testing.
  *
  * All fields are lazy vals to defer environment variable access until actually needed.
  */
class SnowflakeEnv {
  lazy val account: String = SnowflakeEnv.getEnvironmentVariable(
    SnowflakeEnv.PREFIX + "ACCOUNT",
    None
  )
  lazy val username: String = SnowflakeEnv.getEnvironmentVariable(
    SnowflakeEnv.PREFIX + "USERNAME",
    None
  )
  lazy val password: String = SnowflakeEnv.getEnvironmentVariable(
    SnowflakeEnv.PREFIX + "PASSWORD",
    None
  )
  lazy val database: Option[String] = sys.env.get(SnowflakeEnv.PREFIX + "DATABASE")
  lazy val databaseSuffix: String = SnowflakeEnv.getEnvironmentVariable(
    SnowflakeEnv.PREFIX + "DATABASE_SUFFIX",
    Some(System.currentTimeMillis().toString)
  )

  lazy val snowflake: Snowflake = new Snowflake(account, username, password, database.orNull)

  lazy val dataSource: HikariDataSource = {
    val ds = new HikariDataSource()
    ds.setDriverClassName(snowflake.getDriverClassName)
    ds.setJdbcUrl(snowflake.getUrl)
    ds.setDataSourceProperties(snowflake.getProperties)
    ds
  }
}

object SnowflakeEnv {
  val ANALYTICS_INT_TEST_ENV = "ANALYTICS_INT_TEST_ENV"
  private val PREFIX         = "ANALYTICS_INT_TEST_ENV_SNOWFLAKE_"

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
          s"Snowflake integration test environment variable '$key' is not set"
        )
      )
}
