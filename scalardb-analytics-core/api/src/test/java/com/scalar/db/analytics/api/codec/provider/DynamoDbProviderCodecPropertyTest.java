/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.codec.CodecObjectMapperFactory;
import com.scalar.db.analytics.api.codec.CommonArbitraries;
import com.scalar.db.analytics.api.model.datasource.provider.DynamoDbProvider;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class DynamoDbProviderCodecPropertyTest {

  private final DynamoDbProviderCodec codec;

  {
    ObjectMapper objectMapper = CodecObjectMapperFactory.create();
    codec = new DynamoDbProviderCodec(objectMapper);
  }

  @Property
  void roundTrip_withRegion_shouldPreserveAllFields(@ForAll("region") String region) {
    // Given
    DynamoDbProvider original = DynamoDbProvider.builder().region(region).build();

    // When
    JsonNode json = codec.serialize(original);
    DynamoDbProvider roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped.getRegion()).isEqualTo(region);
    assertThat(roundTripped.getEndpoint()).isNull();
  }

  @Property
  void roundTrip_withEndpoint_shouldPreserveAllFields(@ForAll("endpoint") String endpoint) {
    // Given
    DynamoDbProvider original = DynamoDbProvider.builder().endpoint(endpoint).build();

    // When
    JsonNode json = codec.serialize(original);
    DynamoDbProvider roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped.getRegion()).isNull();
    assertThat(roundTripped.getEndpoint()).isEqualTo(endpoint);
  }

  @Provide
  Arbitrary<String> region() {
    return Arbitraries.of(
        "us-east-1", "us-east-2", "us-west-1", "us-west-2", "eu-west-1", "ap-northeast-1");
  }

  @Provide
  Arbitrary<String> endpoint() {
    return CommonArbitraries.url();
  }
}
