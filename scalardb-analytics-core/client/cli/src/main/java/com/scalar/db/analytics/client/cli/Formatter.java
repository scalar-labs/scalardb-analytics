/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalar.db.analytics.client.exception.ClientException;
import com.scalar.db.analytics.client.exception.ErrorDetail.FormatError;

/** A formatter interface to format the {@link CommandResult} object. */
public interface Formatter {
  String format(CommandResult result) throws ClientException;

  /** A {@link Formatter} that formats the {@link CommandResult} object as a JSON string. */
  class JsonFormatter implements Formatter {
    @Override
    public String format(CommandResult result) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.writeValueAsString(result);
      } catch (JsonProcessingException e) {
        throw new ClientException(new FormatError(e));
      }
    }
  }
}
