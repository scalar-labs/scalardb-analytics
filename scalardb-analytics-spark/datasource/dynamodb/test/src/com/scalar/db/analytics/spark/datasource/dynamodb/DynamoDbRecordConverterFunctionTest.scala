/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.dynamodb

import com.scalar.db.analytics.api.model.{Column, DataType, TableDetail, TableInfo}
import com.scalar.db.analytics.spark.lib.table.hadoop.RecordKeyValue
import org.apache.hadoop.dynamodb.DynamoDBItemWritable
import org.apache.hadoop.io.Text
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import software.amazon.awssdk.core.SdkBytes
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import java.math.BigDecimal
import java.util.UUID
import scala.jdk.CollectionConverters._

class DynamoDbRecordConverterFunctionTest extends AnyFlatSpec with Matchers {

  "DynamoDbRecordConverter" should "convert DynamoDB record to object array" in {
    val tableId     = UUID.randomUUID()
    val namespaceId = UUID.randomUUID()
    val tableInfo   = TableInfo.create(namespaceId, "test_table")
    val tableDetail = new TableDetail(
      tableInfo,
      Seq(
        Column.create(tableId, "int_col", DataType.Int.INSTANCE, 1, false),
        Column.create(tableId, "text_col", DataType.Text.INSTANCE, 2, true),
        Column.create(tableId, "bool_col", DataType.Boolean.INSTANCE, 3, false)
      ).asJava
    )

    val converter = new DynamoDbRecordConverter(tableDetail)

    // Create DynamoDB item
    val item = Map(
      "int_col"  -> AttributeValue.builder().n("123").build(),
      "text_col" -> AttributeValue.builder().s("test value").build(),
      "bool_col" -> AttributeValue.builder().bool(true).build()
    ).asJava

    val itemWritable = new DynamoDBItemWritable()
    itemWritable.setItem(item)

    val keyValue = RecordKeyValue(new Text("key"), itemWritable)
    val result   = converter.convert(keyValue)

    result should have length 3
    result(0) should be(123)
    result(1) should be("test value")
    result(2) shouldBe true
  }

  it should "handle missing columns as null" in {
    val tableId     = UUID.randomUUID()
    val namespaceId = UUID.randomUUID()
    val tableInfo   = TableInfo.create(namespaceId, "test_table")
    val tableDetail = new TableDetail(
      tableInfo,
      Seq(
        Column.create(tableId, "int_col", DataType.Int.INSTANCE, 1, false),
        Column.create(tableId, "missing_col", DataType.Text.INSTANCE, 2, true)
      ).asJava
    )

    val converter = new DynamoDbRecordConverter(tableDetail)

    // Create DynamoDB item with only one column
    val item = Map(
      "int_col" -> AttributeValue.builder().n("456").build()
    ).asJava

    val itemWritable = new DynamoDBItemWritable()
    itemWritable.setItem(item)

    val keyValue = RecordKeyValue(new Text("key"), itemWritable)
    val result   = converter.convert(keyValue)

    result should have length 2
    result(0) should be(456)
    // Testing that missing columns are converted to null as expected by Spark
    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    val nullValue: Any = null
    result(1) shouldBe nullValue
  }

  it should "convert all numeric types correctly" in {
    val tableId     = UUID.randomUUID()
    val namespaceId = UUID.randomUUID()
    val tableInfo   = TableInfo.create(namespaceId, "test_table")
    val tableDetail = new TableDetail(
      tableInfo,
      Seq(
        Column.create(tableId, "byte_col", DataType.Byte.INSTANCE, 1, false),
        Column.create(tableId, "short_col", DataType.SmallInt.INSTANCE, 2, false),
        Column.create(tableId, "long_col", DataType.BigInt.INSTANCE, 3, false),
        Column.create(tableId, "float_col", DataType.Float.INSTANCE, 4, false),
        Column.create(tableId, "double_col", DataType.Double.INSTANCE, 5, false),
        Column.create(tableId, "decimal_col", DataType.Decimal.DEFAULT_INSTANCE, 6, false)
      ).asJava
    )

    val converter = new DynamoDbRecordConverter(tableDetail)

    val item = Map(
      "byte_col"    -> AttributeValue.builder().n("127").build(),
      "short_col"   -> AttributeValue.builder().n("32767").build(),
      "long_col"    -> AttributeValue.builder().n("9223372036854775807").build(),
      "float_col"   -> AttributeValue.builder().n("3.14").build(),
      "double_col"  -> AttributeValue.builder().n("2.718281828").build(),
      "decimal_col" -> AttributeValue.builder().n("123.456").build()
    ).asJava

    val itemWritable = new DynamoDBItemWritable()
    itemWritable.setItem(item)

    val keyValue = RecordKeyValue(new Text("key"), itemWritable)
    val result   = converter.convert(keyValue)

    result should have length 6
    result(0) should be(127.toByte)
    result(1) should be(32767.toShort)
    result(2) should be(9223372036854775807L)
    result(3) should be(3.14f)
    result(4) should be(2.718281828)
    result(5) shouldBe a[BigDecimal]
    // Cast is safe here - we just verified it's a BigDecimal above
    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    val bigDecimalValue = result(5).asInstanceOf[BigDecimal]
    bigDecimalValue.toString should be("123.456")
  }

  it should "convert blob data correctly" in {
    val tableId     = UUID.randomUUID()
    val namespaceId = UUID.randomUUID()
    val tableInfo   = TableInfo.create(namespaceId, "test_table")
    val tableDetail = new TableDetail(
      tableInfo,
      Seq(
        Column.create(tableId, "blob_col", DataType.Blob.INSTANCE, 1, false)
      ).asJava
    )

    val converter = new DynamoDbRecordConverter(tableDetail)

    val binaryData = "Hello World".getBytes("UTF-8")
    val item = Map(
      "blob_col" -> AttributeValue.builder().b(SdkBytes.fromByteArray(binaryData)).build()
    ).asJava

    val itemWritable = new DynamoDBItemWritable()
    itemWritable.setItem(item)

    val keyValue = RecordKeyValue(new Text("key"), itemWritable)
    val result   = converter.convert(keyValue)

    result should have length 1
    // Check that result(0) is an Array[Byte] by attempting to use it as one
    @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
    val byteArray = result(0).asInstanceOf[Array[Byte]]
    new String(byteArray, "UTF-8") should be("Hello World")
  }

  it should "throw UnsupportedOperationException for time-related types" in {
    val tableId     = UUID.randomUUID()
    val namespaceId = UUID.randomUUID()
    val tableInfo   = TableInfo.create(namespaceId, "test_table")
    val tableDetail = new TableDetail(
      tableInfo,
      Seq(
        Column.create(tableId, "date_col", DataType.Date.INSTANCE, 1, false)
      ).asJava
    )
    an[UnsupportedOperationException] should be thrownBy new DynamoDbRecordConverter(tableDetail)
  }
}
