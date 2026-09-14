/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Internal domain models for authentication.
 *
 * <p>Types in this package are used only within the server and are not exposed to clients:
 *
 * <ul>
 *   <li>{@link com.scalar.db.analytics.domain.auth.PasswordIdentity} - Maps backend-specific
 *       identities to internal user IDs.
 *   <li>{@link com.scalar.db.analytics.domain.auth.IssuedToken} - Server-side record of an issued
 *       access token.
 * </ul>
 */
@NullMarked
package com.scalar.db.analytics.domain.auth;

import org.jspecify.annotations.NullMarked;
