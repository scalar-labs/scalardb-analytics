/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.autoconfigure;

import com.scalar.db.analytics.repository.impl.spring.constants.ScalarDbNamespaces;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "scalar.db.analytics.server.db")
@Validated
@Data
@NoArgsConstructor
public class ScalarDbSqlProperties {

  private static final Logger logger = LoggerFactory.getLogger(ScalarDbSqlProperties.class);

  static final String ISOLATION_LEVEL = "scalar.db.consensus_commit.isolation_level";

  /** Deprecated ScalarDB alias of {@link #ISOLATION_LEVEL}, still read by ScalarDB 3.x. */
  static final String DEPRECATED_ISOLATION_LEVEL = "scalar.db.isolation_level";

  static final String REQUIRED_ISOLATION_LEVEL = "SERIALIZABLE";

  public ScalarDbSqlProperties(String contactPoints, @Nullable String username, String password) {
    this.contactPoints = contactPoints;
    this.username = username;
    this.password = password;
  }

  // Populated by Spring from configuration after no-arg construction; @NotBlank enforces presence.
  @SuppressWarnings("NullAway.Init")
  @NotBlank
  private String contactPoints;

  @Nullable private String username;

  private String password = "";

  private final Map<String, String> parameters = new LinkedHashMap<>();

  // toMapWithDefaults() is called more than once at startup (the data source and the migrator), so
  // the override warning is logged only the first time.
  @Getter(AccessLevel.NONE)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  private final AtomicBoolean isolationOverrideLogged = new AtomicBoolean();

  public Map<String, String> toMapWithDefaults() {
    LinkedHashMap<String, String> params = new LinkedHashMap<>();
    params.put("scalar.db.storage", "jdbc");
    params.put("scalar.db.sql.connection_mode", "direct");
    params.put("scalar.db.sql.default_namespace_name", ScalarDbNamespaces.NAMESPACE);
    params.put("scalar.db.sql.default_namespace_name.existence_check.enabled", "false");
    // Enabled globally so principal listing (UserUseCaseImpl#listUsers ->
    // AuthUserRepository.findAll) can scan auth_user. This is a server-wide default, not a
    // per-query setting. Whether to localize it instead -- e.g. a fixed-value directory_id
    // secondary index on auth_user so findAll becomes a bounded index scan and this default can
    // revert to false -- is tracked, with the related stale-doc and scan-avoidance cleanups, in
    // #505.
    params.put("scalar.db.cross_partition_scan.enabled", "true");
    params.put("scalar.db.contact_points", contactPoints);

    if (username != null && !username.isBlank()) {
      params.put("scalar.db.username", username);
    }
    if (password != null && !password.isBlank()) {
      params.put("scalar.db.password", password);
    }

    params.putAll(parameters);
    enforceSerializableIsolation(params);
    return params;
  }

  /**
   * Forces the SERIALIZABLE isolation level, overriding any level given in {@code parameters}.
   *
   * <p>The repositories check that a name is free and then insert in the same transaction. Only
   * SERIALIZABLE makes that check-then-insert safe against a concurrent create of the same name;
   * under SNAPSHOT or READ_COMMITTED both transactions commit and leave a duplicate that can no
   * longer be looked up by name. The losing transaction aborts with a conflict, which the
   * repository reports as the retryable {@code ANALYTICS_DB_TRANSACTION_CONFLICT}.
   */
  private void enforceSerializableIsolation(Map<String, String> params) {
    StringBuilder overridden = new StringBuilder();
    for (String key : List.of(ISOLATION_LEVEL, DEPRECATED_ISOLATION_LEVEL)) {
      String configured = params.remove(key);
      if (configured != null && !REQUIRED_ISOLATION_LEVEL.equalsIgnoreCase(configured.trim())) {
        overridden
            .append(overridden.length() == 0 ? "" : ", ")
            .append(key)
            .append('=')
            .append(configured);
      }
    }
    params.put(ISOLATION_LEVEL, REQUIRED_ISOLATION_LEVEL);

    if (overridden.length() > 0 && isolationOverrideLogged.compareAndSet(false, true)) {
      logger.warn(
          "Ignoring {} in scalar.db.analytics.server.db.parameters: the server always uses the {}"
              + " isolation level for its database ({}={}) so that concurrent requests cannot"
              + " create duplicate catalog metadata",
          overridden,
          REQUIRED_ISOLATION_LEVEL,
          ISOLATION_LEVEL,
          REQUIRED_ISOLATION_LEVEL);
    }
  }

  public Properties toProperties() {
    Properties props = new Properties();
    toMapWithDefaults().forEach(props::setProperty);
    return props;
  }
}
