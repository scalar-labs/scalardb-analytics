/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import org.apache.hadoop.mapred.InputFormat
import org.apache.spark.sql.connector.read.{Batch, Scan}
import org.apache.spark.sql.types.StructType

class InputFormatScan[K, V](
    private val inputFormatClass: Class[_ <: InputFormat[K, V]],
    private val schema: StructType,
    convertFunction: RecordKeyValue[K, V] => Array[Any],
    configurator: InputFormatJobConfConfigurator
) extends Scan {

  override def readSchema(): StructType = schema

  override def toBatch(): Batch =
    new InputFormatBatch(inputFormatClass, schema, convertFunction, configurator)

  // Used by Spark's CacheManager to match cached plans via
  // DataSourceV2ScanRelation.sameResult(). Without this, CACHE TABLE has no effect.
  override def equals(obj: Any): Boolean = obj match {
    case other: InputFormatScan[_, _] =>
      inputFormatClass == other.inputFormatClass && schema == other.schema
    case _ => false
  }

  override def hashCode(): Int =
    31 * inputFormatClass.hashCode() + schema.hashCode()
}
