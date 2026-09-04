/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import org.apache.hadoop.mapred.RecordReader

/** RecordKeyValue is a simple class to hold a key and a value returned from a RecordReader.
  * Normally, K and V are the subclass of Writable.
  */
case class RecordKeyValue[K, V](key: K, value: V)

object RecordKeyValue {
  def fromRecordReader[K, V](reader: RecordReader[K, V]): RecordKeyValue[K, V] =
    RecordKeyValue(reader.createKey(), reader.createValue())
}
