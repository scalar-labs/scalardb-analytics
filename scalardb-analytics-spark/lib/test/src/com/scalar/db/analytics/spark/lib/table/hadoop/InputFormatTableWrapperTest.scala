/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapred.{InputFormat, SequenceFileInputFormat, TextInputFormat}
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class InputFormatTableWrapperTest extends AnyFunSuite with Matchers {

  private val schema1 = StructType(
    Seq(StructField("col1", StringType), StructField("col2", IntegerType))
  )
  private val schema2 = StructType(
    Seq(StructField("col1", StringType), StructField("col2", IntegerType))
  )
  private val differentSchema = StructType(
    Seq(StructField("col1", StringType))
  )

  private val tableName = "ds.ns.tbl"

  private val textInputFormat: Class[_ <: InputFormat[LongWritable, Text]] =
    classOf[TextInputFormat]

  private def createTable(
      cls: Class[_ <: InputFormat[LongWritable, Text]],
      schema: StructType,
      name: String
  ): InputFormatTableWrapper[LongWritable, Text] =
    // A distinct convert function and configurator per instance, as each load creates them anew
    new InputFormatTableWrapper(cls, schema, _ => Array.empty, _ => (), name)

  test("equals should return true for tables with the same input format, schema, and name") {
    createTable(textInputFormat, schema1, tableName) shouldEqual
      createTable(textInputFormat, schema2, tableName)
  }

  test("hashCode should be the same for equal tables") {
    createTable(textInputFormat, schema1, tableName).hashCode() shouldEqual
      createTable(textInputFormat, schema2, tableName).hashCode()
  }

  test("equals should return false for tables with different names") {
    createTable(textInputFormat, schema1, tableName) should not equal
      createTable(textInputFormat, schema1, "ds.ns.other")
  }

  test("equals should return false for tables with different schemas") {
    createTable(textInputFormat, schema1, tableName) should not equal
      createTable(textInputFormat, differentSchema, tableName)
  }

  test("equals should return false for tables with different input formats") {
    createTable(textInputFormat, schema1, tableName) should not equal
      createTable(classOf[SequenceFileInputFormat[LongWritable, Text]], schema1, tableName)
  }

  test("equals should return false for null") {
    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    val result =
      createTable(textInputFormat, schema1, tableName).equals(None.orNull) // scalastyle:ignore null
    result shouldBe false
  }

  test("equals should return false for a different type") {
    createTable(textInputFormat, schema1, tableName) should not equal "not a table"
  }
}
