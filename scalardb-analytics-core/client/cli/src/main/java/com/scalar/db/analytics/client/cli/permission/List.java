/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.client.cli.permission;

import com.scalar.db.analytics.api.authz.EffectivePermission;
import com.scalar.db.analytics.client.cli.CommandResult.CommandError;
import com.scalar.db.analytics.client.cli.CommandResult.Success;
import com.scalar.db.analytics.client.cli.Printer;
import com.scalar.db.analytics.client.module.Modules;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import org.jspecify.annotations.Nullable;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.ExitCode;
import picocli.CommandLine.ParentCommand;

@Command(name = "list", description = "List effective permissions for a user or a role")
public class List implements Callable<Integer> {

  // Permission name prefix and the matching grant subcommand.
  private static final String[][] RESOURCE_PREFIXES = {
    {"CATALOG_", "catalog"},
    {"DATA_SOURCE_", "data-source"},
    {"NAMESPACE_", "namespace"},
    {"TABLE_", "table"},
  };

  @ArgGroup(exclusive = true, multiplicity = "1")
  private GranteeOptions grantee;

  @ParentCommand
  @SuppressWarnings("NotNullFieldNotInitialized")
  private PermissionCommand parent;

  @Override
  public Integer call() {
    try {
      Modules modules = parent.root().loadModules();
      java.util.List<EffectivePermission> permissions =
          grantee.user != null
              ? modules.client().permission().listPermissions(grantee.user)
              : modules.client().permission().listPermissionsForRole(grantee.role);
      java.util.List<Map<String, String>> entries = new ArrayList<>(permissions.size());
      for (EffectivePermission permission : permissions) {
        entries.add(toEntry(permission));
      }
      printer().printSuccess(new Success(entries));
      return ExitCode.OK;
    } catch (Exception e) {
      printer().printError(new CommandError("Failed to list permissions", e));
      return ExitCode.SOFTWARE;
    }
  }

  private Printer printer() {
    return parent.root().printer();
  }

  /**
   * Converts a permission into the form that {@code permission grant} takes: the resource type as
   * the grant subcommand, the permission without its resource prefix, and the resource names under
   * the grant option names. If the server could not resolve the names, the resource ID is shown
   * instead.
   */
  static Map<String, String> toEntry(EffectivePermission permission) {
    Map<String, String> entry = new LinkedHashMap<>();
    String resource = permission.getPermission();
    String level = resource;
    for (String[] prefix : RESOURCE_PREFIXES) {
      if (permission.getPermission().startsWith(prefix[0])) {
        resource = prefix[1];
        level = permission.getPermission().substring(prefix[0].length());
        break;
      }
    }
    entry.put("resource", resource);
    entry.put("permission", level.toLowerCase(Locale.ROOT));
    String catalogName = permission.getCatalogName();
    if (catalogName != null) {
      entry.put("catalog", catalogName);
      putIfNotNull(entry, "data-source", permission.getDataSourceName());
      if (!permission.getNamespaceNames().isEmpty()) {
        entry.put("namespace", String.join(",", permission.getNamespaceNames()));
      }
      putIfNotNull(entry, "table", permission.getTableName());
    } else {
      entry.put("resource-id", permission.getResourceId());
    }
    if ("VIA_ROLE".equals(permission.getSource())) {
      entry.put("source", "role");
      putIfNotNull(entry, "role", permission.getViaRoleName());
    } else {
      entry.put("source", permission.getSource().toLowerCase(Locale.ROOT));
    }
    return entry;
  }

  private static void putIfNotNull(Map<String, String> entry, String key, @Nullable String value) {
    if (value != null) {
      entry.put(key, value);
    }
  }
}
