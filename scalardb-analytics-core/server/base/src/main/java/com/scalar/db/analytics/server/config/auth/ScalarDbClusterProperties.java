/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.server.config.auth;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "scalar.db.analytics.server.auth.password.scalardb-cluster")
public class ScalarDbClusterProperties {

  @NotBlank private String host = "localhost";

  @Min(1)
  @Max(65535)
  private int port = 60053;

  @Min(1)
  private long deadlineMillis = 5000;

  private boolean aclDelegation = false;

  private final Tls tls = new Tls();

  public boolean isAclDelegation() {
    return aclDelegation;
  }

  public void setAclDelegation(boolean aclDelegation) {
    this.aclDelegation = aclDelegation;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  public int getPort() {
    return port;
  }

  public void setPort(int port) {
    this.port = port;
  }

  public long getDeadlineMillis() {
    return deadlineMillis;
  }

  public void setDeadlineMillis(long deadlineMillis) {
    this.deadlineMillis = deadlineMillis;
  }

  public Tls getTls() {
    return tls;
  }

  public static class Tls {
    private boolean enabled = false;
    @Nullable private String caRootCertPath;
    @Nullable private String overrideAuthority;

    public boolean isEnabled() {
      return enabled;
    }

    public void setEnabled(boolean enabled) {
      this.enabled = enabled;
    }

    @Nullable
    public String getCaRootCertPath() {
      return caRootCertPath;
    }

    public void setCaRootCertPath(@Nullable String caRootCertPath) {
      this.caRootCertPath = caRootCertPath;
    }

    @Nullable
    public String getOverrideAuthority() {
      return overrideAuthority;
    }

    public void setOverrideAuthority(@Nullable String overrideAuthority) {
      this.overrideAuthority = overrideAuthority;
    }
  }
}
