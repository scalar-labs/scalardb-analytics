/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.compat.spark

import org.apache.spark.sql.catalyst.util.{AttributeNameParser, QuotingUtils}
import org.apache.spark.sql.execution.datasources.jdbc.{JDBCOptions, JdbcUtils}
import org.apache.spark.sql.jdbc.JdbcDialects

/** Wraps `JdbcUtils.classifyException`, whose signature changed in Spark 4.0.
  *
  * The error conditions and message parameters are those that Spark's own `JDBCTable` passes.
  */
object JdbcUtilsCompat {
  def classifyCreateIndexException[T](
      indexName: String,
      tableName: String,
      options: JDBCOptions
  )(f: => T): T =
    JdbcUtils.classifyException(
      condition = "FAILED_JDBC.CREATE_INDEX",
      messageParameters = indexMessageParameters(indexName, tableName, options),
      dialect = JdbcDialects.get(options.url),
      description = s"Failed to create index $indexName in $tableName",
      isRuntime = false
    )(f)

  def classifyDropIndexException[T](
      indexName: String,
      tableName: String,
      options: JDBCOptions
  )(f: => T): T =
    JdbcUtils.classifyException(
      condition = "FAILED_JDBC.DROP_INDEX",
      messageParameters = indexMessageParameters(indexName, tableName, options),
      dialect = JdbcDialects.get(options.url),
      description = s"Failed to drop index $indexName in $tableName",
      isRuntime = false
    )(f)

  private def indexMessageParameters(
      indexName: String,
      tableName: String,
      options: JDBCOptions
  ): Map[String, String] =
    Map(
      "url"       -> options.getRedactUrl(),
      "indexName" -> toSQLId(indexName),
      "tableName" -> toSQLId(tableName)
    )

  // Spark's DataTypeErrorsBase.toSQLId, which is private to Spark.
  private def toSQLId(parts: String): String =
    AttributeNameParser.parseAttributeName(parts).map(QuotingUtils.quoteIdentifier).mkString(".")
}
