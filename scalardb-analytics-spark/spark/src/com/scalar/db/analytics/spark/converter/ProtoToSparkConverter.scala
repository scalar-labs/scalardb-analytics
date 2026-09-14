/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.converter

import com.scalar.db.analytics.api.model.{Column, DataSourceNamespaceTableDetail, DataType}
import org.apache.spark.sql.types.{DataType => SparkDataType, _}

import scala.jdk.CollectionConverters._

object ProtoToSparkConverter {

  /** Convert API DataType to Spark DataType
    */
  def convertDataType(dataType: DataType): SparkDataType =
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
      case _: DataType.Time        => TimestampType // Time is represented as Timestamp in Spark
      case _: DataType.Timestamp   => TimestampType
      case _: DataType.TimestampTZ => TimestampType
      case _: DataType.Duration    => LongType      // Duration as microseconds
      case _: DataType.Interval    => StringType    // Interval as string representation
      case _                       => StringType    // Default to string for unknown types
    }

  /** Convert API Column to Spark StructField
    */
  def convertColumn(column: Column): StructField = {
    val sparkType = convertDataType(column.getType)
    StructField(
      name = column.getName,
      dataType = sparkType,
      nullable = column.isNullable
    )
  }

  /** Convert API table detail to Spark StructType schema
    */
  def convertTableDetailToSchema(tableDetail: DataSourceNamespaceTableDetail): StructType = {
    val detail = tableDetail.getTable
    val fields = detail.getColumns.asScala
      .sortBy(_.getOrdinalPosition) // Sort by ordinal position
      .map(convertColumn)
      .toSeq
    StructType(fields)
  }

}
