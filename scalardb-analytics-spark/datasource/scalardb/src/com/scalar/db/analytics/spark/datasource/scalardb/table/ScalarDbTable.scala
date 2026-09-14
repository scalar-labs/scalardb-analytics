/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.api.model.TableDetail
import com.scalar.db.analytics.spark.lib.catalog.CatalogIdentifier
import com.scalar.db.analytics.spark.lib.table.SchemaConverter
import com.scalar.db.config.DatabaseConfig
import org.apache.spark.sql.connector.catalog.{Column, SupportsRead, Table, TableCapability}
import org.apache.spark.sql.connector.read.ScanBuilder
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import java.util
import scala.jdk.CollectionConverters._

/** Implementation of [[Table]] for ScalarDB. This currently supports only batch read operation.
  * This class reads data from ScalarDB-backed tables using the ScalarDB library and its storage
  * API.
  */
final class ScalarDbTable(
    private val identifier: CatalogIdentifier,
    table: TableDetail,
    val config: DatabaseConfig
) extends Table
    with SupportsRead {

  override def name(): String =
    identifier.toString

  override def columns(): Array[Column] =
    SchemaConverter.toSparkColumns(table)

  @SuppressWarnings(Array("deprecation"))
  override def schema(): StructType =
    SchemaConverter.toSparkSchema(table)

  override def capabilities(): util.Set[TableCapability] =
    Set(TableCapability.BATCH_READ).asJava

  override def newScanBuilder(options: CaseInsensitiveStringMap): ScanBuilder =
    new ScalarDbScanBuilder(identifier, table, config)

  // Used by Spark's CacheManager. Cache matching runs on the pre-optimization plan, where this
  // table is wrapped in a DataSourceV2Relation; DataSourceV2Relation.sameResult() compares the
  // wrapped Table via equals. Without this, each instance is distinct and CACHE TABLE has no
  // effect.
  override def equals(obj: Any): Boolean = obj match {
    case other: ScalarDbTable => identifier == other.identifier
    case _                    => false
  }

  override def hashCode(): Int = identifier.hashCode()
}
