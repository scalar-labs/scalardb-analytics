/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.reading;

import java.math.BigDecimal;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;

/**
 * Converts a {@link BigDecimal} value to a boolean when reading from the database. This is
 * specifically needed for Oracle which returns NUMBER(1) as BigDecimal when representing boolean
 * values. Convention: 0 = false, non-zero = true.
 */
@ReadingConverter
public class BigDecimalToBooleanConverter implements Converter<BigDecimal, Boolean> {
  @Override
  public Boolean convert(BigDecimal source) {
    return source.compareTo(BigDecimal.ZERO) != 0;
  }
}
