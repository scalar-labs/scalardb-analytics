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
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.jspecify.annotations.Nullable;

class MySqlCodecPropertyTest {

  private final MySqlCodec codec;

  {
    ObjectMapper objectMapper = CodecObjectMapperFactory.create();
    codec = new MySqlCodec(objectMapper);
  }

  @Property
  void roundTrip_shouldPreserveAllFields(
      @ForAll("host") String host,
      @ForAll("port") int port,
      @ForAll("username") String username,
      @ForAll("password") String password,
      @ForAll("nullableDatabase") String database,
      @ForAll("nullableSslMode") String sslMode) {
    // Given
    MySql original = new MySql(host, port, username, password, database, sslMode);

    // When - serialize and deserialize
    JsonNode json = codec.serialize(original);
    MySql roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped.getHost()).isEqualTo(host);
    assertThat(roundTripped.getPort()).isEqualTo(port);
    assertThat(roundTripped.getUsername()).isEqualTo(username);
    assertThat(roundTripped.getPassword()).isEqualTo(password);
    assertThat(roundTripped.getDatabase()).isEqualTo(database);
    assertThat(roundTripped.getSslMode()).isEqualTo(sslMode);
  }

  // Providers for jqwik
  @Provide
  Arbitrary<String> host() {
    return CommonArbitraries.host();
  }

  @Provide
  Arbitrary<Integer> port() {
    return CommonArbitraries.port();
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

  @Provide
  Arbitrary<@Nullable String> nullableSslMode() {
    return Arbitraries.of("disable", "trust", "verify-ca", "verify-full").injectNull(0.3);
  }
}
