/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.postgresql

import com.scalar.db.analytics.spark.util.TestImages
import org.testcontainers.utility.DockerImageName

class CatalogWithPostgreSql16Test extends CatalogWithPostgreSqlTestBase {
  override protected def postgresqlImage: DockerImageName = TestImages.POSTGRESQL_16
}
