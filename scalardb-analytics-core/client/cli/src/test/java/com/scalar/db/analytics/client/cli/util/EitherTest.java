/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EitherTest {

  @Nested
  @SuppressWarnings("ClassCanBeStatic")
  class Left {
    @Test
    void shouldCreateLeftValue() {
      Either<String, Integer> either = Either.left("error");

      assertThat(either.isLeft()).isTrue();
      assertThat(either.isRight()).isFalse();
      assertThat(either.getLeft()).isEqualTo("error");
      assertThat(either.getRight()).isNull();
    }
  }

  @Nested
  @SuppressWarnings("ClassCanBeStatic")
  class Right {
    @Test
    void shouldCreateRightValue() {
      Either<String, Integer> either = Either.right(42);

      assertThat(either.isLeft()).isFalse();
      assertThat(either.isRight()).isTrue();
      assertThat(either.getLeft()).isNull();
      assertThat(either.getRight()).isEqualTo(42);
    }
  }

  @Nested
  @SuppressWarnings("ClassCanBeStatic")
  class FromOptional {
    @Test
    void givenPresentOptional_shouldCreateRight() {
      Optional<Integer> optional = Optional.of(42);

      Either<String, Integer> either = Either.fromOptional(optional, "error");

      assertThat(either.isLeft()).isFalse();
      assertThat(either.isRight()).isTrue();
      assertThat(either.getRight()).isEqualTo(42);
      assertThat(either.getLeft()).isNull();
    }

    @Test
    void givenEmptyOptional_shouldCreateLeft() {
      Optional<Integer> optional = Optional.empty();

      Either<String, Integer> either = Either.fromOptional(optional, "error");

      assertThat(either.isLeft()).isTrue();
      assertThat(either.isRight()).isFalse();
      assertThat(either.getLeft()).isEqualTo("error");
      assertThat(either.getRight()).isNull();
    }
  }

  @Nested
  @SuppressWarnings("ClassCanBeStatic")
  class TypeVariance {
    @Test
    void shouldWorkWithDifferentTypes() {
      Either<Exception, String> errorCase = Either.left(new RuntimeException("error"));
      Either<Exception, String> successCase = Either.right("success");

      assertThat(errorCase.isLeft()).isTrue();
      assertThat(errorCase.getLeft()).isInstanceOf(RuntimeException.class);

      assertThat(successCase.isRight()).isTrue();
      assertThat(successCase.getRight()).isEqualTo("success");
    }
  }
}
