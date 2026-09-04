/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.domain.auth.internal;

/**
 * Credential stored by the internal password authentication backend.
 *
 * <p>Each record represents a single user's login credential in the internal directory. The
 * password is stored as a BCrypt hash and never in plaintext.
 *
 * @param username the unique username used for login
 * @param passwordHash the BCrypt-hashed password
 */
public record InternalCredential(String username, String passwordHash) {}
