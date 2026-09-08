/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.auth;

import lombok.Value;

/**
 * Username and password credential for authentication.
 *
 * <p>Used as input to the password authentication flow. The password is the plaintext password
 * provided by the user; it will be verified against the stored hash on the server side.
 */
@Value
public class PasswordCredential {
  /** The username identifying the user account. */
  String username;

  /** The plaintext password to verify. */
  String password;
}
