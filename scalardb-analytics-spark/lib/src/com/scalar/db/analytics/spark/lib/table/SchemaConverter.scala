/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table

import com.scalar.db.analytics.api.model.{Column => AnalyticsColumn, DataType, TableDetail}
import org.apache.spark.sql.connector.catalog.Column
import org.apache.spark.sql.types.{DataType => SparkDataType, _}

import scala.jdk.CollectionConverters._

/** Utility for converting between proto/API schema and Spark schema.
  */
object SchemaConverter {

  def toSparkColumns(tableDetail: TableDetail): Array[Column] =
    tableDetail.getColumns.asScala.map { col =>
      Column.create(
        col.getName,
        toSparkType(col.getType),
        col.isNullable
      )
    }.toArray

  def toSparkSchema(tableDetail: TableDetail): StructType = {
    val fields = tableDetail.getColumns.asScala.map(toSparkField).toArray
    StructType(fields)
  }

  def toSparkField(column: AnalyticsColumn): StructField =
    StructField(
      column.getName,
      toSparkType(column.getType),
      column.isNullable,
      Metadata.empty
    )

  private def toSparkType(dataType: DataType): SparkDataType =
    dataType match {
      case _: DataType.Byte        => ByteType
      case _: DataType.SmallInt    => ShortType
      case _: DataType.Int         => IntegerType
      case _: DataType.BigInt      => LongType
      case _: DataType.Float       => FloatType
      case _: DataType.Double      => DoubleType
      case d: DataType.Decimal     => DataTypes.createDecimalType(d.getPrecision, d.getScale)
      case _: DataType.Text        => StringType
      case _: DataType.Blob        => BinaryType
      case _: DataType.Boolean     => BooleanType
      case _: DataType.Date        => DateType
      case _: DataType.Time        => TimestampNTZType
      case _: DataType.Timestamp   => TimestampNTZType
      case _: DataType.TimestampTZ => TimestampType
      case _: DataType.Duration    => CalendarIntervalType
      case _: DataType.Interval    => CalendarIntervalType
      case _ =>
        throw new IllegalArgumentException(s"Unsupported data type: ${dataType.toString}")
    }
}
