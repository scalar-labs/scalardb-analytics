/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import com.scalar.db.analytics.spark.lib.catalog.{CatalogIdentifier, CatalogNamespace}
import org.apache.hadoop.io.{LongWritable, Text}
import org.apache.hadoop.mapred.TextInputFormat
import org.apache.spark.sql.types.{IntegerType, StringType, StructField, StructType}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class InputFormatTableWrapperTest extends AnyFunSuite with Matchers {

  private val schema = StructType(
    Seq(StructField("col1", StringType), StructField("col2", IntegerType))
  )

  private val identifier = CatalogIdentifier(CatalogNamespace("ds", List("ns")), "tbl")

  private def createTable(id: CatalogIdentifier): InputFormatTableWrapper[LongWritable, Text] =
    // A distinct convert function and configurator per instance, as each load creates them anew
    new InputFormatTableWrapper(
      classOf[TextInputFormat],
      schema,
      _ => Array.empty,
      _ => (),
      id.table,
      id
    )

  test("equals should return true for independently created tables with the same identifier") {
    createTable(identifier) shouldEqual
      createTable(CatalogIdentifier(CatalogNamespace("ds", List("ns")), "tbl"))
  }

  test("hashCode should be the same for equal tables") {
    createTable(identifier).hashCode() shouldEqual createTable(identifier).hashCode()
  }

  test("equals should return false for the same table name in another data source") {
    createTable(identifier) should not equal
      createTable(CatalogIdentifier(CatalogNamespace("other_ds", List("ns")), "tbl"))
  }

  test("equals should return false for the same table name in another namespace") {
    createTable(identifier) should not equal
      createTable(CatalogIdentifier(CatalogNamespace("ds", List("other_ns")), "tbl"))
  }

  test("equals should not confuse identifiers whose joined names are the same") {
    // "a.b" + "c" + "t" and "a" + "b.c" + "t" both read a.b.c.t
    createTable(CatalogIdentifier(CatalogNamespace("a.b", List("c")), "t")) should not equal
      createTable(CatalogIdentifier(CatalogNamespace("a", List("b.c")), "t"))
  }

  test("name should be the table name, not the identifier") {
    createTable(identifier).name() shouldEqual "tbl"
  }

  test("equals should return false for null") {
    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    val result = createTable(identifier).equals(None.orNull) // scalastyle:ignore null
    result shouldBe false
  }

  test("equals should return false for a different type") {
    createTable(identifier) should not equal "not a table"
  }
}
