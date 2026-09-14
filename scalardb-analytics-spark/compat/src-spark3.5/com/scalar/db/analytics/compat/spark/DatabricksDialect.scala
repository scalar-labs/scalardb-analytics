/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.compat.spark

import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions
import org.apache.spark.sql.execution.datasources.v2.TableSampleInfo
import org.apache.spark.sql.jdbc.{JdbcDialect, JdbcType}
import org.apache.spark.sql.types._

import java.sql.Connection
import scala.collection.mutable.ArrayBuilder

/* This class adapts the specificities of the Databricks JDBC driver for Spark SQL.
 *
 * The DatabricksDialect needs to be registered when using Spark 3, but not from Spark 4, since the
 * databricks dialect is registered by default. This is a partial backport for Spark 3 of the
 * Spark 4 implementation.
 */
@SuppressWarnings(
  Array(
    "org.wartremover.warts.MutableDataStructures",
    "org.wartremover.warts.Return",
    "org.wartremover.warts.StringPlusAny"
  )
)
object DatabricksDialect extends JdbcDialect {
  def getInstance: JdbcDialect = this
  override def canHandle(url: String): Boolean =
    url.startsWith("jdbc:databricks")

  override def getCatalystType(
      sqlType: Int,
      typeName: String,
      size: Int,
      md: MetadataBuilder
  ): Option[DataType] =
    sqlType match {
      case java.sql.Types.TINYINT  => Some(ByteType)
      case java.sql.Types.SMALLINT => Some(ShortType)
      case java.sql.Types.REAL     => Some(FloatType)
      case _                       => None
    }

  override def getJDBCType(dt: DataType): Option[JdbcType] = dt match {
    case BooleanType => Some(JdbcType("BOOLEAN", java.sql.Types.BOOLEAN))
    case DoubleType  => Some(JdbcType("DOUBLE", java.sql.Types.DOUBLE))
    case StringType  => Some(JdbcType("STRING", java.sql.Types.VARCHAR))
    case BinaryType  => Some(JdbcType("BINARY", java.sql.Types.BINARY))
    case _           => None
  }

  override def quoteIdentifier(colName: String): String =
    s"`$colName`"

  override def supportsLimit: Boolean = true

  override def supportsOffset: Boolean = true

  override def supportsTableSample: Boolean = true

  override def getTableSample(sample: TableSampleInfo): String =
    s"TABLESAMPLE (${(sample.upperBound - sample.lowerBound) * 100}) REPEATABLE (${sample.seed})"

  // Override listSchemas to run "show schemas" as a PreparedStatement instead of
  // invoking getMetaData.getSchemas as it may not work correctly in older versions of the driver.
  override def schemasExists(
      conn: Connection,
      options: JDBCOptions,
      schema: String
  ): Boolean = {
    val stmt = conn.prepareStatement("SHOW SCHEMAS")
    val rs   = stmt.executeQuery()
    while (rs.next())
      if (rs.getString(1) == schema) {
        return true
      }
    false
  }

  // Override listSchemas to run "show schemas" as a PreparedStatement instead of
  // invoking getMetaData.getSchemas as it may not work correctly in older versions of the driver.
  override def listSchemas(
      conn: Connection,
      options: JDBCOptions
  ): Array[Array[String]] = {
    val schemaBuilder = ArrayBuilder.make[Array[String]]
    val stmt          = conn.prepareStatement("SHOW SCHEMAS")
    val rs            = stmt.executeQuery()
    while (rs.next())
      schemaBuilder += Array(rs.getString(1))
    schemaBuilder.result()
  }
}
