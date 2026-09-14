/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.util;

import java.util.Optional;
import org.jspecify.annotations.Nullable;

public class Either<L, R> {
  @Nullable private final L left;

  @Nullable private final R right;

  private Either(@Nullable L left, @Nullable R right) {
    this.left = left;
    this.right = right;
  }

  public static <L, R> Either<L, R> left(L value) {
    return new Either<>(value, null);
  }

  public static <L, R> Either<L, R> right(R value) {
    return new Either<>(null, value);
  }

  @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
  public static <L, R> Either<L, R> fromOptional(Optional<R> optional, L errorValue) {
    return optional.map(Either::<L, R>right).orElseGet(() -> Either.left(errorValue));
  }

  public boolean isLeft() {
    return left != null;
  }

  public boolean isRight() {
    return right != null;
  }

  @Nullable
  public L getLeft() {
    return left;
  }

  @Nullable
  public R getRight() {
    return right;
  }
}
