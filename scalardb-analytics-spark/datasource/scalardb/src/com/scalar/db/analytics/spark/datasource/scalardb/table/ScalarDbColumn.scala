/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.io.DataType
import org.apache.spark.sql.types.StructField

case class ScalarDbColumn(
    scalarDbType: DataType,
    sparkField: StructField
)
