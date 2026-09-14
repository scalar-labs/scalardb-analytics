/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import org.apache.hadoop.mapred.{InputFormat, JobConf, Reporter}
import org.apache.hadoop.util.ReflectionUtils
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.read.{InputPartition, PartitionReader, PartitionReaderFactory}
import org.apache.spark.sql.types.StructType

import java.io.IOException

class InputFormatPartitionReaderFactory[K, V](
    inputFormatClass: Class[_ <: InputFormat[K, V]],
    schema: StructType,
    convertFunction: RecordKeyValue[K, V] => Array[Any],
    configurator: InputFormatJobConfConfigurator
) extends PartitionReaderFactory {

  override def createReader(partition: InputPartition): PartitionReader[InternalRow] =
    partition match {
      case inputFormatPartition: InputFormatPartition =>
        val jobConf = new JobConf()
        configurator.accept(jobConf)
        val inputFormat = ReflectionUtils.newInstance(inputFormatClass, jobConf)

        val recordReader =
          try
            inputFormat.getRecordReader(inputFormatPartition.getSplit, jobConf, Reporter.NULL)
          catch {
            case e: IOException =>
              throw new RuntimeException("Failed to create record reader", e)
          }

        new InputFormatPartitionReader(recordReader, schema, convertFunction)

      case _ =>
        throw new IllegalArgumentException(
          s"Unsupported partition type: ${partition.getClass.getName}"
        )
    }
}
