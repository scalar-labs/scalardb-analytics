/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.writing;

import com.scalar.db.analytics.repository.impl.spring.converter.StringList;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Converts a {@link StringList} to a native PostgreSQL array when writing to the database. This is
 * specifically for PostgreSQL which has native array support and can store string arrays directly
 * in TEXT[] columns. The StringList wrapper is used to bypass Spring Data JDBC's automatic array
 * handling.
 */
@WritingConverter
public class StringListToPostgreSqlArrayConverter implements Converter<StringList, String[]> {
  @Override
  public String[] convert(StringList source) {
    return source.values().toArray(new String[0]);
  }
}
