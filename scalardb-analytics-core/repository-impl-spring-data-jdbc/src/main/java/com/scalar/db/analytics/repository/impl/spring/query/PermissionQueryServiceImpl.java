/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.repository.impl.spring.query;

import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.ACCESS_CONTROL_ENTRY;
import static com.scalar.db.analytics.repository.impl.spring.constants.EntityTypes.ROLE_ASSIGNMENT;

import com.scalar.db.analytics.api.error.AnalyticsErrorCode;
import com.scalar.db.analytics.api.error.AnalyticsException;
import com.scalar.db.analytics.domain.authz.AccessControlEntry;
import com.scalar.db.analytics.domain.authz.GranteeType;
import com.scalar.db.analytics.domain.authz.Permission;
import com.scalar.db.analytics.domain.authz.ResourceNames;
import com.scalar.db.analytics.domain.authz.ResourceType;
import com.scalar.db.analytics.domain.authz.Role;
import com.scalar.db.analytics.repository.PermissionQueryService;
import com.scalar.db.analytics.repository.authz.AccessControlEntryRepository;
import com.scalar.db.analytics.repository.impl.spring.exception.SpringDataJdbcErrorMapper;
import com.scalar.db.analytics.repository.impl.spring.query.util.NameParsingUtility;
import com.scalar.db.analytics.repository.impl.spring.query.util.RowMapperThrowables;
import com.scalar.db.analytics.repository.impl.spring.transaction.SpringDataJdbcTransactionContext;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Resolves the resource names of a grantee's permissions with one join query per resource type,
 * instead of looking up each resource and its parents one by one.
 *
 * <p>The queries satisfy the ScalarDB SQL join restrictions: every join predicate covers the
 * primary key of the right table, and the WHERE clause references only the FROM table. Because the
 * joins are inner joins, each query returns only the entries on resources of its type that still
 * exist. The grantee's entries themselves are read separately so that an entry whose resource can
 * no longer be found is still returned, in the order in which the entries are stored.
 */
