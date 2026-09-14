/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapred.{InputFormat, TextInputFormat}
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class InputFormatScanTest extends AnyFunSuite with Matchers {

  private val schema1 = StructType(
    Seq(StructField("col1", StringType), StructField("col2", IntegerType))
  )
  private val schema2 = StructType(
    Seq(StructField("col1", StringType), StructField("col2", IntegerType))
  )
  private val differentSchema = StructType(
    Seq(StructField("col1", StringType))
  )

  private val dummyConvert: RecordKeyValue[LongWritable, Text] => Array[Any] =
    _ => Array.empty

  private val dummyConfigurator: InputFormatJobConfConfigurator = _ => ()

  private def createScan(
      cls: Class[_ <: InputFormat[LongWritable, Text]],
      schema: StructType
  ): InputFormatScan[LongWritable, Text] =
    new InputFormatScan(cls, schema, dummyConvert, dummyConfigurator)

  test(
    "equals should return true for scans with the same inputFormatClass and schema"
  ) {
    val scan1 = createScan(classOf[TextInputFormat], schema1)
    val scan2 = createScan(classOf[TextInputFormat], schema2)
    scan1 shouldEqual scan2
  }

  test(
    "hashCode should be the same for scans with the same inputFormatClass and schema"
  ) {
    val scan1 = createScan(classOf[TextInputFormat], schema1)
    val scan2 = createScan(classOf[TextInputFormat], schema2)
    scan1.hashCode() shouldEqual scan2.hashCode()
  }

  test("equals should return false for scans with different schemas") {
    val scan1 = createScan(classOf[TextInputFormat], schema1)
    val scan2 = createScan(classOf[TextInputFormat], differentSchema)
    scan1 should not equal scan2
  }

  test("equals should return false for null") {
    val scan = createScan(classOf[TextInputFormat], schema1)
    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    val result = scan.equals(None.orNull) // scalastyle:ignore null
    result shouldBe false
  }

  test("equals should return false for a different type") {
    val scan = createScan(classOf[TextInputFormat], schema1)
    scan should not equal "not a scan"
  }
}
