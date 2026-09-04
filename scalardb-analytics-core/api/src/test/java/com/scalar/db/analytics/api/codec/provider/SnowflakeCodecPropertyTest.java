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
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Snowflake;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.jspecify.annotations.Nullable;

class SnowflakeCodecPropertyTest {

  private final SnowflakeCodec codec;

  {
    ObjectMapper objectMapper = CodecObjectMapperFactory.create();
    codec = new SnowflakeCodec(objectMapper);
  }

  @Property
  void roundTrip_shouldPreserveAllFields(
      @ForAll("account") String account,
      @ForAll("username") String username,
      @ForAll("password") String password,
      @ForAll("nullableDatabase") String database) {
    // Given
    Snowflake original = new Snowflake(account, username, password, database);

    // When
    JsonNode json = codec.serialize(original);
    Snowflake roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped.getAccount()).isEqualTo(account);
    assertThat(roundTripped.getUsername()).isEqualTo(username);
    assertThat(roundTripped.getPassword()).isEqualTo(password);
    assertThat(roundTripped.getDatabase()).isEqualTo(database);
  }

  @Provide
  Arbitrary<String> account() {
    return CommonArbitraries.systemIdentifier(50);
  }

  @Provide
  Arbitrary<String> username() {
    return CommonArbitraries.username();
  }

  @Provide
  Arbitrary<String> password() {
    return CommonArbitraries.password();
  }

  @Provide
  Arbitrary<@Nullable String> nullableDatabase() {
    return CommonArbitraries.databaseName().injectNull(0.3);
  }
}