@Component
@Transactional(readOnly = true)
public class PermissionQueryServiceImpl
    implements PermissionQueryService<SpringDataJdbcTransactionContext> {

  private static final String CATALOG_NAMES_QUERY =
      "SELECT ace.resource_id AS resource_id, c.name AS catalog_name "
          + "FROM authz_access_control_entries AS ace "
          + "JOIN catalogs AS c ON c.catalog_id = ace.resource_id"
          + granteeFilter(ResourceType.CATALOG);

  private static final String DATA_SOURCE_NAMES_QUERY =
      "SELECT ace.resource_id AS resource_id, c.name AS catalog_name, "
          + "d.name AS data_source_name "
          + "FROM authz_access_control_entries AS ace "
          + "JOIN registry_data_sources AS d ON d.data_source_id = ace.resource_id "
          + "JOIN catalogs AS c ON c.catalog_id = d.catalog_id"
          + granteeFilter(ResourceType.DATA_SOURCE);

  private static final String NAMESPACE_NAMES_QUERY =
      "SELECT ace.resource_id AS resource_id, c.name AS catalog_name, "
          + "d.name AS data_source_name, n.names AS namespace_names "
          + "FROM authz_access_control_entries AS ace "
          + "JOIN registry_namespaces AS n ON n.namespace_id = ace.resource_id "
          + "JOIN registry_data_sources AS d ON d.data_source_id = n.data_source_id "
          + "JOIN catalogs AS c ON c.catalog_id = d.catalog_id"
          + granteeFilter(ResourceType.NAMESPACE);

  private static final String TABLE_NAMES_QUERY =
      "SELECT ace.resource_id AS resource_id, c.name AS catalog_name, "
          + "d.name AS data_source_name, n.names AS namespace_names, t.name AS table_name "
          + "FROM authz_access_control_entries AS ace "
          + "JOIN registry_tables AS t ON t.table_id = ace.resource_id "
          + "JOIN registry_namespaces AS n ON n.namespace_id = t.namespace_id "
          + "JOIN registry_data_sources AS d ON d.data_source_id = n.data_source_id "
          + "JOIN catalogs AS c ON c.catalog_id = d.catalog_id"
          + granteeFilter(ResourceType.TABLE);

  // A left join, so that an assignment of a role that can no longer be found is reported as an
  // inconsistency instead of being silently dropped.
  private static final String ROLES_QUERY =
      "SELECT ra.role_id AS role_id, r.name AS role_name, r.built_in AS built_in "
          + "FROM authz_role_assignments AS ra "
          + "LEFT JOIN authz_roles AS r ON r.id = ra.role_id "
          + "WHERE ra.user_id = :userId";

  private final NamedParameterJdbcOperations jdbcOperations;
  private final NameParsingUtility nameParsingUtility;
  private final AccessControlEntryRepository<SpringDataJdbcTransactionContext> aceRepository;
  private final SpringDataJdbcErrorMapper errorMapper;

  public PermissionQueryServiceImpl(
      NamedParameterJdbcOperations jdbcOperations,
      NameParsingUtility nameParsingUtility,
      AccessControlEntryRepository<SpringDataJdbcTransactionContext> aceRepository,
      SpringDataJdbcErrorMapper errorMapper) {
    this.jdbcOperations = jdbcOperations;
    this.nameParsingUtility = nameParsingUtility;
    this.aceRepository = aceRepository;
    this.errorMapper = errorMapper;
  }

  @Override
  public List<GrantedPermission> listByGrantee(
      SpringDataJdbcTransactionContext ctx, GranteeType granteeType, UUID granteeId) {
    List<AccessControlEntry> entries = aceRepository.findByGrantee(ctx, granteeType, granteeId);
    if (entries.isEmpty()) {
      return List.of();
    }
    MapSqlParameterSource params =
        new MapSqlParameterSource()
            .addValue("granteeId", granteeId.toString())
            .addValue("granteeType", granteeType.name());
    Set<ResourceType> resourceTypes = EnumSet.noneOf(ResourceType.class);
    entries.forEach(entry -> resourceTypes.add(entry.permission().resourceType()));
    Map<ResourceType, Map<UUID, ResourceNames>> namesByType = new EnumMap<>(ResourceType.class);
    try {
      // Only the resource types that the grantee has permissions on are queried.
      for (ResourceType resourceType : resourceTypes) {
        namesByType.put(resourceType, queryResourceNames(resourceType, params));
      }
    } catch (AnalyticsException e) {
      throw e;
    } catch (Exception e) {
      throw errorMapper.mapException(e, ACCESS_CONTROL_ENTRY, granteeId.toString());
    }

    List<GrantedPermission> result = new ArrayList<>(entries.size());
    for (AccessControlEntry entry : entries) {
      Map<UUID, ResourceNames> names =
          namesByType.getOrDefault(entry.permission().resourceType(), Map.of());
      result.add(
          new GrantedPermission(
              entry.permission(), entry.resourceId(), names.get(entry.resourceId())));
    }
    return result;
  }

  @Override
  public List<Role> listRolesByUserId(SpringDataJdbcTransactionContext ctx, UUID userId) {
    try {
      return jdbcOperations.query(
          ROLES_QUERY,
          new MapSqlParameterSource("userId", userId.toString()),
          RowMapperThrowables.throwableRowMapper(this::mapRole));
    } catch (AnalyticsException e) {
      throw e;
    } catch (Exception e) {
      throw errorMapper.mapException(e, ROLE_ASSIGNMENT, userId.toString());
    }
  }

  /**
   * Returns the WHERE clause of a resource names query: the grantee's entries whose permission is
   * on the resource type.
   *
   * <p>ScalarDB SQL runs a join as one lookup in the right table per row of the left table, so
   * without the permission filter each of the four queries would look up every entry of the
   * grantee. ScalarDB SQL has no IN predicate, so the permissions are matched with OR.
   */
  private static String granteeFilter(ResourceType resourceType) {
    return " WHERE ace.grantee_id = :granteeId AND ace.grantee_type = :granteeType AND ("
        + Arrays.stream(Permission.values())
            .filter(permission -> permission.resourceType() == resourceType)
            .map(permission -> "ace.permission_id = '" + permission.id() + "'")
            .collect(Collectors.joining(" OR "))
        + ")";
  }

  /**
   * Runs the resource names query of a resource type for the grantee and returns the names by
   * resource ID.
   */
  private Map<UUID, ResourceNames> queryResourceNames(
      ResourceType resourceType, MapSqlParameterSource params) {
    Map<UUID, ResourceNames> names = new HashMap<>();
    RowCallbackHandler handler =
        switch (resourceType) {
          case CATALOG ->
              rs ->
                  names.put(
                      parseUuid(rs, "resource_id"),
                      new ResourceNames(rs.getString("catalog_name"), null, List.of(), null));
          case DATA_SOURCE ->
              rs ->
                  names.put(
                      parseUuid(rs, "resource_id"),
                      new ResourceNames(
                          rs.getString("catalog_name"),
                          rs.getString("data_source_name"),
                          List.of(),
                          null));
          case NAMESPACE ->
              rs ->
                  names.put(
                      parseUuid(rs, "resource_id"),
                      new ResourceNames(
                          rs.getString("catalog_name"),
                          rs.getString("data_source_name"),
                          parseNamespaceNames(rs),
                          null));
          case TABLE ->
              rs ->
                  names.put(
                      parseUuid(rs, "resource_id"),
                      new ResourceNames(
                          rs.getString("catalog_name"),
                          rs.getString("data_source_name"),
                          parseNamespaceNames(rs),
                          rs.getString("table_name")));
        };
    String sql =
        switch (resourceType) {
          case CATALOG -> CATALOG_NAMES_QUERY;
          case DATA_SOURCE -> DATA_SOURCE_NAMES_QUERY;
          case NAMESPACE -> NAMESPACE_NAMES_QUERY;
          case TABLE -> TABLE_NAMES_QUERY;
        };
    jdbcOperations.query(sql, params, handler);
    return names;
  }

  private List<String> parseNamespaceNames(ResultSet rs) throws SQLException {
    try {
      return nameParsingUtility.parseNamesFromColumn(rs, "namespace_names");
    } catch (IOException e) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of("field_name", "names", "entity_name", parseUuid(rs, "resource_id").toString()),
          e);
    }
  }

  private Role mapRole(ResultSet rs) throws SQLException {
    UUID roleId = parseUuid(rs, "role_id");
    String name = rs.getString("role_name");
    if (name == null) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of("field_name", "role_id", "entity_name", roleId.toString()));
    }
    return new Role(roleId, name, rs.getBoolean("built_in"));
  }

  private static UUID parseUuid(ResultSet rs, String column) throws SQLException {
    String raw = rs.getString(column);
    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException e) {
      throw new AnalyticsException(
          AnalyticsErrorCode.DATA_INCONSISTENCY,
          Map.of("field_name", column, "entity_name", raw),
          e);
    }
  }
}
