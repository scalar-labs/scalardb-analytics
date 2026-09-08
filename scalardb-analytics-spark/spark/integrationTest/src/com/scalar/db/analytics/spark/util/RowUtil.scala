/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.util

import org.apache.spark.sql.Row
import org.apache.spark.sql.types.DataType

object RowUtil {
  def getString(row: Row, colName: String): String =
    row.getString(row.fieldIndex(colName))

  def getDataType(row: Row, colName: String): DataType =
    DataType.fromDDL(getString(row, colName))
}
