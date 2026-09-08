/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark

import com.scalar.db.analytics.compat.spark.DatabricksDialect
import com.scalar.db.analytics.grpc.common.TlsConfig
import com.scalar.db.analytics.api.auth.PasswordCredential
import com.scalar.db.analytics.sdk.ScalarDbAnalyticsClient
import com.scalar.db.analytics.api.error.AnalyticsException
import com.scalar.db.analytics.spark.config.{Config, ServerConfig}
import com.scalar.db.analytics.spark.exception.ScalarDbAnalyticsCatalogException
import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.scheduler.{SparkListener, SparkListenerApplicationEnd}
import org.apache.spark.sql.{AnalysisException, SparkSession}
import org.apache.spark.sql.catalyst.analysis.NoSuchNamespaceException
import org.apache.spark.sql.connector.catalog.{
  Identifier,
  NamespaceChange,
  SupportsNamespaces,
  Table,
  TableCatalog,
  TableChange
}
import org.apache.spark.sql.connector.expressions.Transform
import org.apache.spark.sql.jdbc.JdbcDialects
import org.apache.spark.sql.types.StructType
import org.apache.spark.sql.util.CaseInsensitiveStringMap

import scala.jdk.CollectionConverters._
import scala.util.Try
import scala.jdk.OptionConverters._

/** Spark catalog implementation backed by ScalarDB Analytics gRPC services.
  *
  * The catalog behaviour lives here so that a distribution can attach its own concerns through the
  * three hooks below without reimplementing any catalog operation. The hooks are abstract so that
  * every concrete catalog states what it does at each point, including doing nothing.
  */
trait ScalarDbAnalyticsCatalogBase extends TableCatalog with SupportsNamespaces {

  /** Runs once after the catalog has initialized its client and registered its dialect. */
  protected def onInitialized(): Unit

  /** Runs before every catalog operation. Throwing here aborts the operation. */
  protected def beforeOperation(): Unit

  /** Runs when the Spark application ends, before the catalog's own resources are closed. */
  protected def onClose(): Unit

  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  @volatile private var _catalogName: Option[String] = None
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  @volatile private var _serverConfig: Option[ServerConfig] = None
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  @volatile private var _analyticsClient: Option[ScalarDbAnalyticsClient] = None
  @SuppressWarnings(Array("org.wartremover.warts.Var"))
  @volatile private var _tlsConfig: Option[TlsConfig] = None

  private def catalogName: String =
    _catalogName.getOrElse(throw new IllegalStateException("Catalog not initialized"))

  private def analyticsClient: ScalarDbAnalyticsClient =
    _analyticsClient.getOrElse(throw new IllegalStateException("Catalog not initialized"))

  override def initialize(name: String, options: CaseInsensitiveStringMap): Unit =
    withErrorHandling {
      _catalogName = Some(name)

      val config       = Config.parse(options.asScala.toMap)
      val serverConfig = config.serverConfig
      _serverConfig = Some(serverConfig)

      _tlsConfig = if (serverConfig.tlsEnabled) {
        val tlsConfigBuilder = TlsConfig.builder()
        serverConfig.caCertFilePath.foreach(tlsConfigBuilder.caRootCertPath)
        serverConfig.tlsOverrideAuthority.foreach(tlsConfigBuilder.overrideAuthority)
        Some(tlsConfigBuilder.build())
      } else None

      // Initialize analytics client
      try {
        val clientBuilder =
          ScalarDbAnalyticsClient.builder().host(serverConfig.host).port(serverConfig.catalogPort)
        _tlsConfig.foreach(clientBuilder.tlsConfig)
        serverConfig.authCredential.foreach { cred =>
          clientBuilder.passwordCredential(new PasswordCredential(cred.username, cred.password))
        }
        _analyticsClient = Some(clientBuilder.build())
      } catch {
        case e: Exception =>
          val endpoint     = s"${serverConfig.host}:${serverConfig.catalogPort.toString}"
          val errorMessage = Option(e.getMessage).filter(_.nonEmpty).getOrElse("unknown error")
          throw ScalarDbAnalyticsCatalogException(
            s"Failed to create analytics client ($endpoint): $errorMessage",
            e
          )
      }

      // Register cleanup listener to close resources on application end
      SparkSession.active.sparkContext.addSparkListener(new CatalogCleanupListener(this))

      JdbcDialects.registerDialect(DatabricksDialect)

      onInitialized()
    }

  override def name(): String = catalogName

  override def listTables(namespace: Array[String]): Array[Identifier] =
    withCatalogOperation {
      if (!checkNamespaceExists(namespace)) {
        throw new NoSuchNamespaceException(namespace)
      }

      val dataSourceName = namespace.headOption.getOrElse("")
      val namespaceNames = namespace.drop(1).toSeq.asJava

      analyticsClient
        .table()
        .listTablesByNamespace(catalogName, dataSourceName, namespaceNames)
        .asScala
        .map { table =>
          val dsName    = table.getDataSource.getName
          val nsNames   = table.getNamespace.getNames.asScala.toSeq
          val tableName = table.getTable.getInfo.getName
          Identifier.of((Seq(dsName) ++ nsNames).toArray, tableName)
        }
        .toArray
    }

  override def listNamespaces(): Array[Array[String]] =
    withCatalogOperation {
      analyticsClient
        .namespace()
        .listNamespacesByCatalog(catalogName)
        .asScala
        .map { dsNamespace =>
          val dsName  = dsNamespace.getDataSource.getName
          val nsNames = dsNamespace.getNamespace.getNames.asScala.toSeq
          (Seq(dsName) ++ nsNames).toArray
        }
        .toArray
    }

