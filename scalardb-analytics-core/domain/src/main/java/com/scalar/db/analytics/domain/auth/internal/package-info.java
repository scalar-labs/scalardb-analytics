/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
/**
 * Domain models specific to the internal password authentication backend.
 *
 * <p>Contains types used only when {@link
 * com.scalar.db.analytics.api.auth.PasswordBackendType#INTERNAL} is active:
 *
 * <ul>
 *   <li>{@link com.scalar.db.analytics.domain.auth.internal.InternalCredential} - Username and
 *       hashed password stored by the internal backend.
 * </ul>
 */
@NullMarked
package com.scalar.db.analytics.domain.auth.internal;

import org.jspecify.annotations.NullMarked;
