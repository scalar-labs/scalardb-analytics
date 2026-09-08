/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.codec;

import java.util.Arrays;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;

/**
 * Common arbitrary definitions for property-based tests. This class provides reusable arbitrary
 * generators for common data types used across codec tests.
 */
public final class CommonArbitraries {

  private CommonArbitraries() {
    // Utility class
  }

  /** Generates valid hostnames or IP addresses. */
  public static Arbitrary<String> host() {
    return Arbitraries.oneOf(
        // IP addresses
        Arbitraries.strings()
            .withCharRange('0', '9')
            .withChars('.')
            .ofMinLength(7)
            .ofMaxLength(15)
            .filter(s -> s.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}"))
            .filter(
                s ->
                    Arrays.stream(s.split("\\."))
                        .allMatch(
                            octet -> {
                              int value = Integer.parseInt(octet);
                              return value >= 0 && value <= 255;
                            })),
        // Hostnames
        Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars('-', '.')
            .ofMinLength(1)
            .ofMaxLength(255)
            .filter(
                s ->
                    !s.startsWith("-")
                        && !s.endsWith("-")
                        && !s.startsWith(".")
                        && !s.endsWith(".")));
  }

  /** Generates valid database names. Common pattern for MySQL, PostgreSQL, etc. */
  public static Arbitrary<String> databaseName() {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('_')
        .ofMinLength(1)
        .ofMaxLength(64)
        .filter(name -> !name.startsWith("_"));
  }

  /** Generates valid port numbers. */
  public static Arbitrary<Integer> port() {
    return Arbitraries.integers().between(1, 65535);
  }

  /** Generates valid usernames. */
  public static Arbitrary<String> username() {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('_', '-', '.')
        .ofMinLength(1)
        .ofMaxLength(100);
  }

  /** Generates passwords of varying complexity. */
  public static Arbitrary<String> password() {
    return Arbitraries.strings().ascii().ofMinLength(0).ofMaxLength(100);
  }

  /** Generates service names for Oracle. */
  public static Arbitrary<String> serviceName() {
    return Arbitraries.strings().alpha().numeric().withChars('_').ofMinLength(1).ofMaxLength(64);
  }

  /** Generates Snowflake account identifiers. */
  public static Arbitrary<String> snowflakeAccount() {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('-', '_')
        .ofMinLength(3)
        .ofMaxLength(50)
        .filter(s -> !s.startsWith("-") && !s.startsWith("_"));
  }

  /** Generates Databricks HTTP paths. */
  public static Arbitrary<String> databricksHttpPath() {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('/', '-', '_', '.')
        .ofMinLength(5)
        .ofMaxLength(200)
        .filter(s -> s.startsWith("/"));
  }

  /** Generates OAuth client IDs. */
  public static Arbitrary<String> oauthClientId() {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('-', '_')
        .ofMinLength(10)
        .ofMaxLength(100);
  }

  /** Generates OAuth secrets. */
  public static Arbitrary<String> oauthSecret() {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('-', '_', '+', '/', '=')
        .ofMinLength(20)
        .ofMaxLength(200);
  }

  /** Generates file paths. */
  public static Arbitrary<String> filePath() {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('/', '-', '_', '.')
        .ofMinLength(1)
        .ofMaxLength(255)
        .filter(s -> !s.isEmpty());
  }

  /** Generates catalog names. */
  public static Arbitrary<String> catalogName() {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('_')
        .ofMinLength(1)
        .ofMaxLength(100)
        .filter(name -> !name.startsWith("_") && !Character.isDigit(name.charAt(0)));
  }

  /** Generates AWS region names. */
  public static Arbitrary<String> awsRegion() {
    return Arbitraries.of(
        "us-east-1", "us-west-2", "eu-west-1", "ap-northeast-1", "ap-southeast-1", "sa-east-1");
  }

  /** Generates endpoint URLs. */
  public static Arbitrary<String> endpoint() {
    return host()
        .flatMap(
            h ->
                port()
                    .map(
                        p -> {
                          String protocol = Arbitraries.of("http", "https").sample();
                          return protocol + "://" + h + ":" + p;
                        }));
  }

  /** Alias for endpoint() - generates URLs. */
  public static Arbitrary<String> url() {
    return endpoint();
  }

  /** Generates namespace names. */
  public static Arbitrary<String> namespaceName() {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('_')
        .ofMinLength(1)
        .ofMaxLength(64)
        .filter(name -> !name.startsWith("_") && !Character.isDigit(name.charAt(0)));
  }

  /** Generates system identifiers with variable length. */
  public static Arbitrary<String> systemIdentifier(int maxLength) {
    return Arbitraries.strings()
        .alpha()
        .numeric()
        .withChars('_', '-')
        .ofMinLength(1)
        .ofMaxLength(maxLength)
        .filter(s -> !s.startsWith("-") && !s.startsWith("_"));
  }
}
