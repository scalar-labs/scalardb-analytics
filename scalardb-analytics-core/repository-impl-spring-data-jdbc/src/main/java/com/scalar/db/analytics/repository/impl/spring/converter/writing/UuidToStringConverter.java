/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.writing;

import java.util.UUID;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Converts a {@link UUID} object to its string representation when writing to the database. This is
 * necessary for databases that don't have native UUID support (e.g., Oracle, SQL Server) and need
 * to store UUIDs as CHAR(36) strings.
 */
@WritingConverter
public class UuidToStringConverter implements Converter<UUID, String> {
  @Override
  public String convert(UUID source) {
    return source.toString();
  }
}
