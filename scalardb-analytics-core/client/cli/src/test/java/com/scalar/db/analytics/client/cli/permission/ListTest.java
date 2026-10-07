/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.permission;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.scalar.db.analytics.api.authz.EffectivePermission;
import com.scalar.db.analytics.client.cli.CliTestBase;
import com.scalar.db.analytics.sdk.authz.PermissionClient;
import java.util.Arrays;
import java.util.Collections;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import picocli.CommandLine.ExitCode;

class ListTest extends CliTestBase {

  @Test
  void givenNoOptions_thenFails() {
    int exitCode = new CommandLine(cliRoot).execute(args("permission", "list"));
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    verifyNoInteractions(modules.client().permission());
  }

  @Test
  void givenUser_thenCallListPermissions() {
    PermissionClient mock = modules.client().permission();
    when(mock.listPermissions("alice"))
        .thenReturn(
            Arrays.asList(
                catalogPermission("CATALOG_READ", "DIRECT", null),
                tablePermission("TABLE_READ", "VIA_ROLE", "admin")));

    int exitCode = new CommandLine(cliRoot).execute(args("permission", "list", "--user", "alice"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput())
        .contains(
            "{\"resource\":\"catalog\",\"permission\":\"read\",\"catalog\":\"test_catalog\","
                + "\"source\":\"direct\"}")
        .contains(
            "{\"resource\":\"table\",\"permission\":\"read\",\"catalog\":\"test_catalog\","
                + "\"data-source\":\"ds\",\"namespace\":\"db,public\",\"table\":\"orders\","
                + "\"source\":\"role\",\"role\":\"admin\"}");
    verify(mock).listPermissions("alice");
  }

  @Test
  void toEntry_dataSourcePermission_shouldUseGrantSubcommandAndPermissionLevel() {
    EffectivePermission permission =
        new EffectivePermission(
            "DATA_SOURCE_ADMIN",
            "res-3",
            "DIRECT",
            null,
            null,
            "test_catalog",
            "ds",
            Collections.emptyList(),
            null);

    assertThat(List.toEntry(permission))
        .containsExactly(
            entry("resource", "data-source"),
            entry("permission", "admin"),
            entry("catalog", "test_catalog"),
            entry("data-source", "ds"),
            entry("source", "direct"));
  }

  @Test
  void toEntry_namespacePermission_shouldJoinNamespaceNamesLikeGrantInput() {
    EffectivePermission permission =
        new EffectivePermission(
            "NAMESPACE_READ",
            "res-4",
            "DIRECT",
            null,
            null,
            "test_catalog",
            "ds",
            Arrays.asList("db", "public"),
            null);

    assertThat(List.toEntry(permission))
        .containsEntry("resource", "namespace")
        .containsEntry("namespace", "db,public")
        .doesNotContainKey("table");
  }

  @Test
  void toEntry_whenNamesNotResolved_shouldShowResourceId() {
    EffectivePermission permission =
        new EffectivePermission("CATALOG_WRITE", "res-1", "DIRECT", null, null);

    assertThat(List.toEntry(permission))
        .containsExactly(
            entry("resource", "catalog"),
            entry("permission", "write"),
            entry("resource-id", "res-1"),
            entry("source", "direct"));
  }

  private static EffectivePermission catalogPermission(
      String permission, String source, @Nullable String roleName) {
    return new EffectivePermission(
        permission,
        "res-1",
        source,
        roleName == null ? null : "r-1",
        roleName,
        "test_catalog",
        null,
        Collections.emptyList(),
        null);
  }

  private static EffectivePermission tablePermission(
      String permission, String source, @Nullable String roleName) {
    return new EffectivePermission(
        permission,
        "res-2",
        source,
        roleName == null ? null : "r-1",
        roleName,
        "test_catalog",
        "ds",
        Arrays.asList("db", "public"),
        "orders");
  }

  @Test
  void givenNoPermissions_thenReturnsEmptyList() {
    PermissionClient mock = modules.client().permission();
    when(mock.listPermissions("bob")).thenReturn(Collections.emptyList());

    int exitCode = new CommandLine(cliRoot).execute(args("permission", "list", "-u", "bob"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    verify(mock).listPermissions("bob");
  }

  @Test
  void givenRole_thenCallListPermissionsForRole() {
    PermissionClient mock = modules.client().permission();
    when(mock.listPermissionsForRole("admin"))
        .thenReturn(
            Arrays.asList(
                catalogPermission("CATALOG_READ", "DIRECT", null),
                tablePermission("TABLE_READ", "DIRECT", null)));

    int exitCode = new CommandLine(cliRoot).execute(args("permission", "list", "--role", "admin"));

    assertThat(exitCode).isEqualTo(ExitCode.OK);
    assertThat(printer.getOutput())
        .contains("\"resource\":\"catalog\",\"permission\":\"read\"")
        .contains("\"resource\":\"table\",\"permission\":\"read\"");
    verify(mock).listPermissionsForRole("admin");
  }

  @Test
  void givenBothUserAndRole_thenFails() {
    int exitCode =
        new CommandLine(cliRoot)
            .execute(args("permission", "list", "--user", "alice", "--role", "admin"));

    // --user / --role form a mutually exclusive (exclusive=true) ArgGroup, so supplying both is a
    // usage error and the command must not reach the client.
    assertThat(exitCode).isEqualTo(ExitCode.USAGE);
    verifyNoInteractions(modules.client().permission());
  }
}
