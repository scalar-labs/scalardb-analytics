/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.exception

/** Exception thrown when the ScalarDB analytics catalog encounters an unrecoverable error. */
final class ScalarDbAnalyticsCatalogException private (
    message: String,
    cause: Option[Throwable]
) extends RuntimeException(message) {
  cause.foreach(initCause)
}

object ScalarDbAnalyticsCatalogException {
  def apply(message: String, cause: Throwable): ScalarDbAnalyticsCatalogException =
    new ScalarDbAnalyticsCatalogException(message, Some(cause))

  def apply(message: String): ScalarDbAnalyticsCatalogException =
    new ScalarDbAnalyticsCatalogException(message, None)
}
