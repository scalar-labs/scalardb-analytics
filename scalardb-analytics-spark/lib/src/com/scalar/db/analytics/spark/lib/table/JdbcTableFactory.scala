/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.lib.table

import com.scalar.db.analytics.api.model.TableDetail
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider
import com.scalar.db.analytics.api.model.datasource.provider.rdbms._
import com.scalar.db.analytics.spark.lib.catalog.CatalogIdentifier
import com.scalar.db.analytics.spark.lib.port.ForkedJDBCTable
import com.scalar.db.analytics.spark.lib.Constants
import org.apache.spark.sql.execution.datasources.jdbc.JDBCOptions

object JdbcTableFactory {

  def create(
      identifier: CatalogIdentifier,
      detail: TableDetail,
      provider: DataSourceProvider
  ): ForkedJDBCTable =
    provider match {
      case mysql: MySql =>
        createMySqlTable(identifier, detail, mysql)

      case postgresql: PostgreSql =>
        createPostgreSqlTable(identifier, detail, postgresql)

      case oracle: Oracle =>
        createOracleTable(identifier, detail, oracle)

      case sqlServer: SqlServer =>
        createSqlServerTable(identifier, detail, sqlServer)

      case databricks: Databricks =>
        createDatabricksTable(identifier, detail, databricks)

      case snowflake: Snowflake =>
        createSnowflakeTable(identifier, detail, snowflake)

      case _ =>
        throw new IllegalArgumentException(
          s"Unsupported JDBC provider type: ${provider.getClass.getSimpleName}"
        )
    }

  private def createForkedJDBCTable(
      identifier: CatalogIdentifier,
      detail: TableDetail,
      url: String,
      driverClass: String,
      extraOptions: Map[String, String]
  ): ForkedJDBCTable = {
    val jdbcOptions = Map(
      JDBCOptions.JDBC_URL              -> url,
      JDBCOptions.JDBC_DRIVER_CLASS     -> driverClass,
      JDBCOptions.JDBC_TABLE_NAME       -> getJdbcTableName(identifier),
      JDBCOptions.JDBC_BATCH_FETCH_SIZE -> Constants.JDBC_TABLE_BATCH_SIZE.toString
    ) ++ extraOptions

    ForkedJDBCTable(
      identifier.toSpark,
      SchemaConverter.toSparkSchema(detail),
      new JDBCOptions(jdbcOptions)
    )
  }

  private def createMySqlTable(
      identifier: CatalogIdentifier,
      detail: TableDetail,
      mysql: MySql
  ): ForkedJDBCTable = {
    // Reuse MySql.getUrl() so the connection options it carries (permitMysqlScheme, sslMode)
    // cannot drift from the URL used at data source registration time.
    val url = mysql.getUrl
    val extraOptions = Map(
      "user"     -> mysql.getUsername,
      "password" -> mysql.getPassword
    )
    createForkedJDBCTable(identifier, detail, url, "org.mariadb.jdbc.Driver", extraOptions)
  }

  private def createPostgreSqlTable(
      identifier: CatalogIdentifier,
      detail: TableDetail,
      postgresql: PostgreSql
  ): ForkedJDBCTable = {
    val url =
      s"jdbc:postgresql://${postgresql.getHost}:${postgresql.getPort.toString}/${postgresql.getDatabase}"
    val extraOptions = Map(
      "user"     -> postgresql.getUsername,
      "password" -> postgresql.getPassword
    )
    createForkedJDBCTable(identifier, detail, url, "org.postgresql.Driver", extraOptions)
  }

  private def createOracleTable(
      identifier: CatalogIdentifier,
      detail: TableDetail,
      oracle: Oracle
  ): ForkedJDBCTable = {
    val url =
      s"jdbc:oracle:thin:@//${oracle.getHost}:${oracle.getPort.toString}/${oracle.getServiceName}"
    val extraOptions = Map(
      "user"     -> oracle.getUsername,
      "password" -> oracle.getPassword
    )
    createForkedJDBCTable(identifier, detail, url, "oracle.jdbc.OracleDriver", extraOptions)
  }

  private def createSqlServerTable(
      identifier: CatalogIdentifier,
      detail: TableDetail,
      sqlServer: SqlServer
  ): ForkedJDBCTable = {
    val url = s"jdbc:sqlserver://${sqlServer.getHost}:${sqlServer.getPort.toString}"
    val extraOptions = {
      val opts = Map.newBuilder[String, String]
      opts ++= Map(
        "user"     -> sqlServer.getUsername,
        "password" -> sqlServer.getPassword
      )
      Option(sqlServer.getDatabase).foreach(db => opts += ("databaseName" -> db))
      Option(sqlServer.getSecure).foreach(s => opts += ("encrypt" -> s.toString))
      opts.result()
    }
    createForkedJDBCTable(
      identifier,
      detail,
      url,
      "com.microsoft.sqlserver.jdbc.SQLServerDriver",
      extraOptions
    )
  }

  private def createDatabricksTable(
      identifier: CatalogIdentifier,
      detail: TableDetail,
      databricks: Databricks
  ): ForkedJDBCTable = {
    val url = Option(databricks.getPort) match {
      case Some(p) => s"jdbc:databricks://${databricks.getHost}:${p.toString}"
      case None    => s"jdbc:databricks://${databricks.getHost}"
    }
    val extraOptions = {
      val opts = Map.newBuilder[String, String]
      opts ++= Map(
        "httpPath"       -> databricks.getHttpPath,
        "AuthMech"       -> "11",
        "Auth_Flow"      -> "1",
        "OAuth2ClientId" -> databricks.getOAuthClientId,
        "OAuth2Secret"   -> databricks.getOAuthSecret
      )
      Option(databricks.getCatalog).foreach(catalog => opts += ("catalog" -> catalog))
      opts.result()
    }
    createForkedJDBCTable(
      identifier,
      detail,
      url,
      "com.databricks.client.jdbc.Driver",
      extraOptions
    )
  }

  private def createSnowflakeTable(
      identifier: CatalogIdentifier,
      detail: TableDetail,
      snowflake: Snowflake
  ): ForkedJDBCTable = {
    val url = s"jdbc:snowflake://${snowflake.getAccount}.snowflakecomputing.com/"
    val extraOptions = {
      val opts = Map.newBuilder[String, String]
      opts ++= Map(
        "user"     -> snowflake.getUsername,
        "password" -> snowflake.getPassword
      )
      Option(snowflake.getDatabase).foreach(db => opts += ("db" -> db))
      opts.result()
    }
    createForkedJDBCTable(
      identifier,
      detail,
      url,
      "net.snowflake.client.jdbc.SnowflakeDriver",
      extraOptions
    )
  }

  private def getJdbcTableName(identifier: CatalogIdentifier): String =
    (identifier.namespace.namespace :+ identifier.table).mkString(".")
}
