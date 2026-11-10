/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.compat.spark

import org.apache.spark.sql.execution.datasources.jdbc.{JDBCOptions, JdbcUtils}
import org.apache.spark.sql.jdbc.JdbcDialects

/** Wraps `JdbcUtils.classifyException`, whose signature changed in Spark 4.0. */
object JdbcUtilsCompat {
  def classifyCreateIndexException[T](
      indexName: String,
      tableName: String,
      options: JDBCOptions
  )(f: => T): T =
    JdbcUtils.classifyException(
      s"Failed to create index $indexName in $tableName",
      JdbcDialects.get(options.url)
    )(f)

  def classifyDropIndexException[T](
      indexName: String,
      tableName: String,
      options: JDBCOptions
  )(f: => T): T =
    JdbcUtils.classifyException(
      s"Failed to drop index $indexName in $tableName",
      JdbcDialects.get(options.url)
    )(f)
}
