/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.converter

import com.scalar.db.analytics.api.model.{
  Column,
  DataSource,
  DataSourceNamespaceTableDetail,
  DataType,
  Namespace,
  TableDetail,
  TableInfo
}
import org.apache.spark.sql.types._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

import java.util.UUID
import scala.jdk.CollectionConverters._

class ProtoToSparkConverterTest extends AnyFunSuite with Matchers {

  test("convertDataType should handle all API data types") {
    ProtoToSparkConverter.convertDataType(DataType.Byte.INSTANCE) shouldBe ByteType
    ProtoToSparkConverter.convertDataType(DataType.SmallInt.INSTANCE) shouldBe ShortType
    ProtoToSparkConverter.convertDataType(DataType.Int.INSTANCE) shouldBe IntegerType
    ProtoToSparkConverter.convertDataType(DataType.BigInt.INSTANCE) shouldBe LongType
    ProtoToSparkConverter.convertDataType(DataType.Float.INSTANCE) shouldBe FloatType
    ProtoToSparkConverter.convertDataType(DataType.Double.INSTANCE) shouldBe DoubleType
    ProtoToSparkConverter.convertDataType(DataType.Text.INSTANCE) shouldBe StringType
    ProtoToSparkConverter.convertDataType(DataType.Boolean.INSTANCE) shouldBe BooleanType
    ProtoToSparkConverter.convertDataType(DataType.Date.INSTANCE) shouldBe DateType
    ProtoToSparkConverter.convertDataType(DataType.Timestamp.INSTANCE) shouldBe TimestampType
  }

  test("convertColumn should create proper StructField") {
    val tableId = UUID.randomUUID()
    val column  = Column.create(tableId, "test_column", DataType.Int.INSTANCE, 1, true)

    val structField = ProtoToSparkConverter.convertColumn(column)

    structField.name shouldBe "test_column"
    structField.dataType shouldBe IntegerType
    structField.nullable shouldBe true
  }

  test("convertTableDetailToSchema should create proper StructType") {
    val tableId     = UUID.randomUUID()
    val namespaceId = UUID.randomUUID()
    val columns = java.util.Arrays.asList(
      Column.create(tableId, "id", DataType.BigInt.INSTANCE, 1, false),
      Column.create(tableId, "name", DataType.Text.INSTANCE, 2, true),
      Column.create(tableId, "age", DataType.Int.INSTANCE, 3, true)
    )

    val tableInfo   = TableInfo.create(namespaceId, "test_table")
    val tableDetail = new TableDetail(tableInfo, columns)

    // Create dummy DataSource and Namespace (not used by converter but required by API)
    val catalogId = UUID.randomUUID()
    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    val dataSource = new DataSource(UUID.randomUUID(), catalogId, "test_datasource", null)
    val namespace  = Namespace.create(dataSource.getId, List("test_namespace").asJava)
    val dataSourceTableDetail =
      new DataSourceNamespaceTableDetail(dataSource, namespace, tableDetail)

    val schema = ProtoToSparkConverter.convertTableDetailToSchema(dataSourceTableDetail)

    schema.fields should have length 3
    schema.fields(0) shouldBe StructField("id", LongType, nullable = false)
    schema.fields(1) shouldBe StructField("name", StringType, nullable = true)
    schema.fields(2) shouldBe StructField("age", IntegerType, nullable = true)
  }

  test("convertTableDetailToSchema should sort columns by ordinal position") {
    val tableId     = UUID.randomUUID()
    val namespaceId = UUID.randomUUID()
    val columns = java.util.Arrays.asList(
      Column.create(tableId, "third", DataType.Text.INSTANCE, 3, true),
      Column.create(tableId, "first", DataType.Int.INSTANCE, 1, true),
      Column.create(tableId, "second", DataType.Boolean.INSTANCE, 2, true)
    )

    val tableInfo   = TableInfo.create(namespaceId, "test_table")
    val tableDetail = new TableDetail(tableInfo, columns)

    // Create dummy DataSource and Namespace (not used by converter but required by API)
    val catalogId = UUID.randomUUID()
    @SuppressWarnings(Array("org.wartremover.warts.Null"))
    val dataSource = new DataSource(UUID.randomUUID(), catalogId, "test_datasource", null)
    val namespace  = Namespace.create(dataSource.getId, List("test_namespace").asJava)
    val dataSourceTableDetail =
      new DataSourceNamespaceTableDetail(dataSource, namespace, tableDetail)

    val schema = ProtoToSparkConverter.convertTableDetailToSchema(dataSourceTableDetail)

    schema.fields(0).name shouldBe "first"
    schema.fields(1).name shouldBe "second"
    schema.fields(2).name shouldBe "third"
  }
}
