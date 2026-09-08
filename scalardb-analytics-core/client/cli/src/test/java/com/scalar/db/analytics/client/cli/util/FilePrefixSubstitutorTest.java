/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.client.exception.ClientException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FilePrefixSubstitutorTest {

  @TempDir Path tempDir;
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  @Test
  void substitute_withPropertiesFile_shouldReplaceWithJsonObject() throws IOException {
    // Arrange
    Path propsFile = tempDir.resolve("config.properties");
    Files.write(
        propsFile,
        "scalar.db.storage=cassandra\nscalar.db.contact_points=localhost\n"
            .getBytes(StandardCharsets.UTF_8));

    String json =
        "{"
            + "\"type\": \"scalardb\","
            + "\"configs\": \"${file:"
            + propsFile.toAbsolutePath()
            + "}\""
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act
    JsonNode result = FilePrefixSubstitutor.substitute(inputNode);

    // Assert
    JsonNode configs = result.get("configs");
    assertThat(configs.isObject()).isTrue();
    assertThat(configs.get("scalar.db.storage").asText()).isEqualTo("cassandra");
    assertThat(configs.get("scalar.db.contact_points").asText()).isEqualTo("localhost");
  }

  @Test
  void substitute_withJsonFile_shouldReplaceWithJsonObject() throws IOException {
    // Arrange
    Path jsonFile = tempDir.resolve("config.json");
    Files.write(
        jsonFile,
        ("{"
                + "\"scalar.db.storage\": \"cassandra\","
                + "\"scalar.db.contact_points\": \"localhost\""
                + "}")
            .getBytes(StandardCharsets.UTF_8));

    String json =
        "{"
            + "\"type\": \"scalardb\","
            + "\"configs\": \"${file:"
            + jsonFile.toAbsolutePath()
            + "}\""
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act
    JsonNode result = FilePrefixSubstitutor.substitute(inputNode);

    // Assert
    JsonNode configs = result.get("configs");
    assertThat(configs.isObject()).isTrue();
    assertThat(configs.get("scalar.db.storage").asText()).isEqualTo("cassandra");
    assertThat(configs.get("scalar.db.contact_points").asText()).isEqualTo("localhost");
  }

  @Test
  void substitute_withJsonFileContainingNonStringValue_shouldAccept() throws IOException {
    // Arrange
    Path jsonFile = tempDir.resolve("config.json");
    Files.write(
        jsonFile,
        ("{" + "\"scalar.db.storage\": \"cassandra\"," + "\"scalar.db.port\": 9042" + "}")
            .getBytes(StandardCharsets.UTF_8));

    String json =
        "{"
            + "\"type\": \"scalardb\","
            + "\"configs\": \"${file:"
            + jsonFile.toAbsolutePath()
            + "}\""
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act
    JsonNode result = FilePrefixSubstitutor.substitute(inputNode);

    // Assert - should accept any JSON structure, deserialization will validate
    JsonNode configs = result.get("configs");
    assertThat(configs.isObject()).isTrue();
    assertThat(configs.get("scalar.db.storage").asText()).isEqualTo("cassandra");
    assertThat(configs.get("scalar.db.port").asInt()).isEqualTo(9042);
  }

  @Test
  void substitute_withJsonFileContainingArray_shouldAccept() throws IOException {
    // Arrange
    Path jsonFile = tempDir.resolve("options.json");
    Files.write(jsonFile, "[\"value1\", \"value2\"]".getBytes(StandardCharsets.UTF_8));

    String json =
        "{"
            + "\"type\": \"postgresql\","
            + "\"host\": \"localhost\","
            + "\"port\": 5432,"
            + "\"username\": \"postgres\","
            + "\"password\": \"password\","
            + "\"database\": \"testdb\","
            + "\"options\": \"${file:"
            + jsonFile.toAbsolutePath()
            + "}\""
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act
    JsonNode result = FilePrefixSubstitutor.substitute(inputNode);

    // Assert - should accept array
    JsonNode options = result.get("options");
    assertThat(options.isArray()).isTrue();
    assertThat(options.size()).isEqualTo(2);
    assertThat(options.get(0).asText()).isEqualTo("value1");
  }

  @Test
  void substitute_withJsonFileContainingNestedObject_shouldAccept() throws IOException {
    // Arrange
    Path jsonFile = tempDir.resolve("config.json");
    Files.write(
        jsonFile,
        ("{" + "\"scalar.db.storage\": \"cassandra\"," + "\"nested\": {\"key\": \"value\"}" + "}")
            .getBytes(StandardCharsets.UTF_8));

    String json =
        "{"
            + "\"type\": \"scalardb\","
            + "\"configs\": \"${file:"
            + jsonFile.toAbsolutePath()
            + "}\""
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act
    JsonNode result = FilePrefixSubstitutor.substitute(inputNode);

    // Assert - should accept nested object
    JsonNode configs = result.get("configs");
    assertThat(configs.get("nested").isObject()).isTrue();
    assertThat(configs.get("nested").get("key").asText()).isEqualTo("value");
  }

  @Test
  void substitute_withNonExistentFile_shouldThrowException() throws IOException {
    // Arrange
    Path nonExistentFile = tempDir.resolve("nonexistent.properties");
    String json =
        "{"
            + "\"type\": \"scalardb\","
            + "\"configs\": \"${file:"
            + nonExistentFile.toAbsolutePath()
            + "}\""
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act & Assert
    assertThatThrownBy(() -> FilePrefixSubstitutor.substitute(inputNode))
        .isInstanceOf(ClientException.class)
        .hasMessageContaining("File not found");
  }

  @Test
  void substitute_withDirectory_shouldThrowException() throws IOException {
    // Arrange
    Path dir = tempDir.resolve("directory");
    Files.createDirectory(dir);

    String json =
        "{"
            + "\"type\": \"scalardb\","
            + "\"configs\": \"${file:"
            + dir.toAbsolutePath()
            + "}\""
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act & Assert
    assertThatThrownBy(() -> FilePrefixSubstitutor.substitute(inputNode))
        .isInstanceOf(ClientException.class)
        .hasMessageContaining("Not a regular file");
  }

  @Test
  void substitute_withUnsupportedFileType_shouldThrowException() throws IOException {
    // Arrange
    Path txtFile = tempDir.resolve("config.txt");
    Files.write(txtFile, "some content".getBytes(StandardCharsets.UTF_8));

    String json =
        "{"
            + "\"type\": \"scalardb\","
            + "\"configs\": \"${file:"
            + txtFile.toAbsolutePath()
            + "}\""
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act & Assert
    assertThatThrownBy(() -> FilePrefixSubstitutor.substitute(inputNode))
        .isInstanceOf(ClientException.class)
        .hasMessageContaining("Unsupported file type")
        .hasMessageContaining(".properties and .json are supported");
  }

  @Test
  void substitute_withMultipleFileReferences_shouldReplaceAll() throws IOException {
    // Arrange
    Path propsFile1 = tempDir.resolve("config1.properties");
    Files.write(propsFile1, "scalar.db.storage=cassandra\n".getBytes(StandardCharsets.UTF_8));

    Path propsFile2 = tempDir.resolve("config2.properties");
    Files.write(propsFile2, "scalar.db.storage=jdbc\n".getBytes(StandardCharsets.UTF_8));

    String json =
        "{"
            + "\"type\": \"scalardb\","
            + "\"configs\": \"${file:"
            + propsFile1.toAbsolutePath()
            + "}\","
            + "\"alternativeConfigs\": \"${file:"
            + propsFile2.toAbsolutePath()
            + "}\""
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act
    JsonNode result = FilePrefixSubstitutor.substitute(inputNode);

    // Assert
    assertThat(result.get("configs").get("scalar.db.storage").asText()).isEqualTo("cassandra");
    assertThat(result.get("alternativeConfigs").get("scalar.db.storage").asText())
        .isEqualTo("jdbc");
  }

  @Test
  void substitute_withNoFileReferences_shouldReturnOriginal() throws IOException {
    // Arrange
    String json =
        "{"
            + "\"type\": \"postgresql\","
            + "\"host\": \"localhost\","
            + "\"port\": 5432,"
            + "\"username\": \"postgres\","
            + "\"password\": \"password\","
            + "\"database\": \"testdb\""
            + "}";
    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act
    JsonNode result = FilePrefixSubstitutor.substitute(inputNode);

    // Assert
    assertThat(result.get("type").asText()).isEqualTo("postgresql");
    assertThat(result.get("host").asText()).isEqualTo("localhost");
    assertThat(result.get("port").asInt()).isEqualTo(5432);
  }

  @Test
  void substitute_withPropertiesFileContainingSpecialCharacters_shouldHandleCorrectly()
      throws IOException {
    // Arrange
    Path propsFile = tempDir.resolve("config.properties");
    Files.write(
        propsFile,
        "key.with.dots=value:with:colons\nkey_with_underscores=value\n"
            .getBytes(StandardCharsets.UTF_8));

    String json = "{" + "\"configs\": \"${file:" + propsFile.toAbsolutePath() + "}\"" + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act
    JsonNode result = FilePrefixSubstitutor.substitute(inputNode);

    // Assert
    JsonNode configs = result.get("configs");
    assertThat(configs.get("key.with.dots").asText()).isEqualTo("value:with:colons");
    assertThat(configs.get("key_with_underscores").asText()).isEqualTo("value");
  }

  @Test
  void substitute_withEmptyPropertiesFile_shouldReplaceWithEmptyObject() throws IOException {
    // Arrange
    Path propsFile = tempDir.resolve("empty.properties");
    Files.write(propsFile, "".getBytes(StandardCharsets.UTF_8));

    String json =
        "{"
            + "\"type\": \"scalardb\","
            + "\"configs\": \"${file:"
            + propsFile.toAbsolutePath()
            + "}\""
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act
    JsonNode result = FilePrefixSubstitutor.substitute(inputNode);

    // Assert
    JsonNode configs = result.get("configs");
    assertThat(configs.isObject()).isTrue();
    assertThat(configs.size()).isEqualTo(0);
  }

  @Test
  void substitute_withEmptyJsonFile_shouldReplaceWithEmptyObject() throws IOException {
    // Arrange
    Path jsonFile = tempDir.resolve("empty.json");
    Files.write(jsonFile, "{}".getBytes(StandardCharsets.UTF_8));

    String json =
        "{"
            + "\"type\": \"scalardb\","
            + "\"configs\": \"${file:"
            + jsonFile.toAbsolutePath()
            + "}\""
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act
    JsonNode result = FilePrefixSubstitutor.substitute(inputNode);

    // Assert
    JsonNode configs = result.get("configs");
    assertThat(configs.isObject()).isTrue();
    assertThat(configs.size()).isEqualTo(0);
  }

  @Test
  void substitute_inArrayElement_shouldReplace() throws IOException {
    // Arrange
    Path jsonFile = tempDir.resolve("config.json");
    Files.write(
        jsonFile, "{\"scalar.db.storage\": \"cassandra\"}".getBytes(StandardCharsets.UTF_8));

    String json =
        "{"
            + "\"type\": \"scalardb\","
            + "\"configsList\": ["
            + "\"${file:"
            + jsonFile.toAbsolutePath()
            + "}\","
            + "{\"scalar.db.storage\": \"jdbc\"}"
            + "]"
            + "}";

    JsonNode inputNode = OBJECT_MAPPER.readTree(json);

    // Act
    JsonNode result = FilePrefixSubstitutor.substitute(inputNode);

    // Assert
    JsonNode configsList = result.get("configsList");
    assertThat(configsList.isArray()).isTrue();
    assertThat(configsList.get(0).get("scalar.db.storage").asText()).isEqualTo("cassandra");
    assertThat(configsList.get(1).get("scalar.db.storage").asText()).isEqualTo("jdbc");
  }
}
