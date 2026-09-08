/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.writing;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Converts a boolean value to an integer when writing to the database. This is necessary for
 * databases that don't have a native boolean type (e.g., MySQL) and use TINYINT or SMALLINT to
 * represent boolean values. Convention: false = 0, true = 1.
 */
@WritingConverter
public class BooleanToIntegerConverter implements Converter<Boolean, Integer> {
  @Override
  public Integer convert(Boolean source) {
    return source ? 1 : 0;
  }
}
