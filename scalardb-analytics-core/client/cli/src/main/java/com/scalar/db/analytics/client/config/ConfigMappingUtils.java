/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.config;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import org.aeonbits.owner.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for discovering and managing configuration mappings between property keys and
 * environment variables.
 */
public final class ConfigMappingUtils {
  private static final Logger logger = LoggerFactory.getLogger(ConfigMappingUtils.class);

  // Cache to avoid repeated reflection
  private static final Map<Class<?>, Map<String, String>> MAPPING_CACHE = new ConcurrentHashMap<>();

  private ConfigMappingUtils() {
    // Prevent instantiation
  }

  /**
   * Loads properties from environment variables based on @EnvVar annotations in the given
   * configuration interface.
   *
   * @param configClass the configuration interface class annotated with @Config methods
   * @return properties loaded from environment variables
   */
  public static Properties loadPropertiesFromEnvironment(Class<? extends Config> configClass) {
    Properties envProps = new Properties();

    // Get mappings (from cache if available)
    Map<String, String> mappings = getEnvironmentMappings(configClass);

    // Apply environment variables
    for (Map.Entry<String, String> entry : mappings.entrySet()) {
      String value = System.getenv(entry.getValue());
      if (value != null) {
        envProps.setProperty(entry.getKey(), value);
      }
    }

    return envProps;
  }

  /**
   * Gets environment variable mappings for the given config class. Results are cached for
   * performance.
   *
   * @param configClass the configuration interface class
   * @return map of property keys to environment variable names
   */
  public static Map<String, String> getEnvironmentMappings(Class<?> configClass) {
    return MAPPING_CACHE.computeIfAbsent(configClass, ConfigMappingUtils::discoverMappings);
  }

  /**
   * Discovers environment variable mappings from annotations on the config interface.
   *
   * @param configClass the configuration interface class
   * @return map of property keys to environment variable names
   */
  static Map<String, String> discoverMappings(Class<?> configClass) {
    Map<String, String> mappings = new HashMap<>();

    for (Method method : configClass.getMethods()) {
      // Skip if method doesn't have @EnvVar annotation
      EnvVar envVar = method.getAnnotation(EnvVar.class);
      if (envVar == null) {
        continue;
      }

      // Get property key from @Config.Key annotation
      Config.Key keyAnnotation = method.getAnnotation(Config.Key.class);
      if (keyAnnotation == null) {
        logger.warn("Method {} has @EnvVar but no @Config.Key annotation", method.getName());
        continue;
      }

      String propertyKey = keyAnnotation.value();
      String envVarName = envVar.value();

      // Auto-generate environment variable name if not specified
      if (envVarName.isEmpty()) {
        envVarName = convertToEnvVarName(propertyKey);
      }

      mappings.put(propertyKey, envVarName);
      logger.debug("Mapped property '{}' to environment variable '{}'", propertyKey, envVarName);
    }

    return mappings;
  }

  /**
   * Converts a property key to environment variable name using convention: - Convert to uppercase -
   * Replace dots with underscores - Preserve existing underscores
   *
   * @param propertyKey the property key to convert
   * @return the environment variable name
   */
  public static String convertToEnvVarName(String propertyKey) {
    if (propertyKey == null || propertyKey.isEmpty()) {
      throw new IllegalArgumentException("Property key cannot be null or empty");
    }
    return propertyKey.toUpperCase().replace('.', '_');
  }

  /** Clears the mapping cache. Useful for testing. */
  static void clearCache() {
    MAPPING_CACHE.clear();
  }

  /**
   * Gets a human-readable description of all mappings for documentation.
   *
   * @param configClass the configuration interface
   * @return formatted string describing all mappings
   */
  public static String describeMappings(Class<?> configClass) {
    StringBuilder sb = new StringBuilder();
    sb.append("Configuration mappings for ").append(configClass.getSimpleName()).append(":\n");

    Map<String, String> mappings = getEnvironmentMappings(configClass);
    mappings.forEach(
        (prop, env) -> sb.append("  ").append(prop).append(" -> ").append(env).append("\n"));

    return sb.toString();
  }
}
