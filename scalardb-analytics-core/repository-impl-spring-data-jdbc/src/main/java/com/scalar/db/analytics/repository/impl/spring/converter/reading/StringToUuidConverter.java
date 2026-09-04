/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.reading;

import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Converts a string representation of UUID to a {@link UUID} object when reading from the database.
 * This is necessary for databases that don't have native UUID support (e.g., Oracle, SQL Server)
 * and store UUIDs as CHAR(36) strings.
 */
@ReadingConverter
public class StringToUuidConverter implements Converter<String, UUID> {
  @Override
  public UUID convert(String source) {
    return UUID.fromString(source);
  }
}
