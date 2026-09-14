/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.api.model.DataType
import com.scalar.db.io.{DataType => ScalarDbDataType}

/** Utility for mapping between API DataType and ScalarDB DataType.
  *
  * Note: This code is duplicated from datasource-scalardb/schema/TypeMapping in the Java version.
  * The duplication is intentional to keep scalardb-analytics-spark self-contained. When datasources
  * are refactored into plugins in the future, this will be consolidated into a single ScalarDB
  * datasource plugin.
  */
object TypeMapping {

  /** Converts API DataType to ScalarDB DataType.
    */
  @SuppressWarnings(Array("org.wartremover.warts.StringPlusAny"))
  def toScalarDbType(dataType: DataType): ScalarDbDataType =
    dataType match {
      case _: DataType.Boolean     => ScalarDbDataType.BOOLEAN
      case _: DataType.Int         => ScalarDbDataType.INT
      case _: DataType.BigInt      => ScalarDbDataType.BIGINT
      case _: DataType.Float       => ScalarDbDataType.FLOAT
      case _: DataType.Double      => ScalarDbDataType.DOUBLE
      case _: DataType.Text        => ScalarDbDataType.TEXT
      case _: DataType.Blob        => ScalarDbDataType.BLOB
      case _: DataType.Date        => ScalarDbDataType.DATE
      case _: DataType.Time        => ScalarDbDataType.TIME
      case _: DataType.Timestamp   => ScalarDbDataType.TIMESTAMP
      case _: DataType.TimestampTZ => ScalarDbDataType.TIMESTAMPTZ
      case _ =>
        throw new IllegalArgumentException(s"Unsupported data type for ScalarDB: $dataType")
    }
}
