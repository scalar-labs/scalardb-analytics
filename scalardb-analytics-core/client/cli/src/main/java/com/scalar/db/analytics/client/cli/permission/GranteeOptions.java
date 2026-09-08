/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.permission;

import picocli.CommandLine.Option;

/** Mutually exclusive grantee options: exactly one of {@code --user} or {@code --role}. */
public class GranteeOptions {
  @Option(
      names = {"-u", "--user"},
      description = "Username")
  String user;

  @Option(
      names = {"-r", "--role"},
      description = "Role name")
  String role;

  String granteeType() {
    return user != null ? "USER" : "ROLE";
  }

  String granteeName() {
    return user != null ? user : role;
  }
}
