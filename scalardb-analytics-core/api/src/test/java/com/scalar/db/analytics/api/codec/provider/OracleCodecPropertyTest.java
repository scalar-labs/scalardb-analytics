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
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Oracle;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class OracleCodecPropertyTest {

  private final OracleCodec codec;

  {
    ObjectMapper objectMapper = CodecObjectMapperFactory.create();
    codec = new OracleCodec(objectMapper);
  }

  @Property
  void roundTrip_shouldPreserveAllFields(
      @ForAll("host") String host,
      @ForAll("port") int port,
      @ForAll("username") String username,
      @ForAll("password") String password,
      @ForAll("serviceName") String serviceName) {
    // Given
    Oracle original = new Oracle(host, port, username, password, serviceName);

    // When
    JsonNode json = codec.serialize(original);
    Oracle roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped.getHost()).isEqualTo(host);
    assertThat(roundTripped.getPort()).isEqualTo(port);
    assertThat(roundTripped.getUsername()).isEqualTo(username);
    assertThat(roundTripped.getPassword()).isEqualTo(password);
    assertThat(roundTripped.getServiceName()).isEqualTo(serviceName);
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
  Arbitrary<String> serviceName() {
    return CommonArbitraries.systemIdentifier(255);
  }
}
