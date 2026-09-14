/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.spark.lib.table.SchemaConverter
import org.apache.spark.sql.types.StructType

import scala.jdk.CollectionConverters._

case class ScalarDbTableDescription(
    namespace: String,
    table: String,
    columns: Array[ScalarDbColumn]
) {

  def getSparkSchema: StructType =
    new StructType(columns.map(_.sparkField))
}

object ScalarDbTableDescription {
  def schemaFromColumns(columns: Array[ScalarDbColumn]): StructType =
    new StructType(columns.map(_.sparkField))

  def from(
      namespace: String,
      tableDetail: com.scalar.db.analytics.api.model.TableDetail
  ): ScalarDbTableDescription = {
    val columns: Array[ScalarDbColumn] = tableDetail.getColumns.asScala.map { c =>
      ScalarDbColumn(
        TypeMapping.toScalarDbType(c.getType),
        SchemaConverter.toSparkField(c)
      )
    }.toArray

    ScalarDbTableDescription(namespace, tableDetail.getInfo.getName, columns)
  }
}
