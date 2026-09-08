/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Public API models for authentication.
 *
 * <p>Types in this package are returned to clients through the gRPC boundary:
 *
 * <ul>
 *   <li>{@link com.scalar.db.analytics.api.auth.AuthUser} - Authenticated user identity (userId
 *       only).
 *   <li>{@link com.scalar.db.analytics.api.auth.AccessToken} - Opaque token for subsequent API
 *       requests.
 *   <li>{@link com.scalar.db.analytics.api.auth.PasswordCredential} - Username/password input for
 *       authentication.
 * </ul>
 */
@NullMarked
package com.scalar.db.analytics.api.auth;

import org.jspecify.annotations.NullMarked;
