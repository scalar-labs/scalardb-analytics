/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import org.apache.hadoop.mapred.InputSplit
import org.apache.spark.SerializableWritable
import org.apache.spark.sql.connector.read.InputPartition

import scala.util.Try

class InputFormatPartition(
    val index: Int,
    // ref: org.apache.spark.rdd.HadoopPartition
    private val split: SerializableWritable[InputSplit]
) extends InputPartition
    with Serializable {

  def this(index: Int, inputSplit: InputSplit) =
    this(index, new SerializableWritable(inputSplit))

  def getSplit: InputSplit = split.value

  override def preferredLocations(): Array[String] =
    Try(getSplit.getLocations).getOrElse(Array.empty[String])
}
