/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.tools.errorcodedoc;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Command-line entry point for {@link ErrorCodeDocGenerator}.
 *
 * <p>Writes the generated MDX document to the file given as the first argument, or to standard
 * output when no argument is given.
 */
public final class ErrorCodeDocGeneratorMain {

  private ErrorCodeDocGeneratorMain() {}

  public static void main(String[] args) throws IOException {
    String document = ErrorCodeDocGenerator.generate();
    if (args.length == 0) {
      System.out.print(document);
      return;
    }
    Path output = Paths.get(args[0]);
    if (output.getParent() != null) {
      Files.createDirectories(output.getParent());
    }
    Files.write(output, document.getBytes(StandardCharsets.UTF_8));
  }
}
