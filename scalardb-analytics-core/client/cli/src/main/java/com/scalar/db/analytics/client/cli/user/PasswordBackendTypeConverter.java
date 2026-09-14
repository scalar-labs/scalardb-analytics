/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.user;

import com.scalar.db.analytics.api.auth.PasswordBackendType;
import picocli.CommandLine.ITypeConverter;

/**
 * picocli converter that resolves a user-supplied {@code --backend} string into a {@link
 * PasswordBackendType} via the case-insensitive {@link PasswordBackendType#fromString(String)}
 * factory. The factory's {@link IllegalArgumentException} is propagated as-is so picocli surfaces
 * the accepted-values list to the user.
 */
public class PasswordBackendTypeConverter implements ITypeConverter<PasswordBackendType> {
  @Override
  public PasswordBackendType convert(String value) {
    return PasswordBackendType.fromString(value);
  }
}
