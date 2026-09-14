/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.exception;

import org.jspecify.annotations.Nullable;

public interface ErrorDetail extends java.io.Serializable {
  class ConfigLoadError implements ErrorDetail {
    private final Exception cause;

    public ConfigLoadError(Exception cause) {
      this.cause = cause;
    }

    public Exception cause() {
      return cause;
    }
  }

  class ConfigValidationError implements ErrorDetail {
    private final String key;
    private final ConfigValidationErrorDetail detail;

    public ConfigValidationError(String key, ConfigValidationErrorDetail detail) {
      this.key = key;
      this.detail = detail;
    }

    public String key() {
      return key;
    }

    public ConfigValidationErrorDetail detail() {
      return detail;
    }
  }

  class FormatError implements ErrorDetail {
    private final Exception cause;

    public FormatError(Exception cause) {
      this.cause = cause;
    }

    public Exception cause() {
      return cause;
    }
  }

  class DataSourceDefinitionError implements ErrorDetail {
    private final String message;
    @Nullable private final Exception cause;

    public DataSourceDefinitionError(String message, @Nullable Exception cause) {
      this.message = message;
      this.cause = cause;
    }

    public DataSourceDefinitionError(String message) {
      this(message, null);
    }

    public String message() {
      return message;
    }

    @Nullable
    public Exception cause() {
      return cause;
    }
  }

  class InvalidCommandParameter implements ErrorDetail {
    private final String message;

    public InvalidCommandParameter(String message) {
      this.message = message;
    }

    public String message() {
      return message;
    }
  }

  default String getMessage() {
    if (this instanceof ConfigLoadError) {
      ConfigLoadError error = (ConfigLoadError) this;
      return "Error occurred while loading config: " + error.cause().getMessage();
    } else if (this instanceof ConfigValidationError) {
      ConfigValidationError error = (ConfigValidationError) this;
      return String.format("%s: %s", error.key(), error.detail().getMessage());
    } else if (this instanceof FormatError) {
      FormatError error = (FormatError) this;
      return "Error occurred while formatting output: " + error.cause().getMessage();
    } else if (this instanceof DataSourceDefinitionError) {
      DataSourceDefinitionError error = (DataSourceDefinitionError) this;
      return error.message();
    } else if (this instanceof InvalidCommandParameter) {
      InvalidCommandParameter error = (InvalidCommandParameter) this;
      return error.message();
    } else {
      throw new IllegalStateException("Unknown ErrorDetail type: " + this.getClass());
    }
  }

  @Nullable
  default Exception getCause() {
    if (this instanceof ConfigLoadError) {
      ConfigLoadError error = (ConfigLoadError) this;
      return error.cause();
    } else if (this instanceof ConfigValidationError) {
      return null;
    } else if (this instanceof FormatError) {
      FormatError error = (FormatError) this;
      return error.cause();
    } else if (this instanceof DataSourceDefinitionError) {
      DataSourceDefinitionError error = (DataSourceDefinitionError) this;
      return error.cause();
    } else if (this instanceof InvalidCommandParameter) {
      return null;
    } else {
      throw new IllegalStateException("Unknown ErrorDetail type: " + this.getClass());
    }
  }
}
