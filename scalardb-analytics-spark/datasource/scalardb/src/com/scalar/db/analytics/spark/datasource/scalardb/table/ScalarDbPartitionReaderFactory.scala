/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.table

import com.scalar.db.analytics.spark.lib.catalog.CatalogIdentifier
import com.scalar.db.config.DatabaseConfig
import org.apache.spark.sql.catalyst.InternalRow
import org.apache.spark.sql.connector.read.{InputPartition, PartitionReader, PartitionReaderFactory}

import java.util.Properties

class ScalarDbPartitionReaderFactory(
    /* Since PartitionReaderFactory must be serializable, we cannot pass the DatabaseConfig object
     * or the TableDetail object directly. So, we pack the necessary information into the
     * configProperties and desc fields. PushDownInfo is also serializable. */
    configProperties: Properties,
    desc: ScalarDbTableDescription,
    pushDownInfo: PushDownInfo
) extends PartitionReaderFactory
    with Serializable {

  def this(
      identifier: CatalogIdentifier,
      config: DatabaseConfig,
      table: com.scalar.db.analytics.api.model.TableDetail,
      pushDownInfo: PushDownInfo
  ) =
    this(
      config.getProperties,
      ScalarDbTableDescription.from(NamespaceUtil.getScalarDbNamespace(identifier), table),
      pushDownInfo
    )

  @SuppressWarnings(Array("org.wartremover.warts.AsInstanceOf"))
  override def createReader(partition: InputPartition): PartitionReader[InternalRow] =
    ScalarDbPartitionReader.create(
      partition.asInstanceOf[ScalarDbInputPartition],
      configProperties,
      desc,
      pushDownInfo
    )
}
