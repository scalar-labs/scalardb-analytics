/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import com.scalar.db.analytics.spark.lib.table.SparkRowConverter
import org.apache.hadoop.mapred.RecordReader
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.read.PartitionReader
import org.apache.spark.sql.types.StructType

import java.io.IOException

class InputFormatPartitionReader[K, V](
    recordReader: RecordReader[K, V],
    schema: StructType,
    convertFunction: RecordKeyValue[K, V] => Array[Any]
) extends PartitionReader[InternalRow] {

  private val converter       = new SparkRowConverter(schema, convertFunction)
  private val currentKeyValue = RecordKeyValue.fromRecordReader(recordReader)

  override def next(): Boolean =
    try
      recordReader.next(currentKeyValue.key, currentKeyValue.value)
    catch {
      case e: Exception =>
        throw new IOException("Failed to read next record", e)
    }

  override def get(): InternalRow =
    converter.convertToInternalRow(currentKeyValue)

  override def close(): Unit =
    if (recordReader != null) {
      recordReader.close()
    }
}
