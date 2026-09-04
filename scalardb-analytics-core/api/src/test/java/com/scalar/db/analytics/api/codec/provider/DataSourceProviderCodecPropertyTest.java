/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.api.codec.CodecObjectMapperFactory;
import com.scalar.db.analytics.api.codec.CommonArbitraries;
import com.scalar.db.analytics.api.model.datasource.DataSourceProvider;
import com.scalar.db.analytics.api.model.datasource.provider.ScalarDbProvider;
import com.scalar.db.analytics.api.model.datasource.provider.rdbms.MySql;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.Example;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import net.jqwik.api.Provide;

class DataSourceProviderCodecPropertyTest {

  private final DataSourceProviderCodec codec;
  private final ObjectMapper objectMapper;

  {
    objectMapper = CodecObjectMapperFactory.create();

    ProviderCodecRegistry registry = ProviderCodecRegistry.create(objectMapper);
    codec = new DataSourceProviderCodec(registry, objectMapper);
  }

  @Property
  void mySql_shouldRoundTripCorrectly(
      @ForAll("host") String host,
      @ForAll("port") int port,
      @ForAll("username") String username,
      @ForAll("password") String password,
      @ForAll("databaseName") String database,
      @ForAll("sslMode") String sslMode) {
    // Given
    MySql original = new MySql(host, port, username, password, database, sslMode);

    // When - serialize and deserialize
    String json = codec.serialize(original);
    DataSourceProvider roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped).isInstanceOf(MySql.class);
    MySql result = (MySql) roundTripped;
    assertThat(result.getHost()).isEqualTo(host);
    assertThat(result.getPort()).isEqualTo(port);
    assertThat(result.getUsername()).isEqualTo(username);
    assertThat(result.getPassword()).isEqualTo(password);
    assertThat(result.getDatabase()).isEqualTo(database);
    assertThat(result.getSslMode()).isEqualTo(sslMode);
    try {
      JsonNode node = objectMapper.readTree(json);
      assertThat(node.get("type").asText()).isEqualTo(MySql.TYPE);
      assertThat(node.get("host").asText()).isEqualTo(host);
      assertThat(node.get("port").asInt()).isEqualTo(port);
      assertThat(node.get("username").asText()).isEqualTo(username);
      assertThat(node.get("database").asText()).isEqualTo(database);
      assertThat(node.has("payload")).isFalse();
    } catch (Exception e) {
      throw new AssertionError("Failed to parse serialized JSON", e);
    }
  }

  @Property
  void scalarDb_shouldRoundTripCorrectly(@ForAll("configs") Map<String, String> configs) {
    // Given
    ScalarDbProvider original = new ScalarDbProvider(configs);

    // When
    String json = codec.serialize(original);
    DataSourceProvider roundTripped = codec.deserialize(json);

    // Then
    assertThat(roundTripped).isInstanceOf(ScalarDbProvider.class);
    ScalarDbProvider result = (ScalarDbProvider) roundTripped;
    assertThat(result.getConfigs()).isEqualTo(configs);
    try {
      JsonNode node = objectMapper.readTree(json);
      assertThat(node.get("type").asText()).isEqualTo(ScalarDbProvider.TYPE);
      assertThat(node.has("configs")).isTrue();
      assertThat(node.has("payload")).isFalse();
    } catch (Exception e) {
      throw new AssertionError("Failed to parse serialized JSON", e);
    }
  }

  @Example
  void deserialize_withInvalidJson_shouldThrowException() {
    String invalidJson = "not a valid json";
    assertThatThrownBy(() -> codec.deserialize(invalidJson))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("malformed");
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
  Arbitrary<String> databaseName() {
    return CommonArbitraries.databaseName();
  }

  @Provide
  Arbitrary<String> sslMode() {
    return Arbitraries.of("disable", "trust", "verify-ca", "verify-full");
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
