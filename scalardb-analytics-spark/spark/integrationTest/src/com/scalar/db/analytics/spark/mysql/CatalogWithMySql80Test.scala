/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.mysql

import com.scalar.db.analytics.spark.util.TestImages
import org.testcontainers.utility.DockerImageName

class CatalogWithMySql80Test extends CatalogWithMySqlTestBase {
  override protected def mysqlImage: DockerImageName = TestImages.MYSQL_80
}
