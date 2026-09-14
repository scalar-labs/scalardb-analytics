/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class UuidMapperPropertyTest {

  private final UuidMapper mapper = UuidMapper.INSTANCE;

  @Property
  void uuid_shouldRoundTripCorrectly(@ForAll("uuid") UUID originalUuid) {
    // UUID -> String -> UUID
    String uuidString = mapper.toString(originalUuid);
    UUID roundTripped = mapper.toUuid(uuidString);

    assertThat(roundTripped).isEqualTo(originalUuid);
  }

  @Property
  void validUuidString_shouldConvertToUuid(@ForAll("validUuidString") String uuidString) {
    UUID uuid = mapper.toUuid(uuidString);

    assertThat(uuid).isNotNull();
    assertThat(uuid.toString()).isEqualTo(uuidString.toLowerCase());
  }

  @Property
  void null_shouldMapToNull() {
    assertThat(mapper.toString(null)).isNull();
    assertThat(mapper.toUuid(null)).isNull();
  }

  @Property
  void emptyString_shouldThrowException() {
    assertThatThrownBy(() -> mapper.toUuid("")).isInstanceOf(IllegalArgumentException.class);
  }

  @Property
  void invalidUuidString_shouldThrowException(@ForAll("invalidUuidString") String invalidUuid) {
    assertThatThrownBy(() -> mapper.toUuid(invalidUuid))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Provide
  Arbitrary<UUID> uuid() {
    return CommonArbitraries.uuid();
  }

  @Provide
  Arbitrary<String> validUuidString() {
    return CommonArbitraries.uuidString();
  }

  @Provide
  Arbitrary<String> invalidUuidString() {
    return CommonArbitraries.invalidUuidString();
  }
}
