/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.constants;

/**
 * Constants for entity type names used in error mapping. This centralizes entity type names to
 * avoid string literals and reduce typos.
 */
public final class EntityTypes {
  public static final String CATALOG = "Catalog";
  public static final String DATA_SOURCE = "DataSource";
  public static final String NAMESPACE = "Namespace";
  public static final String TABLE = "Table";
  public static final String COLUMN = "Column";
  public static final String KEY = "Key";
  public static final String ACCESS_TOKEN = "AccessToken";
  public static final String USER = "User";
  public static final String PASSWORD_IDENTITY = "PasswordIdentity";
  public static final String INTERNAL_CREDENTIAL = "InternalCredential";
  public static final String ROLE = "Role";
  public static final String ROLE_ASSIGNMENT = "RoleAssignment";
  public static final String ACCESS_CONTROL_ENTRY = "AccessControlEntry";
  public static final String RESOURCE_TYPE = "ResourceType";
  public static final String PERMISSION = "Permission";

  private EntityTypes() {
    // Prevent instantiation
  }
}
