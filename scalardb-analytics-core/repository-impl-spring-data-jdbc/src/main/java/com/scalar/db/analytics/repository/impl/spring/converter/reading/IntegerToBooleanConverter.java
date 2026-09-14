/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.reading;

import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Converts an integer value to a boolean when reading from the database. This is necessary for
 * databases that don't have a native boolean type (e.g., MySQL) and use TINYINT or SMALLINT to
 * represent boolean values. Convention: 0 = false, non-zero = true.
 */
@ReadingConverter
public class IntegerToBooleanConverter implements Converter<Integer, Boolean> {
  @Override
  public Boolean convert(Integer source) {
    return source != 0;
  }
}
