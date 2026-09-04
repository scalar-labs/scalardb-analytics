/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.sqlserver

import com.scalar.db.analytics.spark.util.TestImages
import org.testcontainers.utility.DockerImageName

class CatalogWithSqlServer2022Test extends CatalogWithSqlServerTestBase {
  override protected def sqlServerImage: DockerImageName = TestImages.SQL_SERVER_2022
}
