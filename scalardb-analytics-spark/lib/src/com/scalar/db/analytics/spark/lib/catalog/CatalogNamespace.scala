/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.catalog

import scala.jdk.CollectionConverters._

/** Namespace representation for catalog operations.
  */
case class CatalogNamespace(
    dataSource: String,
    namespace: List[String]
) {

  def toSpark: Array[String] =
    (dataSource :: namespace).toArray

  def startsWith(prefix: Array[String]): Boolean = {
    val ns = toSpark
    if (ns.length < prefix.length) {
      false
    } else {
      prefix.indices.forall(i => ns(i) == prefix(i))
    }
  }

  override def toString: String =
    toSpark.mkString(".")
}

object CatalogNamespace {

  def fromModel(ns: com.scalar.db.analytics.api.model.DataSourceNamespace): CatalogNamespace = {
    val dataSourceName = ns.getDataSource.getName
    val namespaceNames = ns.getNamespace.getNames.asScala.toList
    CatalogNamespace(dataSourceName, namespaceNames)
  }

  def fromSpark(sparkNamespace: Array[String]): CatalogNamespace =
    fromSparkOptional(sparkNamespace).getOrElse {
      throw new IllegalArgumentException("The namespace must have at least one element.")
    }

  def fromSparkOptional(sparkNamespace: Array[String]): Option[CatalogNamespace] =
    if (sparkNamespace.isEmpty) None
    else Some(CatalogNamespace(sparkNamespace.head, sparkNamespace.tail.toList))
}
