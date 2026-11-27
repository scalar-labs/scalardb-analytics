/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.config;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

/**
 * A source of raw client configuration values, such as environment variables or a configuration
 * file.
 *
 * <p>A source returns values as they are; normalization (such as treating a blank username as
 * unset) is applied by {@link ClientConfigResolver}. Values are never expanded, so {@code ${...}}
 * in a value is kept literally.
 *
 * <p>Environment sources read the variables each time a value is requested, that is, when the
 * configuration is resolved. Property and file sources take a snapshot when they are created.
 */
@FunctionalInterface
public interface ConfigSource {

  /**
   * Returns the raw value of the key, or {@code null} if this source does not define it.
   *
   * @param key the configuration key
   * @return the raw value, or {@code null}
   */
  @Nullable String get(ClientConfigKey key);

  /** Returns a source that defines no keys. */
  static ConfigSource empty() {
    return key -> null;
  }

  /**
   * Returns a source that reads the process environment, using {@link ClientConfigKey#envVarName()}
   * as variable names.
   */
  static ConfigSource environment() {
    return environment(System::getenv);
  }

  /**
   * Returns a source that reads environment variables through the given function, using {@link
   * ClientConfigKey#envVarName()} as variable names.
   *
   * @param reader returns the value of an environment variable, or {@code null} if it is not set
   */
  static ConfigSource environment(Function<String, @Nullable String> reader) {
    return key -> reader.apply(key.envVarName());
  }

  /**
   * Returns a source backed by a snapshot of the given properties, using {@link
   * ClientConfigKey#propertyKey()} as property keys.
   */
  static ConfigSource properties(Properties properties) {
    Map<String, String> snapshot = new HashMap<>();
    for (String name : properties.stringPropertyNames()) {
      snapshot.put(name, properties.getProperty(name));
    }
    return properties(snapshot);
  }

  /**
   * Returns a source backed by a snapshot of the given map, using {@link
   * ClientConfigKey#propertyKey()} as keys.
   */
  static ConfigSource properties(Map<String, String> properties) {
    return properties(properties, ClientConfigKey::propertyKey);
  }

  /**
   * Returns a source backed by a snapshot of the given map, using {@code keyMapper} to map each
   * configuration key to a key in the map. For example, the Spark connector maps {@link
   * ClientConfigKey#AUTH_USERNAME} to {@code server.auth.username}.
   */
  static ConfigSource properties(
      Map<String, String> properties, Function<ClientConfigKey, String> keyMapper) {
    Map<String, String> snapshot = Collections.unmodifiableMap(new HashMap<>(properties));
    return key -> snapshot.get(keyMapper.apply(key));
  }

  /**
   * Returns a source backed by the properties file at the given path, using {@link
   * ClientConfigKey#propertyKey()} as property keys. The file is read once when this method is
   * called. A missing file is treated as an empty source.
   *
   * @param path the path to a {@code client.properties} file
   * @throws UncheckedIOException if the file exists but cannot be read
   * @throws IllegalArgumentException if the file is not a valid properties file
   */
  static ConfigSource file(Path path) {
    Properties properties = new Properties();
    try (InputStream in = Files.newInputStream(path)) {
      properties.load(in);
    } catch (NoSuchFileException e) {
      return empty();
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    return properties(properties);
  }
}
