/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.reading;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Converts PostgreSQL's PGobject to a string when reading from the database. PostgreSQL JDBC driver
 * returns JSON/JSONB columns as PGobject instances rather than strings. This converter extracts the
 * actual string value from the PGobject using reflection to avoid direct dependency on PostgreSQL
 * JDBC driver.
 */
@ReadingConverter
public class PGobjectToStringConverter implements Converter<Object, String> {
  @Override
  public String convert(Object source) {
    // Handle PostgreSQL's PGobject for JSON columns
    if (source.getClass().getName().equals("org.postgresql.util.PGobject")) {
      try {
        java.lang.reflect.Method getValue = source.getClass().getMethod("getValue");
        return (String) getValue.invoke(source);
      } catch (Exception e) {
        throw new RuntimeException("Failed to convert PGobject to String", e);
      }
    }
    return source.toString();
  }
}
