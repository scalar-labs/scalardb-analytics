/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.config

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
  private val AuthUsername: ConfigDef[String] =
    ConfigDef.string("auth.username", envVarName = "SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME")
  private val AuthPassword: ConfigDef[String] =
    ConfigDef.string(
      "auth.password",
      envVarName = "SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD",
      allowEmpty = true
    )

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
    val authUsername         = loader.getOption(AuthUsername)

    // Password is not trimmed or filtered: empty passwords may be valid.
    val authPassword = loader.getOption(AuthPassword)

    val authCredential = (authUsername, authPassword) match {
      case (Some(u), Some(p)) => Some(AuthCredential(u, p))
      case (None, None)       => None
      case (Some(_), None) =>
        throw new IllegalArgumentException(
          s"`$Prefix.${AuthPassword.propertyKey}` is required when `$Prefix.${AuthUsername.propertyKey}` is set"
        )
      case (None, Some(_)) =>
        throw new IllegalArgumentException(
          s"`$Prefix.${AuthUsername.propertyKey}` is required when `$Prefix.${AuthPassword.propertyKey}` is set"
        )
    }

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
}
