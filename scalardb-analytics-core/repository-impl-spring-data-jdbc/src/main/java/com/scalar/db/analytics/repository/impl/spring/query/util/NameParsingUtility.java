/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.query.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.stereotype.Component;

/** Utility class for parsing namespace names from ScalarDB SQL result sets. */
@Component
public class NameParsingUtility {

  private final ObjectMapper objectMapper;

  public NameParsingUtility(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

  /** Parses namespace names from the default column ("names"). */
  public List<String> parseNamesFromResultSet(ResultSet rs) throws IOException {
    try {
      return parseNamesFromColumn(rs, "names");
    } catch (SQLException e) {
      throw new IOException("Failed to read column 'names'", e);
    }
  }

  public List<String> parseNamesFromColumn(ResultSet rs, String columnLabel)
      throws SQLException, IOException {
    String namesJson = rs.getString(columnLabel);
    return parseNamesFromString(namesJson);
  }

  public List<String> parseNamesFromString(String namesJson) throws IOException {
    if (namesJson != null && !namesJson.isEmpty()) {
      return objectMapper.readValue(namesJson, new TypeReference<List<String>>() {});
    }
    return List.of();
  }
}
