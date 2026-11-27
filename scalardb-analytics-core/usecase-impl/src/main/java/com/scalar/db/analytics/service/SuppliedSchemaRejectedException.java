/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.service;

/**
 * Thrown by a {@link SchemaResolution} that does not accept how a schema was or was not supplied
 * with a registration request for a provider.
 *
 * <p>{@link DataSourceService} turns it into the error returned to the caller, so an implementation
 * reports only the reason.
 */
public class SuppliedSchemaRejectedException extends Exception {
  private static final long serialVersionUID = 4871525012658744914L;

  /** Why the supplied schema, or its absence, is not accepted. */
  public enum Reason {
    /** A schema was supplied for a provider whose schema is resolved without one. */
    NOT_ALLOWED_FOR_PROVIDER,
    /** No schema was supplied for a provider whose schema can only be supplied. */
    REQUIRED_FOR_PROVIDER
  }

  private final Reason reason;

  public SuppliedSchemaRejectedException(Reason reason, String message) {
    super(message);
    this.reason = reason;
  }

  public Reason getReason() {
    return reason;
  }
}
