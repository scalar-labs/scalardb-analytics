/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.reading;

import com.scalar.db.analytics.repository.impl.spring.converter.StringList;
import java.util.List;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Converts a native PostgreSQL array to a {@link StringList} when reading from the database. This
 * is specifically for PostgreSQL which has native array support and stores string arrays directly
 * in TEXT[] columns. The StringList wrapper is used to bypass Spring Data JDBC's automatic array
 * handling.
 */
@ReadingConverter
public class PostgreSqlArrayToStringListConverter implements Converter<String[], StringList> {
  @Override
  public StringList convert(String[] source) {
    return new StringList(List.of(source));
  }
}
