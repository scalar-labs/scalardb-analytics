/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.converters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.repository.impl.spring.converter.StringList;
import com.scalar.db.analytics.repository.impl.spring.converter.reading.JsonToStringListConverter;
import com.scalar.db.analytics.repository.impl.spring.converter.writing.StringListToJsonConverter;
import com.scalar.db.analytics.repository.impl.spring.converter.writing.StringListToPostgreSqlArrayConverter;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ScalarDbStringListConverterTest {

  private final ObjectMapper objectMapper = new ObjectMapper();

  private StringListToJsonConverter stringListToJson() {
    return new StringListToJsonConverter(objectMapper);
  }

  private JsonToStringListConverter jsonToStringList() {
    return new JsonToStringListConverter(objectMapper);
  }

  @Test
  @DisplayName("Should convert StringList with multiple values")
  void shouldConvertMultipleValues() {
    List<String> values = Arrays.asList("string1", "string2", "string3");
    StringList original = new StringList(values);

    String json = stringListToJson().convert(original);
    StringList restored = jsonToStringList().convert(json);
    String[] postgresArray = new StringListToPostgreSqlArrayConverter().convert(original);

    assertThat(restored.values()).containsExactlyElementsOf(values);
    assertThat(postgresArray).containsExactly("string1", "string2", "string3");
  }

  @Test
  @DisplayName("Should handle empty StringList")
  void shouldHandleEmptyList() {
    StringList original = new StringList(List.of());

    String json = stringListToJson().convert(original);
    StringList restored = jsonToStringList().convert(json);
    String[] postgresArray = new StringListToPostgreSqlArrayConverter().convert(original);

    assertThat(restored.values()).isEmpty();
    assertThat(postgresArray).isEmpty();
  }

  @Test
  @DisplayName("Should handle null StringList")
  void shouldHandleNullList() {
    StringList original = new StringList(null);

    String json = stringListToJson().convert(original);
    assertThat(json).isEqualTo("null");

    StringList restored = jsonToStringList().convert(json);
    assertThat(restored.values()).isNull();
  }

  @Test
  @DisplayName("Should handle special characters in StringList")
  void shouldHandleSpecialCharacters() {
    List<String> values =
        Arrays.asList(
            "string with spaces",
            "string,with,commas",
            "string\"with\"quotes",
            "string[with]brackets");
    StringList original = new StringList(values);

    String json = stringListToJson().convert(original);
    StringList restored = jsonToStringList().convert(json);

    assertThat(restored.values()).containsExactlyElementsOf(values);
  }

  @Test
  @DisplayName("Should reject invalid JSON payloads")
  void shouldRejectInvalidJson() {
    assertThatThrownBy(() -> jsonToStringList().convert("not-a-json"))
        .isInstanceOf(RuntimeException.class)
        .hasMessageContaining("Failed to convert JSON to StringList");
  }
}
