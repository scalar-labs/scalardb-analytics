/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;
import org.aeonbits.owner.Config;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConfigMappingUtilsTest {

  @BeforeEach
  void clearCache() {
    ConfigMappingUtils.clearCache();
  }

  @Test
  void testConvertToEnvVarName() {
    assertThat(ConfigMappingUtils.convertToEnvVarName("scalar.db.analytics.server.port"))
        .isEqualTo("SCALAR_DB_ANALYTICS_SERVER_PORT");

    assertThat(ConfigMappingUtils.convertToEnvVarName("server.tls.cert_chain_path"))
        .isEqualTo("SERVER_TLS_CERT_CHAIN_PATH");

    assertThat(ConfigMappingUtils.convertToEnvVarName("tls.ca_root_cert_path"))
        .isEqualTo("TLS_CA_ROOT_CERT_PATH");
  }

  // Passing null violates the @NullMarked contract, but the runtime guard exists for callers that
  // do not enforce it, so the test deliberately passes null.
  @SuppressWarnings("NullAway")
  @Test
  void testConvertToEnvVarNameWithInvalidInput() {
    assertThatThrownBy(() -> ConfigMappingUtils.convertToEnvVarName(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Property key cannot be null or empty");

    assertThatThrownBy(() -> ConfigMappingUtils.convertToEnvVarName(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Property key cannot be null or empty");
  }

  @Test
  void testDiscoverMappings() {
    Map<String, String> mappings = ConfigMappingUtils.discoverMappings(TestConfig.class);

    assertThat(mappings)
        .hasSize(3)
        .containsEntry("test.property", "TEST_PROPERTY")
        .containsEntry("custom.env.var", "MY_CUSTOM_VAR")
        .containsEntry("another.property", "ANOTHER_PROPERTY");

    // Should not include method without @EnvVar
    assertThat(mappings).doesNotContainKey("no.env.mapping");
  }

  @Test
  void testGetEnvironmentMappingsWithCaching() {
    Map<String, String> mappings1 = ConfigMappingUtils.getEnvironmentMappings(TestConfig.class);
    Map<String, String> mappings2 = ConfigMappingUtils.getEnvironmentMappings(TestConfig.class);

    // Should return the same instance due to caching
    assertThat(mappings1).isSameAs(mappings2);
  }

  @Test
  void testLoadPropertiesFromEnvironment() {
    // Note: This test will use actual environment variables if they are set
    // In a real test environment, you would use system-stubs or similar
    // to mock environment variables, but that requires Java 11+

    Map<String, String> mappings = ConfigMappingUtils.getEnvironmentMappings(TestConfig.class);
    assertThat(mappings)
        .containsEntry("test.property", "TEST_PROPERTY")
        .containsEntry("custom.env.var", "MY_CUSTOM_VAR");
  }

  @Test
  void testDescribeMappings() {
    String description = ConfigMappingUtils.describeMappings(TestConfig.class);

    assertThat(description)
        .contains("Configuration mappings for TestConfig:")
        .contains("test.property -> TEST_PROPERTY")
        .contains("custom.env.var -> MY_CUSTOM_VAR")
        .contains("another.property -> ANOTHER_PROPERTY");
  }

  @Test
  void testMethodWithoutConfigKey() {
    Map<String, String> mappings = ConfigMappingUtils.discoverMappings(InvalidConfig.class);

    // Should skip method with @EnvVar but no @Config.Key
    assertThat(mappings).isEmpty();
  }

  // Test configuration interfaces
  interface TestConfig extends Config {
    @Config.Key("test.property")
    @EnvVar
    String testProperty();

    @Config.Key("custom.env.var")
    @EnvVar("MY_CUSTOM_VAR")
    String customEnvVar();

    @Config.Key("another.property")
    @EnvVar
    String anotherProperty();

    @Config.Key("no.env.mapping")
    // No @EnvVar annotation
    String noEnvMapping();
  }

  interface InvalidConfig extends Config {
    // Has @EnvVar but no @Config.Key
    @EnvVar
    String invalidMethod();
  }
}
