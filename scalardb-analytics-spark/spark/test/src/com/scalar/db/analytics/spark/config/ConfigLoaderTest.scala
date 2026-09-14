/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.config

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ConfigLoaderTest extends AnyFunSpec with Matchers {

  private val noEnv: String => Option[String] = _ => None

  describe("ConfigLoader") {

    describe("prefix extraction") {
      it("extracts entries matching the prefix and strips it") {
        val properties = Map("foo.bar" -> "1", "foo.baz" -> "2", "other.key" -> "3")
        val loader     = ConfigLoader(properties, "foo", noEnv)

        loader.getOption(ConfigDef.string("bar")) shouldBe Some("1")
        loader.getOption(ConfigDef.string("baz")) shouldBe Some("2")
      }

      it("strips the dot separator after the prefix") {
        val properties = Map("prefix.value" -> "v")
        val loader     = ConfigLoader(properties, "prefix", noEnv)

        loader.getOption(ConfigDef.string("value")) shouldBe Some("v")
      }

      it("returns None for keys not matching the prefix") {
        val properties = Map("abc.def" -> "1")
        val loader     = ConfigLoader(properties, "zzz", noEnv)

        loader.getOption(ConfigDef.string("def")) shouldBe None
      }

      it("does not match keys that share the prefix string but lack a dot boundary") {
        val properties = Map("foobar.key" -> "v", "foo.real" -> "ok")
        val loader     = ConfigLoader(properties, "foo", noEnv)

        loader.getOption(ConfigDef.string("bar.key")) shouldBe None
        loader.getOption(ConfigDef.string("real")) shouldBe Some("ok")
      }

      it("ignores a bare `prefix.` key with no property name after the dot") {
        val properties = Map("foo." -> "v", "foo.real" -> "ok")
        val loader     = ConfigLoader(properties, "foo", noEnv)

        loader.getOption(ConfigDef.string("", allowEmpty = true)) shouldBe None
        loader.getOption(ConfigDef.string("real")) shouldBe Some("ok")
      }
    }

    describe("typed parsing") {
      it("parses string values") {
        val loader = ConfigLoader(Map("s.name" -> "hello"), "s", noEnv)

        loader.getOption(ConfigDef.string("name")) shouldBe Some("hello")
      }

      it("parses int values") {
        val loader = ConfigLoader(Map("s.port" -> "8080"), "s", noEnv)

        loader.get(ConfigDef.int("port", default = 0)) shouldBe 8080
      }

      it("parses boolean values") {
        val loader = ConfigLoader(Map("s.enabled" -> "true"), "s", noEnv)

        loader.get(ConfigDef.boolean("enabled", default = false)) shouldBe true
      }

      it("throws on parse failure with key name in message") {
        val loader = ConfigLoader(Map("s.port" -> "not-a-number"), "s", noEnv)

        val exception = the[IllegalArgumentException] thrownBy {
          loader.get(ConfigDef.int("port", default = 0))
        }
        exception.getMessage should include("s.port")
      }
    }

    describe("get (required / defaulted)") {
      it("returns value when present") {
        val loader = ConfigLoader(Map("s.host" -> "localhost"), "s", noEnv)

        loader.get(ConfigDef.string("host")) shouldBe "localhost"
      }

      it("returns default when value is missing") {
        val loader = ConfigLoader(Map.empty[String, String], "s", noEnv)

        loader.get(ConfigDef.int("port", default = 8080)) shouldBe 8080
      }

      it("throws when value is missing and no default") {
        val loader = ConfigLoader(Map.empty[String, String], "s", noEnv)

        val exception = the[IllegalArgumentException] thrownBy {
          loader.get(ConfigDef.string("host"))
        }
        exception.getMessage shouldBe "The required key `s.host` is missing"
      }

      it("prefers env var over property") {
        val env                     = Map("MY_HOST" -> "env-host")
        val loader                  = ConfigLoader(Map("s.host" -> "prop-host"), "s", env.get)
        val defn: ConfigDef[String] = ConfigDef("host", Some("MY_HOST"), None, false, identity)

        loader.get(defn) shouldBe "env-host"
      }

      it("prefers env var over default") {
        val env                  = Map("MY_PORT" -> "9090")
        val loader               = ConfigLoader(Map.empty[String, String], "s", env.get)
        val defn: ConfigDef[Int] = ConfigDef("port", Some("MY_PORT"), Some(8080), false, _.toInt)

        loader.get(defn) shouldBe 9090
      }

      it("reads int env var via ConfigDef.int helper") {
        val env    = Map("MY_PORT" -> "9090")
        val loader = ConfigLoader(Map("s.port" -> "8080"), "s", env.get)

        loader.get(ConfigDef.int("port", default = 0, envVarName = "MY_PORT")) shouldBe 9090
      }

      it("reads boolean env var via ConfigDef.boolean helper") {
        val env    = Map("MY_FLAG" -> "true")
        val loader = ConfigLoader(Map("s.flag" -> "false"), "s", env.get)

        loader.get(
          ConfigDef.boolean("flag", default = false, envVarName = "MY_FLAG")
        ) shouldBe true
      }
    }

    describe("getOption (optional)") {
      it("returns Some when value is present") {
        val loader = ConfigLoader(Map("s.key" -> "val"), "s", noEnv)

        loader.getOption(ConfigDef.string("key")) shouldBe Some("val")
      }

      it("returns None when value is missing") {
        val loader = ConfigLoader(Map.empty[String, String], "s", noEnv)

        loader.getOption(ConfigDef.string("key")) shouldBe None
      }

      it("returns env var value when property is missing") {
        val env    = Map("MY_KEY" -> "env-val")
        val loader = ConfigLoader(Map.empty[String, String], "s", env.get)
        val defn   = ConfigDef.string("key", "MY_KEY")

        loader.getOption(defn) shouldBe Some("env-val")
      }

      it("skips env lookup for defs without envVarName") {
        val envReader: String => Option[String] = _ => Some("should-not-appear")
        val loader = ConfigLoader(Map("s.host" -> "original"), "s", envReader)

        loader.getOption(ConfigDef.string("host")) shouldBe Some("original")
      }
    }

    describe("trim and empty handling (default behavior)") {
      it("trims whitespace from values by default") {
        val loader = ConfigLoader(Map("s.name" -> "  hello  "), "s", noEnv)

        loader.getOption(ConfigDef.string("name")) shouldBe Some("hello")
      }

      it("treats whitespace-only values as absent by default") {
        val loader = ConfigLoader(Map("s.name" -> "   "), "s", noEnv)

        loader.getOption(ConfigDef.string("name")) shouldBe None
      }

      it("treats empty values as absent by default") {
        val loader = ConfigLoader(Map("s.name" -> ""), "s", noEnv)

        loader.getOption(ConfigDef.string("name")) shouldBe None
      }

      it("falls back to default when trimmed value is empty") {
        val loader                  = ConfigLoader(Map("s.name" -> "   "), "s", noEnv)
        val defn: ConfigDef[String] = ConfigDef("name", None, Some("fallback"), false, identity)

        loader.get(defn) shouldBe "fallback"
      }
    }

    describe("allowEmpty") {
      it("trims whitespace but preserves the trimmed value when allowEmpty is true") {
        val loader = ConfigLoader(Map("s.pass" -> "  secret  "), "s", noEnv)

        loader.getOption(ConfigDef.string("pass", allowEmpty = true)) shouldBe Some("secret")
      }

      it("preserves empty string when allowEmpty is true") {
        val loader = ConfigLoader(Map("s.pass" -> ""), "s", noEnv)

        loader.getOption(ConfigDef.string("pass", allowEmpty = true)) shouldBe Some("")
      }

      it("normalizes whitespace-only value to empty when allowEmpty is true") {
        val loader = ConfigLoader(Map("s.pass" -> "   "), "s", noEnv)

        loader.getOption(ConfigDef.string("pass", allowEmpty = true)) shouldBe Some("")
      }
    }

    describe("per-source normalization (env vs property)") {
      it("falls back to property when env var is empty and allowEmpty is false") {
        val env    = Map("MY_USER" -> "")
        val loader = ConfigLoader(Map("s.user" -> "alice"), "s", env.get)

        loader.getOption(ConfigDef.string("user", envVarName = "MY_USER")) shouldBe Some("alice")
      }

      it("falls back to property when env var is whitespace-only and allowEmpty is false") {
        val env    = Map("MY_USER" -> "   ")
        val loader = ConfigLoader(Map("s.user" -> "alice"), "s", env.get)

        loader.getOption(ConfigDef.string("user", envVarName = "MY_USER")) shouldBe Some("alice")
      }

      it("lets an empty env var win over a property when allowEmpty is true") {
        val env    = Map("MY_PASS" -> "")
        val loader = ConfigLoader(Map("s.pass" -> "secret"), "s", env.get)
        val defn: ConfigDef[String] =
          ConfigDef.string("pass", envVarName = "MY_PASS", allowEmpty = true)

        loader.getOption(defn) shouldBe Some("")
      }

      it("trims a padded env value when allowEmpty is true") {
        val env    = Map("MY_PASS" -> "  secret  ")
        val loader = ConfigLoader(Map.empty[String, String], "s", env.get)
        val defn: ConfigDef[String] =
          ConfigDef.string("pass", envVarName = "MY_PASS", allowEmpty = true)

        loader.getOption(defn) shouldBe Some("secret")
      }

      it("normalizes whitespace-only env to empty and wins over property when allowEmpty is true") {
        val env    = Map("MY_PASS" -> "   ")
        val loader = ConfigLoader(Map("s.pass" -> "prop-secret"), "s", env.get)
        val defn: ConfigDef[String] =
          ConfigDef.string("pass", envVarName = "MY_PASS", allowEmpty = true)

        loader.getOption(defn) shouldBe Some("")
      }
    }
  }
}
