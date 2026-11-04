/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.config

import com.scalar.db.analytics.sdk.config.{
  ClientConfigException,
  ClientConfigKey,
  ClientConfigResolver,
  ConfigSource
}

import scala.jdk.CollectionConverters._
import scala.jdk.OptionConverters._

final case class ServerConfig(
    host: String,
    catalogPort: Int,
    tlsEnabled: Boolean,
    caCertFilePath: Option[String],
    tlsOverrideAuthority: Option[String],
    authCredential: Option[AuthCredential]
)

object ServerConfig {
  private val Prefix = "server"

  private val Host: ConfigDef[String]        = ConfigDef.string("host")
  private val CatalogPort: ConfigDef[Int]    = ConfigDef.int("catalog.port", default = 11051)
  private val TlsEnabled: ConfigDef[Boolean] = ConfigDef.boolean("tls.enabled", default = false)
  private val TlsCaRootCertPath: ConfigDef[String]    = ConfigDef.string("tls.ca_root_cert_path")
  private val TlsOverrideAuthority: ConfigDef[String] = ConfigDef.string("tls.override_authority")

  def parse(properties: Map[String, String]): ServerConfig =
    parse(properties, sys.env.get)

  def parse(
      properties: Map[String, String],
      envReader: String => Option[String]
  ): ServerConfig = {
    val loader = ConfigLoader(properties, Prefix, envReader)

    val host        = loader.get(Host)
    val catalogPort = loader.get(CatalogPort)
    val tlsEnabled  = loader.get(TlsEnabled)

    val caCertFilePath       = loader.getOption(TlsCaRootCertPath)
    val tlsOverrideAuthority = loader.getOption(TlsOverrideAuthority)
    val authCredential       = resolveAuthCredential(properties, envReader)

    if (authCredential.isDefined && !tlsEnabled) {
      throw new IllegalArgumentException(
        s"`$Prefix.${TlsEnabled.propertyKey}` must be true when authentication credentials are configured"
      )
    }

    ServerConfig(
      host,
      catalogPort,
      tlsEnabled,
      caCertFilePath,
      tlsOverrideAuthority,
      authCredential
    )
  }

  /** Resolves the credential through the SDK, so that it follows the same rules as the CLI and the
    * SDK: environment variables (`SCALAR_DB_ANALYTICS_CLIENT_AUTH_*`) take precedence over the
    * `server.auth.*` catalog options, a blank username falls through to the next source, and the
    * password is used as is.
    */
  private def resolveAuthCredential(
      properties: Map[String, String],
      envReader: String => Option[String]
  ): Option[AuthCredential] = {
    val options =
      ConfigSource.properties(properties.asJava, (key: ClientConfigKey) => optionKey(key))
    val resolved =
      try ClientConfigResolver.resolve(environment(envReader), options)
      catch {
        case e: ClientConfigException =>
          val other = e.key match {
            case ClientConfigKey.AUTH_USERNAME => ClientConfigKey.AUTH_PASSWORD
            case _                             => ClientConfigKey.AUTH_USERNAME
          }
          throw new IllegalArgumentException(
            s"`${optionKey(e.key)}` is required when `${optionKey(other)}` is set",
            e
          )
      }
    resolved.passwordCredential.toScala.map(c => AuthCredential(c.getUsername, c.getPassword))
  }

  private def optionKey(key: ClientConfigKey): String = s"$Prefix.${key.relativeKey}"

  // ConfigSource follows the Java convention of returning null for an unset variable.
  @SuppressWarnings(Array("org.wartremover.warts.Null"))
  private def environment(envReader: String => Option[String]): ConfigSource =
    ConfigSource.environment((name: String) => envReader(name).orNull)
}
