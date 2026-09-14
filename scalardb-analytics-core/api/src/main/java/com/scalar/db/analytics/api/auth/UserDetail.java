/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * Detailed information about a registered user, including assigned roles.
 *
 * <p>{@link #backendUserId} and {@link #backend} are nullable: a principal may exist without any
 * backend-user association (e.g. just created with {@code user create}). When both are {@code
 * null}, the principal has no associated backend identity; they are then omitted from the
 * serialized output rather than rendered as {@code null}.
 */
@Value
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserDetail {
  String userId;
  @Nullable String backendUserId;
  @Nullable PasswordBackendType backend;
  List<String> roles;
  String username;
}
