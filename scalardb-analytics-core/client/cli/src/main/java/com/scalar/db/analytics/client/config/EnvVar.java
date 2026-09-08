/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a configuration property as mappable from an environment variable.
 *
 * <p>If no value is specified, the environment variable name is automatically generated from the
 * property key using the following convention:
 *
 * <ul>
 *   <li>Convert to uppercase
 *   <li>Replace dots with underscores
 *   <li>Preserve existing underscores
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>
 * &#64;Config.Key("scalar.db.analytics.server.port")
 * &#64;EnvVar  // Auto-generates: SCALAR_DB_ANALYTICS_SERVER_PORT
 * Integer port();
 *
 * &#64;Config.Key("server.tls.cert.path")
 * &#64;EnvVar  // Auto-generates: SERVER_TLS_CERT_PATH
 * String certPath();
 *
 * &#64;Config.Key("custom.property")
 * &#64;EnvVar("MY_CUSTOM_ENV_VAR")  // Explicit mapping
 * String customProperty();
 * </pre>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface EnvVar {
  /**
   * Explicitly specify the environment variable name. If empty (default), it will be auto-generated
   * from the property key.
   *
   * @return the environment variable name, or empty string for auto-generation
   */
  String value() default "";
}
