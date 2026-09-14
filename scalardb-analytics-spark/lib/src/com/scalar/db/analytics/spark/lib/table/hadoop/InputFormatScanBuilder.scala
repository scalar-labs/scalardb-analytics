/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import org.apache.hadoop.mapred.InputFormat
import org.apache.spark.sql.connector.read.{Scan, ScanBuilder}
import org.apache.spark.sql.types.StructType

class InputFormatScanBuilder[K, V](
    inputFormatClass: Class[_ <: InputFormat[K, V]],
    schema: StructType,
    convertFunction: RecordKeyValue[K, V] => Array[Any],
    configurator: InputFormatJobConfConfigurator
) extends ScanBuilder {

  override def build(): Scan =
    new InputFormatScan(inputFormatClass, schema, convertFunction, configurator)
}
