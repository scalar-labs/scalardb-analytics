/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.config;

import com.scalar.db.analytics.api.auth.PasswordCredential;
import org.jspecify.annotations.Nullable;

/**
 * Resolves client configuration from explicit values, environment variables, and a configuration
 * file, in that order of precedence.
 *
 * <p>The SDK builder, the CLI, and the Spark connector all resolve through this class, so
 * equivalent inputs produce the same configuration in each of them. The rules are:
 *
 * <ul>
 *   <li>An explicit {@link PasswordCredential} wins as a whole; it is never mixed with values from
 *       other sources.
 *   <li>Otherwise each field is resolved on its own: the environment first, then the file. So the
 *       username and the password may come from different sources.
 *   <li>Each source's value is normalized before it is compared with the next source. A blank
 *       username is treated as unset and falls through to the next source. A password is used as
 *       is: it is not trimmed, and an empty password counts as set.
 *   <li>Values are never expanded or otherwise transformed.
 *   <li>If neither the username nor the password is set, authentication is not configured. If only
 *       one of them is set, a {@link ClientConfigException} is thrown.
 * </ul>
 */
public final class ClientConfigResolver {
  private ClientConfigResolver() {}

  /**
   * Resolves the client configuration from the environment and the configuration file only.
   *
   * @param environment the environment source
   * @param file the configuration file source
   * @return the resolved configuration
   * @throws ClientConfigException if the configuration is incomplete
   */
  public static ResolvedClientConfig resolve(ConfigSource environment, ConfigSource file) {
    return resolve(null, environment, file);
  }

  /**
   * Resolves the client configuration.
   *
   * @param explicitCredential the credential given directly, for example through {@code
   *     ScalarDbAnalyticsClient.Builder#passwordCredential}, or {@code null} if none
   * @param environment the environment source
   * @param file the configuration file source
   * @return the resolved configuration
   * @throws ClientConfigException if the configuration is incomplete
   */
  public static ResolvedClientConfig resolve(
      @Nullable PasswordCredential explicitCredential,
      ConfigSource environment,
      ConfigSource file) {
    if (explicitCredential != null) {
      return new ResolvedClientConfig(explicitCredential);
    }

    String username = resolveField(ClientConfigKey.AUTH_USERNAME, environment, file);
    String password = resolveField(ClientConfigKey.AUTH_PASSWORD, environment, file);
    if (username == null && password == null) {
      return new ResolvedClientConfig(null);
    }
    if (username == null) {
      throw new ClientConfigException(ClientConfigKey.AUTH_USERNAME);
    }
    if (password == null) {
      throw new ClientConfigException(ClientConfigKey.AUTH_PASSWORD);
    }
    return new ResolvedClientConfig(new PasswordCredential(username, password));
  }

  @Nullable
  private static String resolveField(ClientConfigKey key, ConfigSource... sources) {
    for (ConfigSource source : sources) {
      String value = key.normalize(source.get(key));
      if (value != null) {
        return value;
      }
    }
    return null;
  }
}
