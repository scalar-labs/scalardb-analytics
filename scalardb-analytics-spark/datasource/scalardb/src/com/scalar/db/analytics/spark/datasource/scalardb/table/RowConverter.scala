/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.spark.lib.table.SparkRowConverter
import com.scalar.db.api.Result
import com.scalar.db.io.DataType
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.catalyst.expressions.UnsafeRow

import java.time.{LocalDate, LocalDateTime}

/** A converter from ScalarDB's [[Result]] to Spark's [[InternalRow]].
  *
  * When column pruning is active, only the pruned columns are converted.
  */
class RowConverter private (columns: Array[ScalarDbColumn]) {
  private val sparkRowConverter: SparkRowConverter[Result] =
    new SparkRowConverter[Result](
      ScalarDbTableDescription.schemaFromColumns(columns),
      getConvertFunction(columns)
    )

  def convertToUnsafeRow(result: Result): UnsafeRow =
    sparkRowConverter.convertToUnsafeRow(result)

  def convertToInternalRow(result: Result): InternalRow =
    sparkRowConverter.convertToInternalRow(result)

  private def getConvertFunction(
      columns: Array[ScalarDbColumn]
  ): Result => Array[Any] = { (result: Result) =>
    columns.map(col => getValue(result, col.sparkField.name, col.scalarDbType)).toArray
  }

  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private def getValue(result: Result, name: String, scalarDbType: DataType): AnyRef =
    if (result.isNull(name)) {
      null
    } else {
      scalarDbType match {
        case DataType.TIME =>
          val time = result.getTime(name)
          LocalDateTime.of(RowConverter.EPOCH_DATE, time)
        case DataType.BLOB =>
          result.getBlob(name).array()
        case _ =>
          result.getAsObject(name)
      }
    }
}

object RowConverter {
  private val EPOCH_DATE: LocalDate = LocalDate.ofEpochDay(0)

  /** Creates a [[RowConverter]] that converts only the specified columns, or all columns if
    * prunedColumnNames is None.
    */
  def create(
      desc: ScalarDbTableDescription,
      prunedColumnNames: Option[Array[String]]
  ): RowConverter = {
    val columns = prunedColumnNames match {
      case Some(names) =>
        val columnMap = desc.columns.map(col => col.sparkField.name -> col).toMap
        names.flatMap(name => columnMap.get(name).toList)
      case None =>
        desc.columns
    }
    new RowConverter(columns)
  }
}
