/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import org.apache.hadoop.mapred.{InputFormat, JobConf}
import org.apache.hadoop.util.ReflectionUtils
import org.apache.spark.sql.connector.read.{Batch, InputPartition, PartitionReaderFactory}
import org.apache.spark.sql.types.StructType

class InputFormatBatch[K, V](
    inputFormatClass: Class[_ <: InputFormat[K, V]],
    schema: StructType,
    convertFunction: RecordKeyValue[K, V] => Array[Any],
    configurator: InputFormatJobConfConfigurator
) extends Batch {

  override def planInputPartitions(): Array[InputPartition] =
    try {
      val jobConf = new JobConf()
      configurator.accept(jobConf)
      val inputFormat = ReflectionUtils.newInstance(inputFormatClass, jobConf)
      val splits      = inputFormat.getSplits(jobConf, 0)

      splits.zipWithIndex.map { case (split, index) =>
        new InputFormatPartition(index, split)
      }
    } catch {
      case e: Exception =>
        throw new RuntimeException("Failed to plan input partitions", e)
    }

  override def createReaderFactory(): PartitionReaderFactory =
    new InputFormatPartitionReaderFactory(inputFormatClass, schema, convertFunction, configurator)
}
