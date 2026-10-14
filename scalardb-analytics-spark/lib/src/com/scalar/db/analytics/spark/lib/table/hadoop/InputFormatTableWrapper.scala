/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import org.apache.hadoop.mapred.InputFormat
import org.apache.spark.sql.connector.catalog.{SupportsRead, Table, TableCapability}
import org.apache.spark.sql.connector.read.ScanBuilder
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import scala.jdk.CollectionConverters._

/** An implementation of Spark v2 catalog Table interface using Hadoop InputFormat.
  *
  * @param tableName
  *   the name of the table, which must identify it uniquely within the catalog (the catalog
  *   identifier, not the name of the table in the underlying store), because it is part of the
  *   identity this table is compared by
  */
class InputFormatTableWrapper[K, V](
    private val inputFormatClass: Class[_ <: InputFormat[K, V]],
    private val tableSchema: StructType,
    convertFunction: RecordKeyValue[K, V] => Array[Any],
    configurator: InputFormatJobConfConfigurator,
    private val tableName: String
) extends Table
    with SupportsRead {

  override def name(): String = tableName

  override def schema(): StructType = tableSchema

  override def capabilities(): java.util.Set[TableCapability] =
    Set(TableCapability.BATCH_READ).asJava

  override def newScanBuilder(options: CaseInsensitiveStringMap): ScanBuilder =
    new InputFormatScanBuilder(inputFormatClass, tableSchema, convertFunction, configurator)

  // Used by Spark's CacheManager. Cache matching runs on the pre-optimization plan, where this
  // table is wrapped in a DataSourceV2Relation; DataSourceV2Relation.sameResult() compares the
  // wrapped Table via equals. Every load creates a new instance, so without this, a cached table
  // is found only when the very same instance is queried again.
  override def equals(obj: Any): Boolean = obj match {
    case other: InputFormatTableWrapper[_, _] =>
      inputFormatClass == other.inputFormatClass &&
      tableSchema == other.tableSchema &&
      tableName == other.tableName
    case _ => false
  }

  override def hashCode(): Int =
    java.util.Objects.hash(inputFormatClass, tableSchema, tableName)
}
