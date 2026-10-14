/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import org.apache.hadoop.mapred.InputFormat
import org.apache.spark.sql.connector.read.{Batch, Scan}
import org.apache.spark.sql.types.StructType

class InputFormatScan[K, V](
    inputFormatClass: Class[_ <: InputFormat[K, V]],
    schema: StructType,
    convertFunction: RecordKeyValue[K, V] => Array[Any],
    configurator: InputFormatJobConfConfigurator
) extends Scan {

  override def readSchema(): StructType = schema

  override def toBatch(): Batch =
    new InputFormatBatch(inputFormatClass, schema, convertFunction, configurator)
}