  override def listNamespaces(namespace: Array[String]): Array[Array[String]] =
    withCatalogOperation {
      val allNamespaces = listNamespaces()
      val result        = allNamespaces.filter(ns => startsWithNamespace(ns, namespace))
      if (result.isEmpty) {
        throw new NoSuchNamespaceException(namespace)
      }
      result
    }

  override def loadTable(identifier: Identifier): Table =
    withCatalogOperation {
      val catalogTable = CatalogTable(catalogName, identifier, analyticsClient)
      catalogTable.loadSparkTable()
    }

  override def createTable(
      identifier: Identifier,
      schema: StructType,
      partitions: Array[Transform],
      properties: java.util.Map[String, String]
  ): Table =
    throw new UnsupportedOperationException("createTable is not supported")

  override def alterTable(identifier: Identifier, changes: TableChange*): Table =
    throw new UnsupportedOperationException("alterTable is not supported")

  override def dropTable(identifier: Identifier): Boolean =
    throw new UnsupportedOperationException("dropTable is not supported")

  override def renameTable(oldIdentifier: Identifier, newIdentifier: Identifier): Unit =
    throw new UnsupportedOperationException("renameTable is not supported")

  override def loadNamespaceMetadata(namespace: Array[String]): java.util.Map[String, String] =
    Map.empty[String, String].asJava

  override def createNamespace(
      namespace: Array[String],
      metadata: java.util.Map[String, String]
  ): Unit =
    throw new UnsupportedOperationException("createNamespace is not supported")

  override def alterNamespace(namespace: Array[String], changes: NamespaceChange*): Unit =
    throw new UnsupportedOperationException("alterNamespace is not supported")

  override def dropNamespace(namespace: Array[String], cascade: Boolean): Boolean =
    throw new UnsupportedOperationException("dropNamespace is not supported")

  // Wraps catalog and SDK exceptions as AnalysisException so that Spark displays a clean
  // error message instead of a raw stack trace in the spark-sql CLI (PRETTY format).
  // In Spark 3.4/3.5, SparkSQLCLIDriver suppresses the stack trace when the exception
  // is an AnalysisException with no cause (getCause == null).
  // See SparkSQLCLIDriver.processCmd:
  //   if (format == ErrorMessageFormat.PRETTY &&
  //       (!e.isInstanceOf[AnalysisException] || e.getCause != null)) {
  //     e.printStackTrace(err)
  //   }
  // AnalyticsException carries a DB-ANALYTICS error code in its message
  // (e.g. "DB-ANALYTICS-10003: Access denied [...]"), which is the user-facing classifier
  // surfaced here. SQLSTATE is intentionally not set: the spark-sql CLI and DataFrame API do
  // not surface AnalysisException.getSqlState, so it would be invisible. If ScalarDB Analytics
  // is ever served over a Spark Thrift Server (JDBC/ODBC), map AnalyticsException.getErrorCode
  // to a SQLSTATE here (by overriding getSqlState) so JDBC clients can branch on it.
  private def withErrorHandling[T](body: => T): T =
    try body
    catch {
      case e @ (_: ScalarDbAnalyticsCatalogException | _: AnalyticsException) =>
        throw new AnalysisException(e.getMessage, cause = None) {}
    }

  private def withCatalogOperation[T](body: => T): T =
    withErrorHandling {
      beforeOperation()
      body
    }

  protected def serverConfig: ServerConfig =
    _serverConfig.getOrElse(throw new IllegalStateException("Server configuration not initialized"))

  protected def tlsConfig: Option[TlsConfig] = _tlsConfig

  private def checkNamespaceExists(namespace: Array[String]): Boolean =
    try listNamespaces(namespace).nonEmpty
    catch {
      case _: NoSuchNamespaceException => false
    }

  private def startsWithNamespace(namespace: Array[String], prefix: Array[String]): Boolean =
    if (namespace.length < prefix.length) false
    else prefix.indices.forall(i => namespace(i) == prefix(i))

  // Visible for testing
  private[spark] def initializeWithClient(name: String, client: ScalarDbAnalyticsClient): Unit = {
    _catalogName = Some(name)
    _analyticsClient = Some(client)
  }

  private[spark] def closeResources(): Unit =
    // The hook runs first so that a distribution can still reach the analytics client while it
    // shuts down, and the base resources are released in a finally so that a failing hook cannot
    // leak them. The cleanup listener catches around this method, so the hook's exception still
    // surfaces in the log.
    try onClose()
    finally _analyticsClient.foreach(_.close())
}

/** The catalog as distributed in the open-source build, which adds nothing to the base behaviour.
  */
final class ScalarDbAnalyticsCatalog extends ScalarDbAnalyticsCatalogBase {
  override protected def onInitialized(): Unit   = ()
  override protected def beforeOperation(): Unit = ()
  override protected def onClose(): Unit         = ()
}

/** Listener to cleanup catalog resources on application end */
private class CatalogCleanupListener(catalog: ScalarDbAnalyticsCatalogBase)
    extends SparkListener
    with LazyLogging {
  override def onApplicationEnd(applicationEnd: SparkListenerApplicationEnd): Unit =
    Try(catalog.closeResources()).failed.foreach { e =>
      logger.error("Failed to close catalog resources cleanly", e)
    }
}
