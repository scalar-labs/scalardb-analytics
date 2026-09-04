/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.config

case class Config(
    serverConfig: ServerConfig
)

object Config {
  def parse(properties: Map[String, String]): Config =
    parse(properties, sys.env.get)

  def parse(
      properties: Map[String, String],
      envReader: String => Option[String]
  ): Config = {
    val serverConfig = ServerConfig.parse(properties, envReader)
    Config(serverConfig)
  }
}
