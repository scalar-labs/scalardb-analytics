/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import com.scalar.db.analytics.api.auth.PasswordCredential;
import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;

class ClientConfigResolverTest {
  private static final String USERNAME_ENV = "SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME";
  private static final String PASSWORD_ENV = "SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD";
  private static final String USERNAME_KEY = "scalar.db.analytics.client.auth.username";
  private static final String PASSWORD_KEY = "scalar.db.analytics.client.auth.password";

  @Nested
  class SharedCases {
    @TestFactory
    Stream<DynamicTest> withPropertiesSource() {
      return CredentialResolutionCase.load().stream()
          .map(
              c ->
                  DynamicTest.dynamicTest(
                      c.name,
                      () ->
                          assertCase(
                              c,
                              () ->
                                  ClientConfigResolver.resolve(
                                      null,
                                      ConfigSource.environment(c.environmentVariables()::get),
                                      ConfigSource.properties(c.fileProperties())))));
    }

    @TestFactory
    Stream<DynamicTest> withFileSource(@TempDir Path tempDir) {
      return CredentialResolutionCase.load().stream()
          .map(
              c ->
                  DynamicTest.dynamicTest(
                      c.name,
                      () -> {
                        Path file = tempDir.resolve(c.name + ".properties");
                        Properties props = new Properties();
                        props.putAll(c.fileProperties());
                        try (OutputStream out = Files.newOutputStream(file)) {
                          props.store(out, null);
                        }
                        assertCase(
                            c,
                            () ->
                                ClientConfigResolver.resolve(
                                    null,
                                    ConfigSource.environment(c.environmentVariables()::get),
                                    ConfigSource.file(file)));
                      }));
    }
  }

  static void assertCase(CredentialResolutionCase c, Supplier<ResolvedClientConfig> resolve) {
    if (c.missingRelativeKey != null) {
      assertThatThrownBy(resolve::get)
          .isInstanceOfSatisfying(
              ClientConfigException.class,
              e -> assertThat(e.key().relativeKey()).isEqualTo(c.missingRelativeKey));
      return;
    }
    Optional<PasswordCredential> credential = resolve.get().passwordCredential();
    if (c.expectedUsername == null) {
      assertThat(credential).isEmpty();
    } else {
      assertThat(credential)
          .contains(
              new PasswordCredential(
                  c.expectedUsername, Objects.requireNonNull(c.expectedPassword)));
    }
  }

  @Nested
  class Resolve {
    @Test
    void givenExplicitCredential_shouldIgnoreOtherSources() {
      PasswordCredential explicit = new PasswordCredential("explicit", "secret");

      ResolvedClientConfig resolved =
          ClientConfigResolver.resolve(
              explicit,
              env(USERNAME_ENV, "env-user"),
              ConfigSource.properties(Collections.singletonMap(PASSWORD_KEY, "file-pass")));

      assertThat(resolved.passwordCredential()).containsSame(explicit);
    }

    @Test
    void givenExplicitCredentialAndIncompleteSources_shouldNotThrow() {
      PasswordCredential explicit = new PasswordCredential("explicit", "secret");

      ResolvedClientConfig resolved =
          ClientConfigResolver.resolve(
              explicit, env(USERNAME_ENV, "env-user"), ConfigSource.empty());

      assertThat(resolved.passwordCredential()).containsSame(explicit);
    }

    @Test
    void givenNoSources_shouldReturnNoCredential() {
      ResolvedClientConfig resolved =
          ClientConfigResolver.resolve(null, ConfigSource.empty(), ConfigSource.empty());

      assertThat(resolved.passwordCredential()).isEmpty();
    }

    @Test
    void givenUsernameOnly_shouldThrowWithPasswordKey() {
      assertThatThrownBy(
              () ->
                  ClientConfigResolver.resolve(
                      null, env(USERNAME_ENV, "user"), ConfigSource.empty()))
          .isInstanceOfSatisfying(
              ClientConfigException.class,
              e ->
                  assertAll(
                      () -> assertThat(e.key()).isEqualTo(ClientConfigKey.AUTH_PASSWORD),
                      () ->
                          assertThat(e.getErrorCode())
                              .isEqualTo(AnalyticsErrorCode.INVALID_ARGUMENT),
                      () -> assertThat(e.getMetadata()).containsEntry("field", PASSWORD_KEY),
                      () -> assertThat(e.getMetadata()).containsEntry("env_var", PASSWORD_ENV),
                      () -> assertThat(e.getMessage()).doesNotContain("user")));
    }

