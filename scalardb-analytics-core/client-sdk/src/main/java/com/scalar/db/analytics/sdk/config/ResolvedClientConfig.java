/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.config;

import com.scalar.db.analytics.api.auth.PasswordCredential;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/** Client configuration resolved by {@link ClientConfigResolver}. */
public final class ResolvedClientConfig {
  @Nullable private final PasswordCredential passwordCredential;

  ResolvedClientConfig(@Nullable PasswordCredential passwordCredential) {
    this.passwordCredential = passwordCredential;
  }

  /** Returns the password credential, or empty if authentication is not configured. */
  public Optional<PasswordCredential> passwordCredential() {
    return Optional.ofNullable(passwordCredential);
  }
}
