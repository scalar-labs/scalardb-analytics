/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.authz;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Value;
import org.jspecify.annotations.Nullable;

/**
 * An effective permission resolved for a user, indicating which permission applies on which
 * resource and whether it was granted directly or via a role.
 *
 * <p>The resource is identified both by its ID and by its names, from its catalog down to the
 * resource itself. Names below the level of the resource are null (or empty for the namespace
 * names), and all names are null if the resource could not be found.
 */
@Value
public class EffectivePermission {
  String permission;
  String resourceId;
  String source;
  @Nullable String viaRoleId;
  @Nullable String viaRoleName;
  @Nullable String catalogName;
  @Nullable String dataSourceName;
  List<String> namespaceNames;
  @Nullable String tableName;

  public EffectivePermission(
      String permission,
      String resourceId,
      String source,
      @Nullable String viaRoleId,
      @Nullable String viaRoleName,
      @Nullable String catalogName,
      @Nullable String dataSourceName,
      List<String> namespaceNames,
      @Nullable String tableName) {
    this.permission = permission;
    this.resourceId = resourceId;
    this.source = source;
    this.viaRoleId = viaRoleId;
    this.viaRoleName = viaRoleName;
    this.catalogName = catalogName;
    this.dataSourceName = dataSourceName;
    this.namespaceNames = Collections.unmodifiableList(new ArrayList<>(namespaceNames));
    this.tableName = tableName;
  }

  /** Creates an effective permission without resource names. */
  public EffectivePermission(
      String permission,
      String resourceId,
      String source,
      @Nullable String viaRoleId,
      @Nullable String viaRoleName) {
    this(
        permission,
        resourceId,
        source,
        viaRoleId,
        viaRoleName,
        null,
        null,
        Collections.emptyList(),
        null);
  }
}
