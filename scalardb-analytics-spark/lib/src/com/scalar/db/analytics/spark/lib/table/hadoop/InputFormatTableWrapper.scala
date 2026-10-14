/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table.hadoop

import com.scalar.db.analytics.spark.lib.catalog.CatalogIdentifier
import org.apache.hadoop.mapred.InputFormat
import org.apache.spark.sql.connector.catalog.{SupportsRead, Table, TableCapability}
import org.apache.spark.sql.connector.read.ScanBuilder
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import scala.jdk.CollectionConverters._

/** An implementation of Spark v2 catalog Table interface using Hadoop InputFormat.
  *
  * @param tableName
  *   the name of the table, as reported by [[name]]
  * @param identifier
  *   the identifier of the table in the catalog, which this table is compared by
  */
class InputFormatTableWrapper[K, V](
    inputFormatClass: Class[_ <: InputFormat[K, V]],
    schema: StructType,
    convertFunction: RecordKeyValue[K, V] => Array[Any],
    configurator: InputFormatJobConfConfigurator,
    tableName: String,
    private val identifier: CatalogIdentifier
) extends Table
    with SupportsRead {

  override def name(): String = tableName

  override def schema(): StructType = schema

  override def capabilities(): java.util.Set[TableCapability] =
    Set(TableCapability.BATCH_READ).asJava

  override def newScanBuilder(options: CaseInsensitiveStringMap): ScanBuilder =
    new InputFormatScanBuilder(inputFormatClass, schema, convertFunction, configurator)

  // Used by Spark's CacheManager. Cache matching runs on the pre-optimization plan, where this
  // table is wrapped in a DataSourceV2Relation; DataSourceV2Relation.sameResult() compares the
  // wrapped Table via equals. Every load creates a new instance, so without this, a cached table
  // is found only when the very same instance is queried again. Compared by the catalog
  // identifier, as ScalarDbTable is.
  override def equals(obj: Any): Boolean = obj match {
    case other: InputFormatTableWrapper[_, _] => identifier == other.identifier
    case _                                    => false
  }

  override def hashCode(): Int = identifier.hashCode()
}
