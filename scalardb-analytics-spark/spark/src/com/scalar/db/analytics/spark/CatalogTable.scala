/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark

import com.scalar.db.analytics.api.model.TableDetail
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider
import com.scalar.db.analytics.api.model.datasource.provider.rdbms._
import com.scalar.db.analytics.api.model.datasource.provider.{DynamoDbProvider, ScalarDbProvider}
import com.scalar.db.analytics.sdk.ScalarDbAnalyticsClient
import com.scalar.db.analytics.spark.datasource.dynamodb.DynamoDbTableFactory
import com.scalar.db.analytics.spark.datasource.scalardb.table.ScalarDbTableFactory
import com.scalar.db.analytics.spark.lib.catalog.CatalogIdentifier
import com.scalar.db.analytics.spark.lib.table.JdbcTableFactory
import org.apache.spark.sql.catalyst.analysis.NoSuchTableException
import org.apache.spark.sql.connector.catalog.{Identifier, Table}

import scala.jdk.CollectionConverters._

class CatalogTable(
    catalogName: String,
    identifier: CatalogIdentifier,
    analyticsClient: ScalarDbAnalyticsClient
) {

  def loadSparkTable(): Table = {
    val tableDetail = analyticsClient
      .table()
      .describeTableByName(
        catalogName,
        identifier.namespace.dataSource,
        identifier.namespace.namespace.toSeq.asJava,
        identifier.table
      )
      .orElseThrow(() => new NoSuchTableException(identifier.toSpark))

    val detail   = tableDetail.getTable
    val provider = tableDetail.getDataSource.getProvider

    createTableFromProvider(identifier, detail, provider)
  }

  private def createTableFromProvider(
      identifier: CatalogIdentifier,
      detail: TableDetail,
      provider: DataSourceProvider
  ): Table =
    provider match {
      case mysql: MySql =>
        JdbcTableFactory.create(identifier, detail, mysql)

      case postgresql: PostgreSql =>
        JdbcTableFactory.create(identifier, detail, postgresql)

      case oracle: Oracle =>
        JdbcTableFactory.create(identifier, detail, oracle)

      case sqlServer: SqlServer =>
        JdbcTableFactory.create(identifier, detail, sqlServer)

      case scalarDb: ScalarDbProvider =>
        ScalarDbTableFactory.create(identifier, detail, scalarDb)

      case dynamoDb: DynamoDbProvider =>
        DynamoDbTableFactory.create(detail, dynamoDb)

      case databricks: Databricks =>
        JdbcTableFactory.create(identifier, detail, databricks)

      case snowflake: Snowflake =>
        JdbcTableFactory.create(identifier, detail, snowflake)

      case _ =>
        throw new UnsupportedOperationException(
          s"Unsupported data source provider: ${provider.getClass.getSimpleName}"
        )
    }
}

object CatalogTable {
  def apply(
      catalogName: String,
      identifier: Identifier,
      analyticsClient: ScalarDbAnalyticsClient
  ): CatalogTable =
    new CatalogTable(
      catalogName,
      CatalogIdentifier.fromSpark(identifier),
      analyticsClient
    )
}
