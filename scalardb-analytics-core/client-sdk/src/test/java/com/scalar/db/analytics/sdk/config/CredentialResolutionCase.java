/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.sdk.config;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/** A row of the shared case table {@code credential-resolution-cases.tsv}. */
final class CredentialResolutionCase {
  static final String RESOURCE = "/credential-resolution-cases.tsv";

  final String name;
  final Map<ClientConfigKey, String> environment;
  final Map<ClientConfigKey, String> file;
  @Nullable final String expectedUsername;
  @Nullable final String expectedPassword;
  @Nullable final String missingRelativeKey;

  private CredentialResolutionCase(String[] cells) {
    name = cells[0];
    environment = values(cells[1], cells[2]);
    file = values(cells[3], cells[4]);
    expectedUsername = parse(cells[5]);
    expectedPassword = parse(cells[6]);
    missingRelativeKey = cells[7].equals("-") ? null : cells[7];
  }

  /** Returns the environment variables of this case, keyed by variable name. */
  Map<String, String> environmentVariables() {
    Map<String, String> vars = new HashMap<>();
    environment.forEach((key, value) -> vars.put(key.envVarName(), value));
    return vars;
  }

  /** Returns the file values of this case, keyed by {@link ClientConfigKey#propertyKey()}. */
  Map<String, String> fileProperties() {
    Map<String, String> props = new HashMap<>();
    file.forEach((key, value) -> props.put(key.propertyKey(), value));
    return props;
  }

  static List<CredentialResolutionCase> load() {
    InputStream in =
        Objects.requireNonNull(
            CredentialResolutionCase.class.getResourceAsStream(RESOURCE), RESOURCE);
    List<CredentialResolutionCase> cases = new ArrayList<>();
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
      boolean header = true;
      String line;
      while ((line = reader.readLine()) != null) {
        if (line.isEmpty() || line.startsWith("#")) {
          continue;
        }
        if (header) {
          header = false;
          continue;
        }
        cases.add(new CredentialResolutionCase(line.split("\t", -1)));
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    if (cases.isEmpty()) {
      throw new IllegalStateException(RESOURCE + " has no cases");
    }
    return cases;
  }

  private static Map<ClientConfigKey, String> values(String username, String password) {
    Map<ClientConfigKey, String> values = new HashMap<>();
    String u = parse(username);
    String p = parse(password);
    if (u != null) {
      values.put(ClientConfigKey.AUTH_USERNAME, u);
    }
    if (p != null) {
      values.put(ClientConfigKey.AUTH_PASSWORD, p);
    }
    return Collections.unmodifiableMap(values);
  }

  @Nullable
  private static String parse(String cell) {
    if (cell.equals("-")) {
      return null;
    }
    if (cell.length() < 2 || !cell.startsWith("\"") || !cell.endsWith("\"")) {
      throw new IllegalArgumentException("Invalid cell: " + cell);
    }
    return cell.substring(1, cell.length() - 1);
  }

  @Override
  public String toString() {
    return name;
  }
}
