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
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.Databricks;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;
import org.jspecify.annotations.Nullable;

class DatabricksCodecPropertyTest {

  private final DatabricksCodec codec;

  {
    ObjectMapper objectMapper = CodecObjectMapperFactory.create();
    codec = new DatabricksCodec(objectMapper);
  }

  @Property
  void roundTrip_shouldPreserveAllFields(
      @ForAll("host") String host,
      @ForAll("nullablePort") Integer port,
      @ForAll("httpPath") String httpPath,
      @ForAll("oAuthClientId") String oAuthClientId,
      @ForAll("oAuthSecret") String oAuthSecret,
      @ForAll("nullableCatalog") String catalog) {
    // Given
    Databricks original = new Databricks(host, port, httpPath, oAuthClientId, oAuthSecret, catalog);

    // When
    JsonNode json = codec.serialize(original);
    Databricks roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped.getHost()).isEqualTo(host);
    assertThat(roundTripped.getPort()).isEqualTo(port);
    assertThat(roundTripped.getHttpPath()).isEqualTo(httpPath);
    assertThat(roundTripped.getOAuthClientId()).isEqualTo(oAuthClientId);
    assertThat(roundTripped.getOAuthSecret()).isEqualTo(oAuthSecret);
    assertThat(roundTripped.getCatalog()).isEqualTo(catalog);
  }

  @Provide
  Arbitrary<String> host() {
    return CommonArbitraries.host();
  }

  @Provide
  Arbitrary<@Nullable Integer> nullablePort() {
    return CommonArbitraries.port().injectNull(0.3);
  }

  @Provide
  Arbitrary<String> httpPath() {
    return Arbitraries.strings().ofMinLength(1).ofMaxLength(100).filter(s -> s.trim().length() > 0);
  }

  @Provide
  Arbitrary<String> oAuthClientId() {
    return CommonArbitraries.username();
  }

  @Provide
  Arbitrary<String> oAuthSecret() {
    return CommonArbitraries.password();
  }

  @Provide
  Arbitrary<@Nullable String> nullableCatalog() {
    return CommonArbitraries.catalogName().injectNull(0.3);
  }
}
