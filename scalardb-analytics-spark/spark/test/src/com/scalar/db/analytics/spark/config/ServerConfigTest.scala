/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.config

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServerConfigTest extends AnyFunSpec with Matchers {

  describe("AuthCredential") {
    it("redacts password in toString") {
      val credential = AuthCredential("admin", "secret")
      val str        = credential.toString
      str should include("admin")
      str should not include "secret"
    }
  }

  describe("ServerConfig.parse") {
    it("parses basic configuration") {
      val properties = Map(
        "server.host" -> "localhost"
      )

      val config = ServerConfig.parse(properties)
      config.host shouldBe "localhost"
      config.catalogPort shouldBe 11051
      config.tlsEnabled shouldBe false
      config.caCertFilePath shouldBe None
      config.tlsOverrideAuthority shouldBe None
    }

    it("parses configuration with a custom catalog port") {
      val properties = Map(
        "server.host"         -> "localhost",
        "server.catalog.port" -> "50051"
      )

      val config = ServerConfig.parse(properties)
      config.catalogPort shouldBe 50051
    }

    it("parses configuration with TLS enabled") {
      val properties = Map(
        "server.host"                   -> "localhost",
        "server.catalog.port"           -> "50051",
        "server.tls.enabled"            -> "true",
        "server.tls.ca_root_cert_path"  -> "/path/to/ca-cert.pem",
        "server.tls.override_authority" -> "test.server.com"
      )

      val config = ServerConfig.parse(properties)
      config.tlsEnabled shouldBe true
      config.caCertFilePath shouldBe Some("/path/to/ca-cert.pem")
      config.tlsOverrideAuthority shouldBe Some("test.server.com")
    }

    it("treats empty CA cert path as None") {
      val properties = Map(
        "server.host"                  -> "localhost",
        "server.catalog.port"          -> "50051",
        "server.tls.enabled"           -> "true",
        "server.tls.ca_root_cert_path" -> "   "
      )

      val config = ServerConfig.parse(properties)
      config.caCertFilePath shouldBe None
    }

    it("treats empty override authority as None") {
      val properties = Map(
        "server.host"                   -> "localhost",
        "server.catalog.port"           -> "50051",
        "server.tls.enabled"            -> "true",
        "server.tls.override_authority" -> ""
      )

      val config = ServerConfig.parse(properties)
      config.tlsOverrideAuthority shouldBe None
    }

    it("parses configuration with auth credentials and TLS") {
      val properties = Map(
        "server.host"          -> "localhost",
        "server.tls.enabled"   -> "true",
        "server.auth.username" -> "admin",
        "server.auth.password" -> "secret"
      )

      val config = ServerConfig.parse(properties)
      config.authCredential shouldBe Some(AuthCredential("admin", "secret"))
    }

    it("throws when auth credentials are configured without TLS") {
      val properties = Map(
        "server.host"          -> "localhost",
        "server.auth.username" -> "admin",
        "server.auth.password" -> "secret"
      )

      val exception = the[IllegalArgumentException] thrownBy {
        ServerConfig.parse(properties)
      }
      exception.getMessage should include("tls.enabled")
    }

    it("parses configuration without auth credentials") {
      val properties = Map(
        "server.host" -> "localhost"
      )

      val config = ServerConfig.parse(properties)
      config.authCredential shouldBe None
    }

    it("throws when only auth username is provided") {
      val properties = Map(
        "server.host"          -> "localhost",
        "server.auth.username" -> "admin"
      )

      val exception = the[IllegalArgumentException] thrownBy {
        ServerConfig.parse(properties)
      }
      exception.getMessage should include("auth.password")
    }

    it("throws when only auth password is provided") {
      val properties = Map(
        "server.host"          -> "localhost",
        "server.auth.password" -> "secret"
      )

      val exception = the[IllegalArgumentException] thrownBy {
        ServerConfig.parse(properties)
      }
      exception.getMessage should include("auth.username")
    }

    it("treats blank username as unset") {
      val properties = Map(
        "server.host"          -> "localhost",
        "server.auth.username" -> "   "
      )

      val config = ServerConfig.parse(properties)
      config.authCredential shouldBe None
    }

    it("throws when blank username is provided with password") {
      val properties = Map(
        "server.host"          -> "localhost",
        "server.tls.enabled"   -> "true",
        "server.auth.username" -> "   ",
        "server.auth.password" -> "secret"
      )

      val exception = the[IllegalArgumentException] thrownBy {
        ServerConfig.parse(properties)
      }
      exception.getMessage should include("auth.username")
    }

    it("allows empty password with valid username") {
      val properties = Map(
        "server.host"          -> "localhost",
        "server.tls.enabled"   -> "true",
        "server.auth.username" -> "admin",
        "server.auth.password" -> ""
      )

      val config = ServerConfig.parse(properties)
      config.authCredential shouldBe Some(AuthCredential("admin", ""))
    }

    it("throws when required fields are missing") {
      val missingHost = Map.empty[String, String]

      an[IllegalArgumentException] should be thrownBy {
        ServerConfig.parse(missingHost)
      }

      val missingPort = Map("server.host" -> "localhost", "server.catalog.port" -> "not_a_number")

      an[IllegalArgumentException] should be thrownBy {
        ServerConfig.parse(missingPort)
      }
    }
  }

  describe("ServerConfig.parse with envReader") {
    val noEnv: String => Option[String] = _ => None

    it("reads auth credentials from environment variables") {
      val properties = Map(
        "server.host"        -> "localhost",
        "server.tls.enabled" -> "true"
      )
      val env = Map(
        "SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME" -> "env-admin",
        "SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD" -> "env-secret"
      )

      val config = ServerConfig.parse(properties, env.get)
      config.authCredential shouldBe Some(AuthCredential("env-admin", "env-secret"))
    }

    it("env vars override property values") {
      val properties = Map(
        "server.host"          -> "localhost",
        "server.tls.enabled"   -> "true",
        "server.auth.username" -> "prop-user",
        "server.auth.password" -> "prop-pass"
      )
      val env = Map(
        "SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME" -> "env-user",
        "SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD" -> "env-pass"
      )

      val config = ServerConfig.parse(properties, env.get)
      config.authCredential shouldBe Some(AuthCredential("env-user", "env-pass"))
    }

    it("falls back to properties when env vars are not set") {
      val properties = Map(
        "server.host"          -> "localhost",
        "server.tls.enabled"   -> "true",
        "server.auth.username" -> "prop-user",
        "server.auth.password" -> "prop-pass"
      )

      val config = ServerConfig.parse(properties, noEnv)
      config.authCredential shouldBe Some(AuthCredential("prop-user", "prop-pass"))
    }

    it("throws when only env username is set without password") {
      val properties = Map(
        "server.host"        -> "localhost",
        "server.tls.enabled" -> "true"
      )
      val env = Map(
        "SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME" -> "env-admin"
      )

      val exception = the[IllegalArgumentException] thrownBy {
        ServerConfig.parse(properties, env.get)
      }
      exception.getMessage should include("auth.password")
    }

    it("throws when env credentials are set without TLS") {
      val properties = Map(
        "server.host" -> "localhost"
      )
      val env = Map(
        "SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME" -> "env-admin",
        "SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD" -> "env-secret"
      )

      val exception = the[IllegalArgumentException] thrownBy {
        ServerConfig.parse(properties, env.get)
      }
      exception.getMessage should include("tls.enabled")
    }

    it("does not read env vars for keys without envVarName") {
      val properties = Map(
        "server.host" -> "localhost"
      )

      val config = ServerConfig.parse(properties, noEnv)
      config.host shouldBe "localhost"
      config.authCredential shouldBe None
    }
  }
}
