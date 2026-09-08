/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.datasource.scalardb.config

import com.scalar.db.config.DatabaseConfig
import com.scalar.db.storage.multistorage.MultiStorageConfig

object DatabaseConfigUtil {
  def getConfigForNamespace(config: DatabaseConfig, namespace: String): DatabaseConfig = {
    val multiStorageConfig = new MultiStorageConfig(config)
    val storage            = getStorageForNamespace(multiStorageConfig, namespace)
    new DatabaseConfig(multiStorageConfig.getDatabasePropertiesMap.get(storage))
  }

  private def getStorageForNamespace(
      multiStorageConfig: MultiStorageConfig,
      namespace: String
  ): String =
    multiStorageConfig.getNamespaceStorageMap
      .getOrDefault(namespace, multiStorageConfig.getDefaultStorage)
}
