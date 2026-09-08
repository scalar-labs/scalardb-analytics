/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converter.writing;

import java.math.BigDecimal;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;

/**
 * Converts a boolean value to a {@link BigDecimal} when writing to the database. This is
 * specifically needed for Oracle which uses NUMBER(1) to represent boolean values and expects
 * BigDecimal objects. Convention: false = 0, true = 1.
 */
@WritingConverter
public class BooleanToBigDecimalConverter implements Converter<Boolean, BigDecimal> {
  @Override
  public BigDecimal convert(Boolean source) {
    return source ? BigDecimal.ONE : BigDecimal.ZERO;
  }
}
