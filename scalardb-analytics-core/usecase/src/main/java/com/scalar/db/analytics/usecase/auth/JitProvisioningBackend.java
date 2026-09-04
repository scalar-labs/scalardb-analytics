/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.usecase.auth;

/**
 * A password authentication backend that supports JIT (Just-In-Time) user provisioning.
 *
 * <p>When a backend implements this interface, the authentication use case will automatically
 * create an {@code AuthUser} and {@code PasswordIdentity} upon the first successful authentication,
 * rather than requiring pre-registration. {@link #getBackendType()} (inherited from {@link
 * PasswordAuthenticationBackend}) provides the backend type recorded on the provisioned identity.
 */
public interface JitProvisioningBackend extends PasswordAuthenticationBackend {}
