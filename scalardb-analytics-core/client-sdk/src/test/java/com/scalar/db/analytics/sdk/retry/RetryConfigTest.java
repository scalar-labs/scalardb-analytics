/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.retry;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class RetryConfigTest {

  @Test
  void defaultConfig_ShouldReturnEnabledConfigWithDefaults() {
    // Act
    RetryConfig config = RetryConfig.defaultConfig();

    // Assert
    assertThat(config.isEnabled()).isTrue();
    assertThat(config.getMaxAttempts()).isEqualTo(5);
    assertThat(config.getInitialBackoffMillis()).isEqualTo(100);
    assertThat(config.getMaxBackoffMillis()).isEqualTo(5000);
    assertThat(config.getBackoffMultiplier()).isEqualTo(2.0);
  }

  @Test
  void disabled_ShouldReturnDisabledConfig() {
    // Act
    RetryConfig config = RetryConfig.disabled();

    // Assert
    assertThat(config.isEnabled()).isFalse();
  }

  @Test
  void builder_ShouldBuildCustomConfig() {
    // Act
    RetryConfig config =
        RetryConfig.builder()
            .maxAttempts(3)
            .initialBackoffMillis(200)
            .maxBackoffMillis(10000)
            .backoffMultiplier(3.0)
            .enabled(true)
            .build();

    // Assert
    assertThat(config.getMaxAttempts()).isEqualTo(3);
    assertThat(config.getInitialBackoffMillis()).isEqualTo(200);
    assertThat(config.getMaxBackoffMillis()).isEqualTo(10000);
    assertThat(config.getBackoffMultiplier()).isEqualTo(3.0);
    assertThat(config.isEnabled()).isTrue();
  }

  @Test
  void builder_ShouldThrowException_WhenMaxAttemptsIsZero() {
    // Act & Assert
    assertThatThrownBy(() -> RetryConfig.builder().maxAttempts(0).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxAttempts must be at least 1");
  }

  @Test
  void builder_ShouldThrowException_WhenInitialBackoffIsNegative() {
    // Act & Assert
    assertThatThrownBy(() -> RetryConfig.builder().initialBackoffMillis(-1).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("initialBackoffMillis must be non-negative");
  }

  @Test
  void builder_ShouldThrowException_WhenMaxBackoffIsNegative() {
    // Act & Assert
    assertThatThrownBy(() -> RetryConfig.builder().maxBackoffMillis(-1).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("maxBackoffMillis must be non-negative");
  }

  @Test
  void builder_ShouldThrowException_WhenMultiplierIsLessThanOne() {
    // Act & Assert
    assertThatThrownBy(() -> RetryConfig.builder().backoffMultiplier(0.5).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("backoffMultiplier must be at least 1.0");
  }

  @Test
  void builder_ShouldThrowException_WhenMaxBackoffIsLessThanInitialBackoff() {
    // Act & Assert
    assertThatThrownBy(
            () -> RetryConfig.builder().initialBackoffMillis(5000).maxBackoffMillis(1000).build())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining(
            "maxBackoffMillis must be greater than or equal to initialBackoffMillis");
  }

  @Test
  void builder_ShouldSkipValidation_WhenDisabled() {
    // Act - invalid values should not throw when disabled
    RetryConfig config =
        RetryConfig.builder()
            .enabled(false)
            .maxAttempts(-1)
            .initialBackoffMillis(-1)
            .maxBackoffMillis(-1)
            .backoffMultiplier(0.1)
            .build();

    // Assert
    assertThat(config.isEnabled()).isFalse();
  }

  @Test
  void equals_ShouldReturnTrue_ForEqualConfigs() {
    // Arrange
    RetryConfig config1 = RetryConfig.defaultConfig();
    RetryConfig config2 = RetryConfig.defaultConfig();

    // Assert
    assertThat(config1).isEqualTo(config2);
    assertThat(config1.hashCode()).isEqualTo(config2.hashCode());
  }

  @Test
  void equals_ShouldReturnFalse_ForDifferentConfigs() {
    // Arrange
    RetryConfig config1 = RetryConfig.defaultConfig();
    RetryConfig config2 = RetryConfig.builder().maxAttempts(3).build();

    // Assert
    assertThat(config1).isNotEqualTo(config2);
  }

  @Test
  void toString_ShouldContainAllFields() {
    // Act
    String result = RetryConfig.defaultConfig().toString();

    // Assert
    assertThat(result).contains("enabled=true");
    assertThat(result).contains("maxAttempts=5");
    assertThat(result).contains("initialBackoffMillis=100");
    assertThat(result).contains("maxBackoffMillis=5000");
    assertThat(result).contains("backoffMultiplier=2.0");
  }

  @SuppressWarnings("unchecked")
  @Test
  void toServiceConfig_ShouldReturnValidGrpcServiceConfig_WithDefaults() {
    // Arrange
    RetryConfig config = RetryConfig.defaultConfig();

    // Act
    Map<String, Object> serviceConfig = config.toServiceConfig();

    // Assert
    assertThat(serviceConfig).containsKey("methodConfig");
    List<Map<String, Object>> methodConfigs =
        Objects.requireNonNull((List<Map<String, Object>>) serviceConfig.get("methodConfig"));
    assertThat(methodConfigs).hasSize(1);

    Map<String, Object> methodConfig = methodConfigs.get(0);

    // Verify name matches all services
    List<Map<String, Object>> names =
        Objects.requireNonNull((List<Map<String, Object>>) methodConfig.get("name"));
    assertThat(names).hasSize(1);
    assertThat(names.get(0).get("service")).isEqualTo("");

    // Verify retry policy
    Map<String, Object> retryPolicy =
        Objects.requireNonNull((Map<String, Object>) methodConfig.get("retryPolicy"));
    assertThat(retryPolicy.get("maxAttempts")).isEqualTo(5.0);
    assertThat(retryPolicy.get("initialBackoff")).isEqualTo("0.100s");
    assertThat(retryPolicy.get("maxBackoff")).isEqualTo("5s");
    assertThat(retryPolicy.get("backoffMultiplier")).isEqualTo(2.0);
    assertThat((List<String>) retryPolicy.get("retryableStatusCodes"))
        .containsExactlyInAnyOrder("UNAVAILABLE", "DEADLINE_EXCEEDED", "RESOURCE_EXHAUSTED");
  }

  @SuppressWarnings("unchecked")
  @Test
  void toServiceConfig_ShouldReturnCustomValues() {
    // Arrange
    RetryConfig config =
        RetryConfig.builder()
            .maxAttempts(3)
            .initialBackoffMillis(2000)
            .maxBackoffMillis(30000)
            .backoffMultiplier(1.5)
            .build();

    // Act
    Map<String, Object> serviceConfig = config.toServiceConfig();

    // Assert
    List<Map<String, Object>> methodConfigs =
        Objects.requireNonNull((List<Map<String, Object>>) serviceConfig.get("methodConfig"));
    Map<String, Object> retryPolicy =
        Objects.requireNonNull((Map<String, Object>) methodConfigs.get(0).get("retryPolicy"));
    assertThat(retryPolicy.get("maxAttempts")).isEqualTo(3.0);
    assertThat(retryPolicy.get("initialBackoff")).isEqualTo("2s");
    assertThat(retryPolicy.get("maxBackoff")).isEqualTo("30s");
    assertThat(retryPolicy.get("backoffMultiplier")).isEqualTo(1.5);
  }
}
