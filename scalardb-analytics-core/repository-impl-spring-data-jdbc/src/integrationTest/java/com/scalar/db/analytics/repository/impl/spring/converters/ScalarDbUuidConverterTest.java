/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converters;

import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.repository.impl.spring.converter.reading.StringToUuidConverter;
import com.scalar.db.analytics.repository.impl.spring.converter.writing.UuidToStringConverter;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScalarDbUuidConverterTest {

  @Test
  @DisplayName("Should convert UUID between Java and database")
  void uuidShouldRoundTripThroughStringConverters() {
    UUID original = UUID.randomUUID();

    String serialized = new UuidToStringConverter().convert(original);
    UUID restored = new StringToUuidConverter().convert(serialized);

    assertThat(restored).isEqualTo(original);
  }
}