    @Test
    void givenPasswordOnly_shouldThrowWithoutRevealingPassword() {
      assertThatThrownBy(
              () ->
                  ClientConfigResolver.resolve(
                      null, env(PASSWORD_ENV, "s3cr3t"), ConfigSource.empty()))
          .isInstanceOfSatisfying(
              ClientConfigException.class,
              e ->
                  assertAll(
                      () -> assertThat(e.key()).isEqualTo(ClientConfigKey.AUTH_USERNAME),
                      () -> assertThat(e.getMessage()).doesNotContain("s3cr3t")));
    }
  }

  @Nested
  class Keys {
    @Test
    void shouldUseClientPropertyKeysAndEnvVarNames() {
      assertAll(
          () -> assertThat(ClientConfigKey.AUTH_USERNAME.propertyKey()).isEqualTo(USERNAME_KEY),
          () -> assertThat(ClientConfigKey.AUTH_PASSWORD.propertyKey()).isEqualTo(PASSWORD_KEY),
          () -> assertThat(ClientConfigKey.AUTH_USERNAME.envVarName()).isEqualTo(USERNAME_ENV),
          () -> assertThat(ClientConfigKey.AUTH_PASSWORD.envVarName()).isEqualTo(PASSWORD_ENV),
          () -> assertThat(ClientConfigKey.AUTH_USERNAME.relativeKey()).isEqualTo("auth.username"),
          () -> assertThat(ClientConfigKey.AUTH_PASSWORD.relativeKey()).isEqualTo("auth.password"));
    }
  }

  @Nested
  class Sources {
    @Test
    void properties_withKeyMapper_shouldReadMappedKeys() {
      Map<String, String> options = new HashMap<>();
      options.put("server.auth.username", "user");
      options.put("server.auth.password", "pass");
      Function<ClientConfigKey, String> mapper =
          key ->
              key == ClientConfigKey.AUTH_USERNAME
                  ? "server.auth.username"
                  : "server.auth.password";

      ConfigSource source = ConfigSource.properties(options, mapper);

      assertThat(source.get(ClientConfigKey.AUTH_USERNAME)).isEqualTo("user");
      assertThat(source.get(ClientConfigKey.AUTH_PASSWORD)).isEqualTo("pass");
    }

    @Test
    void properties_shouldTakeSnapshot() {
      Map<String, String> props = new HashMap<>();
      props.put(USERNAME_KEY, "before");
      ConfigSource source = ConfigSource.properties(props);

      props.put(USERNAME_KEY, "after");

      assertThat(source.get(ClientConfigKey.AUTH_USERNAME)).isEqualTo("before");
    }

    @Test
    void propertiesFromJavaProperties_shouldReadDefaults() {
      Properties defaults = new Properties();
      defaults.setProperty(USERNAME_KEY, "user");
      Properties props = new Properties(defaults);

      assertThat(ConfigSource.properties(props).get(ClientConfigKey.AUTH_USERNAME))
          .isEqualTo("user");
    }

    @Test
    void file_givenMissingFile_shouldBeEmpty(@TempDir Path tempDir) {
      ConfigSource source = ConfigSource.file(tempDir.resolve("missing.properties"));

      assertThat(source.get(ClientConfigKey.AUTH_USERNAME)).isNull();
      assertThat(source.get(ClientConfigKey.AUTH_PASSWORD)).isNull();
    }

    @Test
    void file_shouldKeepPlaceholdersLiterally(@TempDir Path tempDir) throws IOException {
      Path file = tempDir.resolve("client.properties");
      Files.write(
          file,
          (PASSWORD_KEY + "=p${" + USERNAME_KEY + "}q\n").getBytes(StandardCharsets.ISO_8859_1));

      assertThat(ConfigSource.file(file).get(ClientConfigKey.AUTH_PASSWORD))
          .isEqualTo("p${" + USERNAME_KEY + "}q");
    }

    @Test
    void file_givenUnreadableFile_shouldThrow(@TempDir Path tempDir) throws IOException {
      Path file = Files.createFile(tempDir.resolve("client.properties"));
      Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("---------"));
      // Root can read the file anyway.
      assumeFalse(Files.isReadable(file));

      assertThatThrownBy(() -> ConfigSource.file(file)).isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void file_givenMalformedFile_shouldThrow(@TempDir Path tempDir) throws IOException {
      Path file = tempDir.resolve("client.properties");
      Files.write(file, (USERNAME_KEY + "=\\uZZZZ\n").getBytes(StandardCharsets.ISO_8859_1));

      assertThatThrownBy(() -> ConfigSource.file(file))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  private static ConfigSource env(String name, String value) {
    return ConfigSource.environment(Collections.singletonMap(name, value)::get);
  }
}
