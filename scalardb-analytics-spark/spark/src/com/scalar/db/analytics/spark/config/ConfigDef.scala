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
  * @param allowEmpty
  *   when true, the value is still trimmed but an empty result is preserved as a valid value (e.g.
  *   an empty password). By default (false), values are trimmed and an empty result is treated as
  *   absent.
  * @param parse
  *   function to convert the raw string value to the target type
  * @tparam T
  *   the target type of this configuration field
  */
final case class ConfigDef[T](
    propertyKey: String,
    envVarName: Option[String],
    default: Option[T],
    allowEmpty: Boolean,
    parse: String => T
)

object ConfigDef {
  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def string(
      propertyKey: String,
      envVarName: String = "",
      allowEmpty: Boolean = false
  ): ConfigDef[String] =
    ConfigDef(propertyKey, envOption(envVarName), None, allowEmpty, identity)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def int(
      propertyKey: String,
      default: Int,
      envVarName: String = ""
  ): ConfigDef[Int] =
    ConfigDef(propertyKey, envOption(envVarName), Some(default), allowEmpty = false, _.toInt)

  @SuppressWarnings(Array("org.wartremover.warts.DefaultArguments"))
  def boolean(
      propertyKey: String,
      default: Boolean,
      envVarName: String = ""
  ): ConfigDef[Boolean] =
    ConfigDef(propertyKey, envOption(envVarName), Some(default), allowEmpty = false, _.toBoolean)

  private def envOption(envVarName: String): Option[String] =
    if (envVarName.isEmpty) None else Some(envVarName)
}
