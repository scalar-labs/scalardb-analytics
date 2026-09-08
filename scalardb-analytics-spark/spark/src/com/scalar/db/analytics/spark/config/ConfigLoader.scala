/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.config

import scala.util.control.NonFatal

/** Loads typed configuration values from prefix-scoped properties with environment variable
  * overrides.
  *
  * @param resolved
  *   properties with the prefix stripped
  * @param prefix
  *   the original prefix (used in error messages)
  * @param envReader
  *   function to read environment variables
  */
final class ConfigLoader private (
    resolved: Map[String, String],
    prefix: String,
    envReader: String => Option[String]
) {

  /** Gets a required or defaulted configuration value.
    *
    * Resolution order: environment variable > property > default. If no value is found and no
    * default is defined, throws [[IllegalArgumentException]].
    */
  def get[T](defn: ConfigDef[T]): T =
    resolveRaw(defn) match {
      case Some(v) => parseValue(defn, v)
      case None =>
        defn.default.getOrElse(
          throw new IllegalArgumentException(
            s"The required key `${fullKey(defn)}` is missing"
          )
        )
    }

  /** Gets an optional configuration value.
    *
    * Resolution order: environment variable > property. Returns `None` if no value is found.
    */
  def getOption[T](defn: ConfigDef[T]): Option[T] =
    resolveRaw(defn).map(parseValue(defn, _))

  private def resolveRaw[T](defn: ConfigDef[T]): Option[String] = {
    // Normalize each source before merging so an empty/blank env var does not
    // shadow a valid property value (Item 5). The allowEmpty branch still
    // trims; it only opts out of dropping the empty result (Item 6).
    def normalize(s: String): Option[String] = {
      val trimmed = s.trim
      if (defn.allowEmpty) Some(trimmed) else Some(trimmed).filter(_.nonEmpty)
    }

    defn.envVarName
      .flatMap(envReader)
      .flatMap(normalize)
      .orElse(resolved.get(defn.propertyKey).flatMap(normalize))
  }

  private def parseValue[T](defn: ConfigDef[T], raw: String): T =
    try defn.parse(raw)
    catch {
      case NonFatal(e) =>
        throw new IllegalArgumentException(
          s"Invalid value for `${fullKey(defn)}`: ${e.getMessage}",
          e
        )
    }

  private def fullKey(defn: ConfigDef[_]): String =
    if (defn.propertyKey.isEmpty) prefix else s"$prefix.${defn.propertyKey}"
}

object ConfigLoader {
  def apply(
      properties: Map[String, String],
      prefix: String,
      envReader: String => Option[String]
  ): ConfigLoader = {
    val prefixWithDot = prefix + "."
    val extracted: Map[String, String] = properties.collect {
      case (key, value) if key == prefix =>
        "" -> value
      case (key, value) if key.startsWith(prefixWithDot) && key.length > prefixWithDot.length =>
        key.substring(prefixWithDot.length) -> value
    }
    new ConfigLoader(extracted, prefix, envReader)
  }

  def apply(properties: Map[String, String], prefix: String): ConfigLoader =
    apply(properties, prefix, sys.env.get)
}
