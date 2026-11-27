/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.config

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

import java.nio.charset.StandardCharsets
import scala.io.Source

/** Checks the connector against the credential resolution case table shared with the SDK and the
  * CLI (`credential-resolution-cases.tsv` in client-sdk), so that equivalent inputs resolve to the
  * same credential in every entry point. The catalog options `server.auth.*` play the role of the
  * configuration file.
  */
class CredentialResolutionConformanceTest extends AnyFunSpec with Matchers {
  import CredentialResolutionConformanceTest._

  describe("ServerConfig.parse with the shared credential resolution cases") {
    loadCases().foreach { c =>
      it(c.name) {
        val properties = Map(
          "server.host"        -> "localhost",
          "server.tls.enabled" -> "true"
        ) ++ c.fileUsername.map("server.auth.username" -> _).toList ++
          c.filePassword.map("server.auth.password" -> _).toList
        val env = (c.envUsername.map(UsernameEnv -> _).toList ++
          c.envPassword.map(PasswordEnv -> _).toList).toMap

        c.missing match {
          case Some(key) =>
            val exception = the[IllegalArgumentException] thrownBy {
              ServerConfig.parse(properties, env.get)
            }
            exception.getMessage should startWith(s"`server.$key` is required")
          case None =>
            val expected = for {
              u <- c.expectedUsername
              p <- c.expectedPassword
            } yield AuthCredential(u, p)
            ServerConfig.parse(properties, env.get).authCredential shouldBe expected
        }
      }
    }
  }
}

object CredentialResolutionConformanceTest {
  private val UsernameEnv = "SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME"
  private val PasswordEnv = "SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD"
  private val Resource    = "/credential-resolution-cases.tsv"

  final case class Case(
      name: String,
      envUsername: Option[String],
      envPassword: Option[String],
      fileUsername: Option[String],
      filePassword: Option[String],
      expectedUsername: Option[String],
      expectedPassword: Option[String],
      missing: Option[String]
  )

  def loadCases(): List[Case] = {
    val stream = Option(getClass.getResourceAsStream(Resource))
      .getOrElse(throw new IllegalStateException(s"$Resource is not on the test classpath"))
    val source = Source.fromInputStream(stream, StandardCharsets.UTF_8.name)
    try
      source
        .getLines()
        .filterNot(line => line.isEmpty || line.startsWith("#"))
        .drop(1) // header
        .map(parse)
        .toList match {
        case Nil   => throw new IllegalStateException(s"$Resource has no cases")
        case cases => cases
      }
    finally source.close()
  }

  private def parse(line: String): Case =
    line.split("\t", -1).toList match {
      case List(name, eu, ep, fu, fp, xu, xp, missing) =>
        Case(
          name,
          cell(eu),
          cell(ep),
          cell(fu),
          cell(fp),
          cell(xu),
          cell(xp),
          if (missing == "-") None else Some(missing)
        )
      case _ => throw new IllegalArgumentException(s"Invalid row: $line")
    }

  private def cell(value: String): Option[String] =
    if (value == "-") None
    else if (value.length >= 2 && value.startsWith("\"") && value.endsWith("\""))
      Some(value.substring(1, value.length - 1))
    else throw new IllegalArgumentException(s"Invalid cell: $value")
}
