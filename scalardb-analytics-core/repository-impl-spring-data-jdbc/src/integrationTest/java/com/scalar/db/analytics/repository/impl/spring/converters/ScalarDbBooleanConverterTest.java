/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.repository.impl.spring.converter.reading.BigDecimalToBooleanConverter;
import com.scalar.db.analytics.repository.impl.spring.converter.reading.IntegerToBooleanConverter;
import com.scalar.db.analytics.repository.impl.spring.converter.writing.BooleanToBigDecimalConverter;
import com.scalar.db.analytics.repository.impl.spring.converter.writing.BooleanToIntegerConverter;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScalarDbBooleanConverterTest {

  @Test
  @DisplayName("Should convert true and false boolean values")
  void shouldConvertTrueAndFalse() {
    BooleanToIntegerConverter toInteger = new BooleanToIntegerConverter();
    IntegerToBooleanConverter toBoolean = new IntegerToBooleanConverter();
    BooleanToBigDecimalConverter toBigDecimal = new BooleanToBigDecimalConverter();
    BigDecimalToBooleanConverter fromBigDecimal = new BigDecimalToBooleanConverter();

    Integer trueAsInt = toInteger.convert(Boolean.TRUE);
    Integer falseAsInt = toInteger.convert(Boolean.FALSE);
    BigDecimal trueAsDecimal = toBigDecimal.convert(Boolean.TRUE);
    BigDecimal falseAsDecimal = toBigDecimal.convert(Boolean.FALSE);

    assertThat(toBoolean.convert(trueAsInt)).isTrue();
    assertThat(toBoolean.convert(falseAsInt)).isFalse();
    assertThat(fromBigDecimal.convert(trueAsDecimal)).isTrue();
    assertThat(fromBigDecimal.convert(falseAsDecimal)).isFalse();
  }

  @Test
  @DisplayName("Should preserve null values when conversion is skipped")
  void shouldPreserveNullValues() {
    Boolean nullable = null;
    BooleanToIntegerConverter toInteger = new BooleanToIntegerConverter();
    IntegerToBooleanConverter toBoolean = new IntegerToBooleanConverter();
    BooleanToBigDecimalConverter toBigDecimal = new BooleanToBigDecimalConverter();
    BigDecimalToBooleanConverter fromBigDecimal = new BigDecimalToBooleanConverter();

    Integer storedInt = nullable == null ? null : toInteger.convert(nullable);
    BigDecimal storedDecimal = nullable == null ? null : toBigDecimal.convert(nullable);

    Boolean restoredFromInt = storedInt == null ? null : toBoolean.convert(storedInt);
    Boolean restoredFromDecimal =
        storedDecimal == null ? null : fromBigDecimal.convert(storedDecimal);

    assertThat(storedInt).isNull();
    assertThat(storedDecimal).isNull();
    assertThat(restoredFromInt).isNull();
    assertThat(restoredFromDecimal).isNull();
  }
}
