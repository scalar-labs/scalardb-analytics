/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.spark.lib.catalog.CatalogIdentifier
import com.scalar.db.config.DatabaseConfig
import org.apache.spark.sql.connector.read.{Batch, InputPartition, PartitionReaderFactory}
import org.apache.spark.sql.types.StructType

/** This class is a physical representation of a batch read operation of Spark via ScalarDB. It
  * implements [[Batch]] and creates [[ScalarDbInputPartition]] and
  * [[ScalarDbPartitionReaderFactory]] to be used by Spark.
  */
class ScalarDbBatch(
    identifier: CatalogIdentifier,
    table: com.scalar.db.analytics.api.model.TableDetail,
    config: DatabaseConfig,
    pushedConditions: Option[PushDownConditions],
    prunedSchema: Option[StructType],
    pushedLimit: Option[Int]
) extends Batch {

  /** Returns partitions to be read by Spark. Currently, this always returns a single partition.
    *
    * @return
    *   Partitions to be read by Spark
    */
  override def planInputPartitions(): Array[InputPartition] =
    Array(new ScalarDbInputPartition())

  /** Create an instance of [[ScalarDbPartitionReaderFactory]], which is used to create
    * [[ScalarDbPartitionReader]].
    *
    * @return
    *   An instance of [[ScalarDbPartitionReaderFactory]]
    */
  override def createReaderFactory(): PartitionReaderFactory =
    new ScalarDbPartitionReaderFactory(
      identifier,
      config,
      table,
      PushDownInfo.from(pushedConditions, prunedSchema, pushedLimit)
    )
}
