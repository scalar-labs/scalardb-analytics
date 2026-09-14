/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.config

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ConfigTest extends AnyFlatSpec with Matchers {

  "Config.parse" should "return config with required fields" in {
    val map = Map(
      "server.host"         -> "localhost",
      "server.catalog.port" -> "11052"
    )

    val config = Config.parse(map)
    val server = config.serverConfig
    server.host shouldBe "localhost"
    server.catalogPort shouldBe 11052
    server.tlsEnabled shouldBe false
    server.caCertFilePath shouldBe None
    server.tlsOverrideAuthority shouldBe None
  }

  it should "return config with TLS enabled" in {
    val map = Map(
      "server.host"                  -> "localhost",
      "server.catalog.port"          -> "11052",
      "server.tls.enabled"           -> "true",
      "server.tls.ca_root_cert_path" -> "/path/to/ca-cert.pem"
    )

    val config = Config.parse(map)
    val server = config.serverConfig
    server.host shouldBe "localhost"
    server.catalogPort shouldBe 11052
    server.tlsEnabled shouldBe true
    server.caCertFilePath shouldBe Some("/path/to/ca-cert.pem")
  }

  it should "use system default when TLS enabled but missing CA cert" in {
    val map = Map(
      "server.host"         -> "localhost",
      "server.catalog.port" -> "11052",
      "server.tls.enabled"  -> "true"
    )

    val config = Config.parse(map)
    val server = config.serverConfig
    server.host shouldBe "localhost"
    server.catalogPort shouldBe 11052
    server.tlsEnabled shouldBe true
    server.caCertFilePath shouldBe None
  }

  it should "use system default when TLS enabled with empty CA cert" in {
    val map = Map(
      "server.host"                  -> "localhost",
      "server.catalog.port"          -> "11052",
      "server.tls.enabled"           -> "true",
      "server.tls.ca_root_cert_path" -> ""
    )

    val config = Config.parse(map)
    val server = config.serverConfig
    server.host shouldBe "localhost"
    server.catalogPort shouldBe 11052
    server.tlsEnabled shouldBe true
    server.caCertFilePath shouldBe None
  }

  it should "use system default when TLS enabled with whitespace CA cert" in {
    val map = Map(
      "server.host"                  -> "localhost",
      "server.catalog.port"          -> "11052",
      "server.tls.enabled"           -> "true",
      "server.tls.ca_root_cert_path" -> "   "
    )

    val config = Config.parse(map)
    val server = config.serverConfig
    server.host shouldBe "localhost"
    server.catalogPort shouldBe 11052
    server.tlsEnabled shouldBe true
    server.caCertFilePath shouldBe None
  }

  it should "parse TLS override authority when provided" in {
    val map = Map(
      "server.host"                   -> "localhost",
      "server.catalog.port"           -> "11052",
      "server.tls.enabled"            -> "true",
      "server.tls.override_authority" -> "test.example.com"
    )

    val config = Config.parse(map)
    val server = config.serverConfig
    server.tlsOverrideAuthority shouldBe Some("test.example.com")
  }

  it should "return config with auth credentials and TLS" in {
    val map = Map(
      "server.host"          -> "localhost",
      "server.tls.enabled"   -> "true",
      "server.auth.username" -> "admin",
      "server.auth.password" -> "secret"
    )

    val config = Config.parse(map)
    val server = config.serverConfig
    server.authCredential shouldBe Some(AuthCredential("admin", "secret"))
  }

  it should "throw IllegalArgumentException when only auth username is provided" in {
    val map = Map(
      "server.host"          -> "localhost",
      "server.auth.username" -> "admin"
    )

    an[IllegalArgumentException] should be thrownBy Config.parse(map)
  }

  it should "throw IllegalArgumentException when only auth password is provided" in {
    val map = Map(
      "server.host"          -> "localhost",
      "server.auth.password" -> "secret"
    )

    an[IllegalArgumentException] should be thrownBy Config.parse(map)
  }

  it should "throw IllegalArgumentException when host is missing" in {
    val map = Map("server.catalog.port" -> "11052")

    val exception = intercept[IllegalArgumentException] {
      Config.parse(map)
    }
    exception.getMessage should include("The required key `server.host` is missing")
  }

  it should "throw IllegalArgumentException when catalog port is not numeric" in {
    val map = Map(
      "server.host"         -> "localhost",
      "server.catalog.port" -> "not-a-number"
    )

    val exception = intercept[IllegalArgumentException] {
      Config.parse(map)
    }
    exception.getMessage should include("server.catalog.port")
  }

  it should "read auth credentials from environment variables" in {
    val map = Map(
      "server.host"        -> "localhost",
      "server.tls.enabled" -> "true"
    )
    val env = Map(
      "SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME" -> "env-admin",
      "SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD" -> "env-secret"
    )

    val config = Config.parse(map, env.get)
    val server = config.serverConfig
    server.authCredential shouldBe Some(AuthCredential("env-admin", "env-secret"))
  }

  it should "let env vars override property values for credentials" in {
    val map = Map(
      "server.host"          -> "localhost",
      "server.tls.enabled"   -> "true",
      "server.auth.username" -> "prop-user",
      "server.auth.password" -> "prop-pass"
    )
    val env = Map(
      "SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME" -> "env-user",
      "SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD" -> "env-pass"
    )

    val config = Config.parse(map, env.get)
    val server = config.serverConfig
    server.authCredential shouldBe Some(AuthCredential("env-user", "env-pass"))
  }

  it should "use default envReader (sys.env.get) when envReader is not provided" in {
    val map = Map(
      "server.host" -> "localhost"
    )

    val config = Config.parse(map)
    config.serverConfig.host shouldBe "localhost"
  }
}
