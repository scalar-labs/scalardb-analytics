/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.codec.CodecObjectMapperFactory;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class ScalarDbProviderCodecPropertyTest {

  private final ScalarDbProviderCodec codec;

  {
    ObjectMapper objectMapper = CodecObjectMapperFactory.create();
    codec = new ScalarDbProviderCodec(objectMapper);
  }

  @Property
  void roundTrip_shouldPreserveConfigs(@ForAll("configs") Map<String, String> configs) {
    // Given
    ScalarDbProvider original = new ScalarDbProvider(configs);

    // When
    JsonNode json = codec.serialize(original);
    ScalarDbProvider roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped.getConfigs()).isEqualTo(configs);
  }

  @Provide
  Arbitrary<Map<String, String>> configs() {
    return Arbitraries.maps(
            Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('.', '-', '_')
                .ofMinLength(1)
                .ofMaxLength(50),
            Arbitraries.strings()
                .alpha()
                .numeric()
                .withChars('/', '-', '_', '.', ':')
                .ofMinLength(1)
                .ofMaxLength(255))
        .ofMinSize(1)
        .ofMaxSize(20);
  }
}
