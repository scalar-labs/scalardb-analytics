/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.config

sealed abstract class StorageType(val name: String)

object StorageType {
  case object Dynamo       extends StorageType("dynamo")
  case object Cassandra    extends StorageType("cassandra")
  case object Cosmos       extends StorageType("cosmos")
  case object Jdbc         extends StorageType("jdbc")
  case object MultiStorage extends StorageType("multi-storage")

  @SuppressWarnings(
    Array(
      "org.wartremover.warts.Product",
      "org.wartremover.warts.Serializable",
      "org.wartremover.warts.JavaSerializable"
    )
  )
  val values: Seq[StorageType] = Seq(Dynamo, Cassandra, Cosmos, Jdbc, MultiStorage)

  private val byName: Map[String, StorageType] = values.map(st => st.name -> st).toMap

  /** Returns Some(StorageType) if the name is valid, None otherwise. */
  def of(name: String): Option[StorageType] = byName.get(name)

  /** Returns StorageType if the name is valid, throws IllegalArgumentException otherwise. */
  def apply(name: String): StorageType =
    of(name).getOrElse {
      throw new IllegalArgumentException(s"Unknown ScalarDB storage type: $name")
    }
}
