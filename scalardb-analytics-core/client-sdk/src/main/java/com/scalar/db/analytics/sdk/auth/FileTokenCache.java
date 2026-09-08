/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.auth;

import com.google.common.annotations.VisibleForTesting;
import com.scalar.db.analytics.api.auth.AccessToken;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jspecify.annotations.Nullable;

/**
 * A {@link TokenCache} that persists tokens to disk using Java {@link Properties} format.
 *
 * <p>Each username gets a separate file named {@code token-<sha256(username)>.properties}. Files
 * are written atomically via a temporary file and {@link Files#move}. On POSIX systems, the
 * directory is created with permissions {@code 700} and files with {@code 600}.
 *
 * <p>This class is thread-safe. All operations are synchronized to prevent concurrent file access
 * within the same process.
 */
class FileTokenCache implements TokenCache {
  private static final Logger logger = Logger.getLogger(FileTokenCache.class.getName());

  @VisibleForTesting static final String PROP_TOKEN = "token";
  @VisibleForTesting static final String PROP_EXPIRES_AT = "expiresAt";
  @VisibleForTesting static final String PROP_USER_ID = "userId";

  private static final Set<PosixFilePermission> DIR_PERMISSIONS =
      PosixFilePermissions.fromString("rwx------");
  private static final Set<PosixFilePermission> FILE_PERMISSIONS =
      PosixFilePermissions.fromString("rw-------");

  private final Path directory;

  FileTokenCache(Path directory) {
    this.directory = directory;
  }

  @Override
  public synchronized void save(String username, AccessToken token) {
    try {
      ensureDirectory();

      Properties props = new Properties();
      props.setProperty(PROP_TOKEN, token.getToken());
      props.setProperty(PROP_EXPIRES_AT, Long.toString(token.getExpiresAt().getEpochSecond()));
      props.setProperty(PROP_USER_ID, token.getUserId());

      Path targetFile = resolveTokenFile(username);
      Path tempFile = createTempFile();
      try {
        try (OutputStream out = Files.newOutputStream(tempFile)) {
          props.store(out, null);
        }

        try {
          Files.move(
              tempFile,
              targetFile,
              StandardCopyOption.ATOMIC_MOVE,
              StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
          // Fallback to non-atomic move (some file systems don't support ATOMIC_MOVE)
          Files.move(tempFile, targetFile, StandardCopyOption.REPLACE_EXISTING);
        }
      } finally {
        // Clean up temp file if move failed (no-op if move succeeded)
        Files.deleteIfExists(tempFile);
      }
    } catch (IOException e) {
      logger.log(Level.WARNING, "Failed to save token to disk", e);
    }
  }

  @Nullable
  @Override
  public synchronized AccessToken load(String username) {
    try {
      Path tokenFile = resolveTokenFile(username);
      if (!Files.exists(tokenFile)) {
        return null;
      }

      Properties props = new Properties();
      try (InputStream in = Files.newInputStream(tokenFile)) {
        props.load(in);
      }

      String tokenValue = props.getProperty(PROP_TOKEN);
      String expiresAtStr = props.getProperty(PROP_EXPIRES_AT);
      String userId = props.getProperty(PROP_USER_ID);

      if (tokenValue == null || expiresAtStr == null || userId == null) {
        return null;
      }

      Instant expiresAt = Instant.ofEpochSecond(Long.parseLong(expiresAtStr));
      return new AccessToken(tokenValue, expiresAt, userId);
    } catch (IOException | NumberFormatException e) {
      logger.log(Level.WARNING, "Failed to load token from disk", e);
      return null;
    }
  }

  private void ensureDirectory() throws IOException {
    if (!Files.exists(directory)) {
      if (isPosixSupported()) {
        FileAttribute<Set<PosixFilePermission>> attr =
            PosixFilePermissions.asFileAttribute(DIR_PERMISSIONS);
        Files.createDirectories(directory, attr);
      } else {
        Files.createDirectories(directory);
      }
    }
  }

  private Path createTempFile() throws IOException {
    if (isPosixSupported()) {
      FileAttribute<Set<PosixFilePermission>> attr =
          PosixFilePermissions.asFileAttribute(FILE_PERMISSIONS);
      return Files.createTempFile(directory, "token-", ".tmp", attr);
    }
    return Files.createTempFile(directory, "token-", ".tmp");
  }

  private boolean isPosixSupported() {
    return directory.getFileSystem().supportedFileAttributeViews().contains("posix");
  }

  @VisibleForTesting
  Path resolveTokenFile(String username) {
    String hash = sha256Hex(username);
    return directory.resolve("token-" + hash + ".properties");
  }

  @VisibleForTesting
  static String sha256Hex(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
      StringBuilder hex = new StringBuilder(hash.length * 2);
      for (byte b : hash) {
        hex.append(String.format("%02x", b & 0xff));
      }
      return hex.toString();
    } catch (NoSuchAlgorithmException e) {
      // SHA-256 is guaranteed to be available in every JVM
      throw new AssertionError("SHA-256 not available", e);
    }
  }
}
