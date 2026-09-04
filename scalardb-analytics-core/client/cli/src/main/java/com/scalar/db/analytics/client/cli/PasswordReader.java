/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/** Reads a password from standard input for the {@code --password-stdin} option. */
public final class PasswordReader {

  private PasswordReader() {}

  /**
   * Reads the password from the first line of standard input. A trailing newline (if present) is
   * stripped. The remainder of stdin (if any) is ignored.
   *
   * <p>{@code System.in} is intentionally NOT closed: closing it disables any subsequent read from
   * stdin in the same process (e.g., picocli interactive prompts on other commands).
   *
   * @throws RuntimeException if reading fails or no data is provided
   */
  public static String readFromStdin() {
    BufferedReader reader =
        new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    try {
      String line = reader.readLine();
      if (line == null) {
        throw new RuntimeException("No password provided on standard input");
      }
      return line;
    } catch (IOException e) {
      throw new RuntimeException("Failed to read password from standard input", e);
    }
  }
}
