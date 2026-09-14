/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.autoconfigure;

import com.scalar.db.analytics.repository.impl.spring.constants.ScalarDbNamespaces;
import jakarta.validation.constraints.NotBlank;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "scalar.db.analytics.server.db")
@Validated
@Data
@NoArgsConstructor
public class ScalarDbSqlProperties {

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
    return params;
  }

  public Properties toProperties() {
    Properties props = new Properties();
    toMapWithDefaults().forEach(props::setProperty);
    return props;
  }
}
