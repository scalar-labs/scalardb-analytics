/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.util

import org.testcontainers.containers
import org.testcontainers.utility.DockerImageName

import scala.annotation.nowarn

/** Scala wrappers for testcontainers-java container classes.
  *
  * These wrappers are necessary because some testcontainers-java containers use recursive generic
  * types (the "Curiously Recurring Template Pattern" or CRTP) which causes type inference issues in
  * Scala.
  *
  * For example, MySQLContainer is defined as:
  * {{{
  * public class MySQLContainer<SELF extends MySQLContainer<SELF>>
  *     extends JdbcDatabaseContainer<SELF>
  * }}}
  *
  * When using this directly from Scala with `new MySQLContainer(image)`, the type parameter SELF
  * cannot be inferred, causing method chaining to fail with "value withDatabaseName is not a member
  * of Nothing" errors.
  *
  * By creating concrete Scala classes that specify themselves as the type parameter (e.g.,
  * `MySQLContainer[MySQLContainer]`), we make the type concrete and enable proper type inference
  * for method chaining.
  *
  * Note: OracleContainer does not require this wrapper because it doesn't use the CRTP pattern -
  * it's defined simply as `public class OracleContainer extends
  * JdbcDatabaseContainer<OracleContainer>` without a generic type parameter on the class itself,
  * making it directly usable from Scala.
  *
  * These wrappers also provide Scala-friendly accessor methods (e.g., `username` instead of
  * `getUsername()`) for better ergonomics.
  */
object TestContainers {
  trait GenericContainerUtil {
    self: containers.GenericContainer[_] =>

    def mappedPort(port: Int): Int = getMappedPort(port)
  }
  trait JdbcDatabaseContainerUtil extends GenericContainerUtil {
    self: containers.JdbcDatabaseContainer[_] =>

    def username: String     = getUsername
    def password: String     = getPassword
    def databaseName: String = getDatabaseName
  }

  class GenericContainer(imageName: DockerImageName)
      extends containers.GenericContainer[GenericContainer](imageName)
      with GenericContainerUtil

  @nowarn("cat=deprecation")
  class PostgreSQLContainer(imageName: DockerImageName)
      extends containers.PostgreSQLContainer[PostgreSQLContainer](imageName)
      with JdbcDatabaseContainerUtil

  @nowarn("cat=deprecation")
  class MySQLContainer(imageName: DockerImageName)
      extends containers.MySQLContainer[MySQLContainer](imageName)
      with JdbcDatabaseContainerUtil {
    override def getDriverClassName: String = "org.mariadb.jdbc.Driver"
    override def getJdbcUrl: String = {
      val url       = super.getJdbcUrl
      val separator = if (url.contains("?")) "&" else "?"
      s"$url${separator}permitMysqlScheme=true"
    }
  }

  @nowarn("cat=deprecation")
  class MSSQLServerContainer(imageName: DockerImageName)
      extends containers.MSSQLServerContainer[MSSQLServerContainer](imageName)
      with JdbcDatabaseContainerUtil
}
