/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.writing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.repository.impl.spring.converter.StringList;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Converts a {@link StringList} to a JSON string when writing to the database. This is used for
 * databases that don't have native array support (e.g., SQL Server, MySQL, Oracle) and need to
 * store string arrays as JSON in VARCHAR/TEXT columns. The StringList wrapper is used to bypass
 * Spring Data JDBC's automatic array handling.
 */
@WritingConverter
public class StringListToJsonConverter implements Converter<StringList, String> {
  private final ObjectMapper objectMapper;

  public StringListToJsonConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  public String convert(StringList source) {
    try {
      return objectMapper.writeValueAsString(source.values());
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert StringList to JSON", e);
    }
  }
}
