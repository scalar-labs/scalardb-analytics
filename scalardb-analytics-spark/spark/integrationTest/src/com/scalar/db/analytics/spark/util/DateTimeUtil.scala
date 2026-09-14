/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */

package com.scalar.db.analytics.spark.util

import java.time.{LocalDate, LocalDateTime, LocalTime, ZoneId}

object DateTimeUtil {
  def parseLocalTime(time: String): LocalDateTime =
    toLocalDateTimeWithEpochDate(LocalTime.parse(time))

  def toLocalDateTimeWithEpochDate(time: LocalTime): LocalDateTime =
    LocalDateTime.of(LocalDate.ofEpochDay(0), time)

  def localToUtc(localDateTime: LocalDateTime): LocalDateTime =
    localDateTime
      .atZone(ZoneId.systemDefault())
      .withZoneSameInstant(ZoneId.of("UTC"))
      .toLocalDateTime
}
