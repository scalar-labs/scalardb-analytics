/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.jspecify.annotations.Nullable;

/**
 * Represents the result of a command execution in the CLI client. This interface defines the
 * possible outcomes: Success, Failure, or CommandError.
 */
public interface CommandResult {
  /** Indicates a successful command execution. */
  class Success implements CommandResult {
    private final String message;
    private final Object result;

    public Success(String message, Object result) {
      this.message = message;
      this.result = result;
    }

    public Success(Object result) {
      this("Success", result);
    }

    @JsonProperty("message")
    public String message() {
      return message;
    }

    @JsonProperty("result")
    public Object result() {
      return result;
    }
  }

  /** Indicates a failed command execution with a message. */
  class Failure implements CommandResult {
    // The message is nullable because it is often taken from Throwable.getMessage(), which may
    // return null.
    @Nullable private final String message;

    public Failure(@Nullable String message) {
      this.message = message;
    }

    @JsonProperty("message")
    public @Nullable String message() {
      return message;
    }
  }

  /** Indicates a command execution error with an exception. */
  class CommandError implements CommandResult {
    private final String message;
    private final Exception exception;

    public CommandError(String message, Exception exception) {
      this.message = message;
      this.exception = exception;
    }

    @JsonProperty("message")
    public String message() {
      return message;
    }

    @JsonProperty("exception")
    public Exception exception() {
      return exception;
    }
  }
}
