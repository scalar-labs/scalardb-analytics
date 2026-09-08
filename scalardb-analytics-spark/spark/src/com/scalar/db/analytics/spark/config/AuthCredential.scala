/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.config

final case class AuthCredential(username: String, password: String) {
  override def toString: String =
    s"AuthCredential(username=$username, password=***)"
}
