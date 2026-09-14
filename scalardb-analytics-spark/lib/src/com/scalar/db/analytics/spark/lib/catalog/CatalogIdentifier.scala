/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.catalog

import org.apache.spark.sql.connector.catalog.Identifier

/** Identifier representation for catalog operations.
  */
case class CatalogIdentifier(
    namespace: CatalogNamespace,
    table: String
) {

  def toSpark: Identifier =
    Identifier.of(namespace.toSpark, table)

  override def toString: String =
    s"${namespace.toString}.$table"
}

object CatalogIdentifier {

  def fromModel(
      table: com.scalar.db.analytics.api.model.DataSourceNamespaceTable
  ): CatalogIdentifier = {
    val dataSourceNamespace = table.toDataSourceNamespace()
    val tableName           = table.getTable.getInfo.getName
    CatalogIdentifier(
      CatalogNamespace.fromModel(dataSourceNamespace),
      tableName
    )
  }

  def fromSpark(identifier: Identifier): CatalogIdentifier =
    CatalogIdentifier(
      CatalogNamespace.fromSpark(identifier.namespace()),
      identifier.name()
    )
}
