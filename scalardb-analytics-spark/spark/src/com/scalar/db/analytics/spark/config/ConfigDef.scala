/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.config

/** Declares a typed configuration field.
  *
  * @param propertyKey
  *   the property key used in Spark catalog options (e.g., "auth.username")
  * @param envVarName
  *   the environment variable name that can override the property value
  * @param default
  *   the default value when no value is found
  * @param parse
  *   function to convert the raw string value to the target type
  * @tparam T
  *   the target type of this configuration field
  */
final case class ConfigDef[T](
    propertyKey: String,
    envVarName: Option[String],
    default: Option[T],
    parse: String => T
)

object ConfigDef {
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def string(
      propertyKey: String,
      envVarName: String = ""
  ): ConfigDef[String] =
    ConfigDef(propertyKey, envOption(envVarName), None, identity)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def int(
      propertyKey: String,
      default: Int,
      envVarName: String = ""
  ): ConfigDef[Int] =
    ConfigDef(propertyKey, envOption(envVarName), Some(default), _.toInt)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def boolean(
      propertyKey: String,
      default: Boolean,
      envVarName: String = ""
  ): ConfigDef[Boolean] =
    ConfigDef(propertyKey, envOption(envVarName), Some(default), _.toBoolean)

  private def envOption(envVarName: String): Option[String] =
    if (envVarName.isEmpty) None else Some(envVarName)
}
