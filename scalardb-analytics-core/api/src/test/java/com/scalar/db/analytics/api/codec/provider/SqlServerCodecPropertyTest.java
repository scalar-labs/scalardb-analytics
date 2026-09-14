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
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.SqlServer;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.jspecify.annotations.Nullable;

class SqlServerCodecPropertyTest {

  private final SqlServerCodec codec;

  {
    ObjectMapper objectMapper = CodecObjectMapperFactory.create();
    codec = new SqlServerCodec(objectMapper);
  }

  @Property
  void roundTrip_shouldPreserveAllFields(
      @ForAll("host") String host,
      @ForAll("port") int port,
      @ForAll("username") String username,
      @ForAll("password") String password,
      @ForAll("nullableDatabase") String database,
      @ForAll("nullableSecure") Boolean secure) {
    // Given
    SqlServer original = new SqlServer(host, port, username, password, database, secure);

    // When
    JsonNode json = codec.serialize(original);
    SqlServer roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped.getHost()).isEqualTo(host);
    assertThat(roundTripped.getPort()).isEqualTo(port);
    assertThat(roundTripped.getUsername()).isEqualTo(username);
    assertThat(roundTripped.getPassword()).isEqualTo(password);
    assertThat(roundTripped.getDatabase()).isEqualTo(database);
    assertThat(roundTripped.getSecure()).isEqualTo(secure);
  }

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
  Arbitrary<@Nullable Boolean> nullableSecure() {
    return Arbitraries.of(true, false).injectNull(0.3);
  }
}
