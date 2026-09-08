/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.auth.internal;

import com.scalar.db.analytics.domain.auth.internal.InternalCredential;
import com.scalar.db.analytics.repository.RepositoryTransactionContext;
import java.util.Optional;

/**
 * Repository for managing {@link InternalCredential} records.
 *
 * <p>Used exclusively by the internal password authentication backend to store and retrieve
 * username/password-hash pairs.
 */
public interface InternalCredentialRepository<T extends RepositoryTransactionContext> {

  /** Persists a new credential. Throws if the username already exists. */
  void create(T ctx, InternalCredential credential);

  /** Finds a credential by username, or empty if not found. */
  Optional<InternalCredential> findByUsername(T ctx, String username);

  /** Deletes the credential for the given username. No-op if not found. */
  void deleteByUsername(T ctx, String username);
}
