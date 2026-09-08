/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.lib.functional;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Lombok;

public class Throwables {
  /**
   * Converts a {@link ThrowableFunction} to a {@link Function}. This deceives the compiler by
   * throwing a runtime exception when the function throws a checked exception. This is useful when
   * you want to use a lambda expression that throws a checked exception in a place where a
   * functional interface is expected.
   *
   * @param function the function that throws a checked exception
   * @param <T> the type of the input to the function
   * @param <R> the type of the return value from the function
   * @param <E> the type of the checked exception
   * @return a function that throws a runtime exception
   */
  public static <T, R, E extends Exception> Function<T, R> sneakyThrowableFunction(
      ThrowableFunction<T, R, E> function) {
    return new Function<T, R>() {
      @Override
      public R apply(T t) {
        try {
          return function.apply(t);
        } catch (Exception e) {
          throw Lombok.sneakyThrow(e);
        }
      }
    };
  }

  /**
   * Converts a {@link ThrowableSupplier} to a {@link Supplier}. This deceives the compiler by
   * throwing a runtime exception when the supplier throws a checked exception. This is useful when
   * you want to use a lambda expression that throws a checked exception in a place where a
   * functional interface is expected.
   *
   * @param supplier the supplier that throws a checked exception
   * @param <R> the type of the return value from the function
   * @param <E> the type of the checked exception
   * @return a supplier that throws a runtime exception
   */
  public static <R, E extends Exception> Supplier<R> sneakyThrowableSupplier(
      ThrowableSupplier<R, E> supplier) {
    return new Supplier<R>() {
      @Override
      public R get() {
        try {
          return supplier.get();
        } catch (Exception e) {
          throw Lombok.sneakyThrow(e);
        }
      }
    };
  }

  /**
   * Converts a {@link ThrowableConsumer} to a {@link Consumer}. This deceives the compiler by
   * throwing a runtime exception when the consumer throws a checked exception. This is useful when
   * you want to use a lambda expression that throws a checked exception in a place where a
   * functional interface is expected.
   *
   * @param consumer the consumer that throws a checked exception
   * @param <T> the type of the input to the function
   * @param <E> the type of the checked exception
   * @return a consumer that throws a runtime exception
   */
  public static <T, E extends Exception> Consumer<T> sneakyThrowableConsumer(
      ThrowableConsumer<T, E> consumer) {
    return new Consumer<T>() {
      @Override
      public void accept(T t) {
        try {
          consumer.accept(t);
        } catch (Exception e) {
          throw Lombok.sneakyThrow(e);
        }
      }
    };
  }
}
