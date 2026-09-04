/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.reading;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.repository.impl.spring.converter.StringList;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Converts a JSON string to a {@link StringList} when reading from the database. This is used for
 * databases that don't have native array support (e.g., SQL Server, MySQL, Oracle) and store string
 * arrays as JSON in VARCHAR/TEXT columns. The StringList wrapper is used to bypass Spring Data
 * JDBC's automatic array handling.
 */
@ReadingConverter
public class JsonToStringListConverter implements Converter<String, StringList> {
  private final ObjectMapper objectMapper;

  public JsonToStringListConverter(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  @Override
  @SuppressWarnings("unchecked")
  public StringList convert(String source) {
    try {
      List<String> list = objectMapper.readValue(source, List.class);
      return new StringList(list);
    } catch (Exception e) {
      throw new RuntimeException("Failed to convert JSON to StringList", e);
    }
  }
}
