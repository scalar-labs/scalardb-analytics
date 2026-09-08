/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.auth;

import java.time.Instant;
import lombok.Value;

/**
 * Access token returned to clients after successful authentication.
 *
 * <p>Clients use this token to authenticate subsequent API requests by including it in the
 * Authorization header. The token should be treated as opaque; clients should not parse or rely on
 * its internal structure.
 */
@Value
public class AccessToken {
  /** The opaque token string to be included in API requests. */
  String token;

  /** The time at which this token expires and can no longer be used. */
  Instant expiresAt;

  /** The authenticated user's ID. Clients include this in the x-user-id header. */
  String userId;
}
