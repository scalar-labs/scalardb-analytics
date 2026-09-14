/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.grpc.mapper;

import java.util.Arrays;
import java.util.UUID;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.Arbitrary;
import net.jqwik.api.arbitraries.StringArbitrary;

/**
 * Common arbitrary definitions for property-based tests. This class provides reusable arbitrary
 * generators for common data types used across multiple mapper tests.
 */
public final class CommonArbitraries {

  private CommonArbitraries() {
    // Utility class
  }

  /** Generates valid UUID instances. */
  public static Arbitrary<UUID> uuid() {
    return Arbitraries.create(UUID::randomUUID);
  }

  /** Generates valid UUID strings in the standard format. */
  public static Arbitrary<String> uuidString() {
    return uuid().map(UUID::toString);
  }

  /** Generates invalid UUID strings for error testing. */
  public static Arbitrary<String> invalidUuidString() {
    return Arbitraries.oneOf(
        // Invalid format - missing hyphens
        Arbitraries.strings().withCharRange('0', '9').withCharRange('a', 'f').ofLength(32),
        // Wrong hyphen positions
        Arbitraries.strings()
            .withCharRange('0', '9')
            .withCharRange('a', 'f')
            .ofLength(32)
            .map(str -> str.substring(0, 10) + "-" + str.substring(10)),
        // Invalid characters
        Arbitraries.strings()
            .alpha()
            .numeric()
            .ofMinLength(10)
            .ofMaxLength(50)
            .filter(s -> !s.matches("[0-9a-fA-F-]+")));
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

  /** Generates valid catalog names. */
  public static Arbitrary<String> catalogName() {
    return identifier(100);
  }

  /** Generates valid table names. */
  public static Arbitrary<String> tableName() {
    return identifier(64);
  }

  /** Generates valid column names. */
  public static Arbitrary<String> columnName() {
    return identifier(64);
  }

  /** Generates valid namespace/schema names. */
  public static Arbitrary<String> namespaceName() {
    return identifier(64);
  }

  /**
   * Generates valid SQL identifiers with specified max length. Common pattern for database object
   * names.
   */
  public static Arbitrary<String> identifier(int maxLength) {
    StringArbitrary base =
        Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars('_')
            .ofMinLength(1)
            .ofMaxLength(maxLength);

    return base.filter(name -> !name.startsWith("_") && !Character.isDigit(name.charAt(0)));
  }

  /**
   * Generates alphanumeric identifiers with hyphens and underscores. Common pattern for system
   * identifiers.
   */
  public static Arbitrary<String> systemIdentifier(int maxLength) {
    StringArbitrary base =
        Arbitraries.strings()
            .alpha()
            .numeric()
            .withChars('-', '_')
            .ofMinLength(1)
            .ofMaxLength(maxLength);

    return base.filter(name -> !name.startsWith("-") && !name.startsWith("_"));
  }

  /** Generates arbitrary bytes for testing binary data. */
  public static Arbitrary<byte[]> bytes() {
    return Arbitraries.bytes().array(byte[].class).ofMinSize(0).ofMaxSize(1024);
  }

  /** Generates arbitrary URLs. */
  public static Arbitrary<String> url() {
    return Arbitraries.oneOf(
        // HTTP URLs
        host().map(h -> "http://" + h),
        host().map(h -> "https://" + h),
        // With ports
        host()
            .flatMap(
                h ->
                    Arbitraries.integers()
                        .between(1, 65535)
                        .map(port -> "http://" + h + ":" + port)),
        host()
            .flatMap(
                h ->
                    Arbitraries.integers()
                        .between(1, 65535)
                        .map(port -> "https://" + h + ":" + port)));
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
}
