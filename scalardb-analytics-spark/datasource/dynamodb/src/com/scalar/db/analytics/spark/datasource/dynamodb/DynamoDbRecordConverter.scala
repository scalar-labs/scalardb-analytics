/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.dynamodb

import com.scalar.db.analytics.api.model.{DataType, TableDetail}
import com.scalar.db.analytics.spark.lib.table.hadoop.RecordKeyValue
import org.apache.hadoop.dynamodb.DynamoDBItemWritable
import org.apache.hadoop.io.Text
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

import java.math.BigDecimal
import scala.jdk.CollectionConverters._

/** Factory for creating a function to convert DynamoDB records to Spark rows.
  */
class DynamoDbRecordConverter(tableDetail: TableDetail) extends Serializable {
  private type AttributeValueConverter = AttributeValue => AnyRef

  private val converters: Array[(String, AttributeValueConverter)] =
    tableDetail.getColumns.asScala.map { column =>
      val name      = column.getName
      val converter = createValueConverter(column.getType)
      (name, converter)
    }.toArray

  def convert(keyValue: RecordKeyValue[Text, DynamoDBItemWritable]): Array[Any] = {
    val map = keyValue.value.getItem
    converters.map { case (name, converter) => getValue(map, name, converter) }
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private def getValue(
      map: java.util.Map[String, AttributeValue],
      name: String,
      converter: AttributeValueConverter
  ): AnyRef =
    if (!map.containsKey(name)) {
      null
    } else {
      converter(map.get(name))
    }

  /** Creates a value converter function for a data type.
    *
    * @param dataType
    *   The column data type
    * @return
    *   A function that converts an AttributeValue to the appropriate type
    */
  private def createValueConverter(dataType: DataType): AttributeValue => AnyRef =
    dataType match {
      case _: DataType.Byte     => (attr: AttributeValue) => java.lang.Byte.valueOf(attr.n())
      case _: DataType.SmallInt => (attr: AttributeValue) => java.lang.Short.valueOf(attr.n())
      case _: DataType.Int      => (attr: AttributeValue) => java.lang.Integer.valueOf(attr.n())
      case _: DataType.BigInt   => (attr: AttributeValue) => java.lang.Long.valueOf(attr.n())
      case _: DataType.Float    => (attr: AttributeValue) => java.lang.Float.valueOf(attr.n())
      case _: DataType.Double   => (attr: AttributeValue) => java.lang.Double.valueOf(attr.n())
      case _: DataType.Decimal  => (attr: AttributeValue) => new BigDecimal(attr.n())
      case _: DataType.Text     => (attr: AttributeValue) => attr.s()
      case _: DataType.Blob     => (attr: AttributeValue) => attr.b().asByteArray()
      case _: DataType.Boolean  => (attr: AttributeValue) => java.lang.Boolean.valueOf(attr.bool())
      case _: DataType.Date =>
        throw new UnsupportedOperationException(
          "Date type is not supported for DynamoDB data source yet."
        )
      case _: DataType.Time =>
        throw new UnsupportedOperationException(
          "Time type is not supported for DynamoDB data source yet."
        )
      case _: DataType.Timestamp =>
        throw new UnsupportedOperationException(
          "DateTime type is not supported for DynamoDB data source yet."
        )
      case _: DataType.TimestampTZ =>
        throw new UnsupportedOperationException(
          "Timestamp type is not supported for DynamoDB data source yet."
        )
      case _: DataType.Duration =>
        throw new UnsupportedOperationException(
          "Duration type is not supported for DynamoDB data source yet."
        )
      case _: DataType.Interval =>
        throw new UnsupportedOperationException(
          "Interval type is not supported for DynamoDB data source yet."
        )
      case _ =>
        throw new IllegalArgumentException(s"Unknown data type: ${dataType.toString}")
    }

}
