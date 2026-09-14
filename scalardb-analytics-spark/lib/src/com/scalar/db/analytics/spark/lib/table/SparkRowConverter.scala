/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table

import org.apache.spark.sql.catalyst.expressions.{GenericRow, UnsafeProjection, UnsafeRow}
import org.apache.spark.sql.catalyst.{CatalystTypeConverters, InternalRow}
import org.apache.spark.sql.types.StructType

/** A utility class to convert T into Spark row, where T is a row representation in a particular
  * data source.
  *
  * @param schema
  *   Spark schema that converted Row will have
  * @param convertFunction
  *   Function to convert a row representation in a particular data source to an array of objects,
  *   which will be used to create a Spark Row
  * @tparam T
  *   A row representation in a particular data source
  */
class SparkRowConverter[T](
    schema: StructType,
    convertFunction: T => Array[Any]
) {

  /** A function to convert between org.apache.spark.sql.Row and InternalRow. This wraps
    * org.apache.spark.sql.catalyst.CatalystTypeConverter, which is created using
    * CatalystTypeConverters.createToCatalystConverter.
    */
  private val converter: Any => Any =
    CatalystTypeConverters.createToCatalystConverter(schema)

  /** A projection operator that converts InternalRow to UnsafeRow. */
  private val projection: UnsafeProjection = UnsafeProjection.create(schema)

  def convertToUnsafeRow(result: T): UnsafeRow =
    projection.apply(convertToInternalRow(result))

  // Catalyst converter returns Any but we know it's always InternalRow for row conversions
  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  def convertToInternalRow(result: T): InternalRow = {
    val data = convertFunction(result)
    val row  = new GenericRow(data)
    converter(row).asInstanceOf[InternalRow]
  }
}
