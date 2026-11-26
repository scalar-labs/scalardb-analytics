/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

@ExtendWith(OutputCaptureExtension.class)
class ScalarDbSqlPropertiesTest {

  private static final String ISOLATION_LEVEL = "scalar.db.consensus_commit.isolation_level";
  private static final String DEPRECATED_ISOLATION_LEVEL = "scalar.db.isolation_level";

  private static ScalarDbSqlProperties properties(Map<String, String> parameters) {
    ScalarDbSqlProperties properties =
        new ScalarDbSqlProperties("jdbc:postgresql://localhost/analytics", "user", "secret");
    properties.getParameters().putAll(parameters);
    return properties;
  }

  @Test
  void toMapWithDefaults_withoutIsolationParameter_usesSerializableWithoutWarning(
      CapturedOutput output) {
    Map<String, String> params = properties(Map.of()).toMapWithDefaults();

    assertThat(params).containsEntry(ISOLATION_LEVEL, "SERIALIZABLE");
    assertThat(output).doesNotContain("isolation");
  }

  @ParameterizedTest
  @ValueSource(strings = {"SNAPSHOT", "READ_COMMITTED", "snapshot", "invalid"})
  void toMapWithDefaults_withOtherIsolationLevel_overridesItAndWarns(
      String configured, CapturedOutput output) {
    Map<String, String> params =
        properties(Map.of(ISOLATION_LEVEL, configured)).toMapWithDefaults();

    assertThat(params).containsEntry(ISOLATION_LEVEL, "SERIALIZABLE");
    assertThat(output)
        .contains("WARN")
        .contains(ISOLATION_LEVEL + "=" + configured)
        .contains("SERIALIZABLE");
  }

  @ParameterizedTest
  @ValueSource(strings = {"SERIALIZABLE", "serializable", " SERIALIZABLE "})
  void toMapWithDefaults_withSerializable_keepsItWithoutWarning(
      String configured, CapturedOutput output) {
    Map<String, String> params =
        properties(Map.of(ISOLATION_LEVEL, configured)).toMapWithDefaults();

    assertThat(params).containsEntry(ISOLATION_LEVEL, "SERIALIZABLE");
    assertThat(output).doesNotContain("WARN");
  }

  @Test
  void toMapWithDefaults_withDeprecatedIsolationKey_removesItAndWarns(CapturedOutput output) {
    Map<String, String> params =
        properties(Map.of(DEPRECATED_ISOLATION_LEVEL, "SNAPSHOT")).toMapWithDefaults();

    assertThat(params)
        .doesNotContainKey(DEPRECATED_ISOLATION_LEVEL)
        .containsEntry(ISOLATION_LEVEL, "SERIALIZABLE");
    assertThat(output).contains("WARN").contains(DEPRECATED_ISOLATION_LEVEL + "=SNAPSHOT");
  }

  @Test
  void toMapWithDefaults_calledRepeatedly_warnsOnce(CapturedOutput output) {
    ScalarDbSqlProperties properties = properties(Map.of(ISOLATION_LEVEL, "SNAPSHOT"));

    properties.toMapWithDefaults();
    properties.toMapWithDefaults();
    properties.toProperties();

    assertThat(output.getAll().split("Ignoring ", -1)).hasSize(2);
  }

  @Test
  void toMapWithDefaults_keepsOtherParameters() {
    Map<String, String> params =
        properties(
                Map.of(
                    ISOLATION_LEVEL,
                    "SNAPSHOT",
                    "scalar.db.consensus_commit.parallel_commit.enabled",
                    "false"))
            .toMapWithDefaults();

    assertThat(params)
        .containsEntry("scalar.db.consensus_commit.parallel_commit.enabled", "false")
        .containsEntry("scalar.db.storage", "jdbc")
        .containsEntry("scalar.db.password", "secret");
  }

  @Test
  void toProperties_usesSerializable() {
    assertThat(
            properties(Map.of(ISOLATION_LEVEL, "READ_COMMITTED"))
                .toProperties()
                .getProperty(ISOLATION_LEVEL))
        .isEqualTo("SERIALIZABLE");
  }
}
