/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import com.scalar.db.analytics.api.auth.AccessToken;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTokenCacheTest {

  @TempDir Path tempDir;
  private Path cacheDir;
  private FileTokenCache cache;

  @BeforeEach
  void setUp() {
    cacheDir = tempDir.resolve("tokens");
    cache = new FileTokenCache(cacheDir);
  }

  @Test
  void saveAndLoad_ShouldRoundTrip() {
    // Arrange
    Instant expiresAt = Instant.ofEpochSecond(1700000000L);
    AccessToken token = new AccessToken("my-token", expiresAt, "user-id-1");

    // Act
    cache.save("alice", token);
    AccessToken loaded = cache.load("alice");

    // Assert
    assertThat(loaded).isNotNull();
    assertThat(loaded.getToken()).isEqualTo("my-token");
    assertThat(loaded.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(loaded.getUserId()).isEqualTo("user-id-1");
  }

  @Test
  void load_ShouldReturnNull_WhenNoTokenSaved() {
    // Act & Assert
    assertThat(cache.load("unknown-user")).isNull();
  }

  @Test
  void save_ShouldOverwriteExistingToken() {
    // Arrange
    Instant expiresAt1 = Instant.ofEpochSecond(1700000000L);
    Instant expiresAt2 = Instant.ofEpochSecond(1800000000L);
    AccessToken token1 = new AccessToken("token-1", expiresAt1, "user-id-1");
    AccessToken token2 = new AccessToken("token-2", expiresAt2, "user-id-2");

    // Act
    cache.save("alice", token1);
    cache.save("alice", token2);
    AccessToken loaded = cache.load("alice");

    // Assert
    assertThat(loaded).isNotNull();
    assertThat(loaded.getToken()).isEqualTo("token-2");
    assertThat(loaded.getExpiresAt()).isEqualTo(expiresAt2);
    assertThat(loaded.getUserId()).isEqualTo("user-id-2");
  }

  @Test
  void save_ShouldIsolateDifferentUsers() {
    // Arrange
    AccessToken aliceToken =
        new AccessToken("alice-token", Instant.ofEpochSecond(1700000000L), "alice-id");
    AccessToken bobToken =
        new AccessToken("bob-token", Instant.ofEpochSecond(1800000000L), "bob-id");

    // Act
    cache.save("alice", aliceToken);
    cache.save("bob", bobToken);

    // Assert
    AccessToken loadedAlice = cache.load("alice");
    AccessToken loadedBob = cache.load("bob");

    assertThat(loadedAlice).isNotNull();
    assertThat(loadedAlice.getToken()).isEqualTo("alice-token");
    assertThat(loadedBob).isNotNull();
    assertThat(loadedBob.getToken()).isEqualTo("bob-token");
  }

  @Test
  void save_ShouldCreateDirectoryIfNotExists() {
    // Arrange
    assertThat(Files.exists(cacheDir)).isFalse();
    AccessToken token = new AccessToken("token", Instant.ofEpochSecond(1700000000L), "user-id");

    // Act
    cache.save("alice", token);

    // Assert
    assertThat(Files.isDirectory(cacheDir)).isTrue();
  }

  @Test
  void load_ShouldReturnNull_WhenFileIsCorrupted() throws IOException {
    // Arrange
    Files.createDirectories(cacheDir);
    Path tokenFile = cache.resolveTokenFile("alice");
    Files.write(tokenFile, "not a valid properties content\n\0\0\0".getBytes(UTF_8));

    // Act & Assert - should not throw, just return null or a token with missing fields
    AccessToken loaded = cache.load("alice");
    // The properties file will be parsed but will have no valid keys
    assertThat(loaded).isNull();
  }

  @Test
  void load_ShouldReturnNull_WhenExpiresAtIsNotANumber() throws IOException {
    // Arrange
    Files.createDirectories(cacheDir);
    Path tokenFile = cache.resolveTokenFile("alice");
    String content =
        FileTokenCache.PROP_TOKEN
            + "=my-token\n"
            + FileTokenCache.PROP_EXPIRES_AT
            + "=not-a-number\n"
            + FileTokenCache.PROP_USER_ID
            + "=user-id\n";
    Files.write(tokenFile, content.getBytes(UTF_8));

    // Act & Assert
    assertThat(cache.load("alice")).isNull();
  }

  @Test
  void load_ShouldReturnNull_WhenRequiredFieldMissing() throws IOException {
    // Arrange
    Files.createDirectories(cacheDir);
    Path tokenFile = cache.resolveTokenFile("alice");
    // Missing userId
    String content =
        FileTokenCache.PROP_TOKEN
            + "=my-token\n"
            + FileTokenCache.PROP_EXPIRES_AT
            + "=1700000000\n";
    Files.write(tokenFile, content.getBytes(UTF_8));

    // Act & Assert
    assertThat(cache.load("alice")).isNull();
  }

  @Test
  void resolveTokenFile_ShouldUseSha256OfUsername() {
    // Act
    Path file = cache.resolveTokenFile("alice");

    // Assert
    String expectedHash = FileTokenCache.sha256Hex("alice");
    assertThat(file.getFileName().toString()).isEqualTo("token-" + expectedHash + ".properties");
    assertThat(file.getParent()).isEqualTo(cacheDir);
  }

  @Test
  void sha256Hex_ShouldReturnConsistentHash() {
    // Act & Assert
    String hash1 = FileTokenCache.sha256Hex("test");
    String hash2 = FileTokenCache.sha256Hex("test");
    assertThat(hash1).isEqualTo(hash2);
    assertThat(hash1).hasSize(64); // SHA-256 produces 32 bytes = 64 hex chars
  }

  @Test
  void sha256Hex_ShouldReturnDifferentHashesForDifferentInputs() {
    // Act & Assert
    assertThat(FileTokenCache.sha256Hex("alice")).isNotEqualTo(FileTokenCache.sha256Hex("bob"));
  }
}
