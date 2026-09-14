/*
 * Copyright Scalar, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.scalar.db.analytics.api.error;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Error codes for ScalarDB Analytics following the {@code DB-ANALYTICS-NNNNN} convention.
 *
 * <p>Categories:
 *
 * <ul>
 *   <li>{@code 1xxxx} — User errors (invalid input, permission denied, authentication)
 *   <li>{@code 3xxxx} — Internal/operational errors (database, connection, data corruption)
 *   <li>{@code 4xxxx} — Client-side errors (transport failures detected before a structured server
 *       error was received)
 * </ul>
 *
 * <p>Error codes are stable: once assigned, a code never changes its meaning.
 *
 * <h3>Colocated description</h3>
 *
 * <p>Each constant carries an {@link ErrorDescription} (message + cause + action) so that the
 * user-facing wording lives next to the code. Throw sites pass only {@code (code, metadata)};
 * caller-written messages are not part of the API. The text content of {@code ErrorDescription} is
 * not part of the API contract — see {@link ErrorDescription} and ADR-0010.
 *
 * <h3>Design Principle: Metadata is for context, not control</h3>
 *
 * <p>SDK control flow must be driven exclusively by {@link #getErrorCategory()}, {@link
 * #requiresReauthentication()}, and the error code itself — never by inspecting metadata keys or
 * values. Metadata provides structured contextual information (entity names, IDs, etc.) for
 * logging, diagnostics, and user-facing messages.
 */
public enum AnalyticsErrorCode {

  // 100xx - Authentication & Authorization

  /**
   * Authentication failed (invalid credentials).
   *
   * <p>Expected metadata keys: none (detail in message).
   */
  AUTHENTICATION_FAILED(
      "DB-ANALYTICS-10000",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Authentication failed",
          "The provided credentials are invalid or the user does not exist.",
          "Verify the credentials, then retry. Contact your administrator if you cannot recover the credentials.")),

  /**
   * Token has expired.
   *
   * <p>Expected metadata keys: none.
   */
  TOKEN_EXPIRED(
      "DB-ANALYTICS-10001",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Authentication token has expired",
          "The access token issued during authentication has reached its expiry time.",
          "Re-authenticate to obtain a new token, then retry the request.")),

  /**
   * Token is invalid.
   *
   * <p>Expected metadata keys: none.
   */
  TOKEN_INVALID(
      "DB-ANALYTICS-10002",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Authentication token is invalid",
          "The token is malformed, was revoked, or was not issued by this server.",
          "Authenticate again with valid credentials to obtain a new token, then retry. Contact your administrator if the token continues to be rejected.")),

  /**
   * Insufficient permissions.
   *
   * <p>Expected metadata keys: {@code user_id}, {@code catalog_name}, {@code data_source_name},
   * {@code namespace_name}, {@code table_name} (as applicable).
   */
  ACCESS_DENIED(
      "DB-ANALYTICS-10003",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Access denied",
          "The authenticated user lacks the required role or permission for the requested action on the resource.",
          "Ask your administrator to grant the required role (e.g. SUPERADMIN, CATALOG_ADMIN) or the specific permission on the resource, then retry.")),

  // 101xx - Entity Already Exists

  /**
   * Catalog already exists.
   *
   * <p>Expected metadata keys: {@code catalog_name}.
   */
  CATALOG_ALREADY_EXISTS(
      "DB-ANALYTICS-10100",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Catalog already exists",
          "A catalog with the given name has already been created in this Analytics instance.",
          "Choose a different catalog name, or use the existing catalog instead of creating a new one.")),

  /**
   * Data source already exists.
   *
   * <p>Expected metadata keys: {@code catalog_name}, {@code data_source_name}.
   */
  DATA_SOURCE_ALREADY_EXISTS(
      "DB-ANALYTICS-10101",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Data source already exists",
          "A data source with the given name has already been registered in the catalog.",
          "Choose a different data source name, or use the existing data source instead of registering a new one.")),

  /**
   * Namespace already exists.
   *
   * <p>Expected metadata keys: {@code catalog_name}, {@code data_source_name}, {@code
   * namespace_name}.
   */
  NAMESPACE_ALREADY_EXISTS(
      "DB-ANALYTICS-10102",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Namespace already exists",
          "A namespace with the given name already exists in the catalog for the data source.",
          "The namespace is already registered in the catalog for this data source. Use the existing registration, or refresh it if the registration is outdated.")),

  /**
   * Table already exists.
   *
   * <p>Expected metadata keys: {@code catalog_name}, {@code data_source_name}, {@code
   * namespace_name}, {@code table_name}.
   */
  TABLE_ALREADY_EXISTS(
      "DB-ANALYTICS-10103",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Table already exists",
          "A table with the given name already exists in the namespace.",
          "The table is already registered in the catalog for this namespace. Use the existing registration, or refresh it if the registration is outdated.")),

  /**
   * Role already exists.
   *
   * <p>Expected metadata keys: {@code role_name}.
   */
  ROLE_ALREADY_EXISTS(
      "DB-ANALYTICS-10104",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Role already exists",
          "A role with the given name has already been created.",
          "Choose a different role name, or use the existing role instead of creating a new one.")),

  /**
   * User already exists.
   *
   * <p>Expected metadata keys: {@code username} (when registering through the internal user
   * directory, identified by name) or {@code user_id} (when the user is identified by UUID).
   */
  USER_ALREADY_EXISTS(
      "DB-ANALYTICS-10105",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "User already exists",
          "A user with the given user id has already been registered.",
          "Choose a different user id, or use the existing user instead of registering a new one.")),

  /**
   * Role already assigned.
   *
   * <p>Expected metadata keys: {@code user_id}, and {@code username} + {@code role_name} (when the
   * role was assigned by name) or {@code role_id} (when assigned by id).
   */
  ROLE_ALREADY_ASSIGNED(
      "DB-ANALYTICS-10106",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Role already assigned to user",
          "The role is already assigned to the user.",
          "No action is required if the assignment is intended. Otherwise, choose a different role.")),

  /**
   * Permission already granted.
   *
   * <p>Expected metadata keys: {@code role_name}, {@code permission}, {@code resource}.
   */
  PERMISSION_ALREADY_GRANTED(
      "DB-ANALYTICS-10107",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Permission already granted to role",
          "The permission is already granted to the role on the resource.",
          "No action is required if the grant is intended. Otherwise, choose a different permission or resource.")),

  // 102xx - Operation Precondition

  /**
   * Catalog is not empty.
   *
   * <p>Expected metadata keys: {@code catalog_name}.
   */
  CATALOG_NOT_EMPTY(
      "DB-ANALYTICS-10200",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Catalog is not empty",
          "The catalog still contains data sources or other dependent entities.",
          "Delete the data sources within the catalog before deleting the catalog, or use a cascading delete if supported.")),

  /**
   * Data source is not empty.
   *
   * <p>Expected metadata keys: {@code catalog_name}, {@code data_source_name}.
   */
  DATA_SOURCE_NOT_EMPTY(
      "DB-ANALYTICS-10201",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Data source is not empty",
          "The data source still contains namespaces or tables.",
          "Delete the namespaces and tables within the data source before deleting the data source.")),

  /**
   * Cannot modify built-in entity.
   *
   * <p>Expected metadata keys: {@code entity_name}.
   */
  BUILT_IN_ENTITY_NOT_MODIFIABLE(
      "DB-ANALYTICS-10202",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Built-in entity cannot be modified",
          "The target is a built-in (system-managed) entity that cannot be modified or deleted.",
          "Do not modify the built-in entity. Create a new custom entity instead if you need different behavior.")),

  /**
   * User is not empty.
   *
   * <p>Expected metadata keys: {@code username} (when the user is identified by name) or {@code
   * user_id} (when identified by UUID).
   */
  USER_NOT_EMPTY(
      "DB-ANALYTICS-10203",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "User is not empty",
          "The user still has linked backend users, role assignments, or direct permission grants.",
          "Remove the linked backend users, role assignments, and permission grants before deleting the user, or use a cascading delete.")),

  // 103xx - Data Source Connection

  /**
   * Data source authentication failed.
   *
   * <p>Expected metadata keys: {@code catalog_name}, {@code data_source_name}.
   */
  DATA_SOURCE_AUTHENTICATION_FAILED(
      "DB-ANALYTICS-10300",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Data source authentication failed",
          "The credentials configured for the data source were rejected by the underlying database.",
          "Update the data source credentials with valid values, then retry.")),

  /**
   * Data source is unreachable.
   *
   * <p>Expected metadata keys: {@code catalog_name}, {@code data_source_name}.
   */
  DATA_SOURCE_UNREACHABLE(
      "DB-ANALYTICS-10301",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Data source is unreachable",
          "Analytics could not establish a network connection to the data source.",
          "Verify the data source endpoint, network connectivity, and that the underlying database is running, then retry.")),

  // 104xx - Input Validation

  /**
   * Invalid argument or validation failure.
   *
   * <p>Expected metadata keys: context-dependent.
   */
  INVALID_ARGUMENT(
      "DB-ANALYTICS-10400",
      ErrorCategory.USER_ERROR,
      new ErrorDescription(
          "Invalid argument",
          "One or more arguments do not satisfy the validation rules (format, range, or required field).",
          "Inspect the metadata for the offending field and provide a valid value, then retry.")),

  // 300xx - Retryable Internal Errors

  /**
   * Analytics database connection failed.
   *
   * <p>Expected metadata keys: none (detail in message or cause).
   */
  ANALYTICS_DB_CONNECTION_FAILED(
      "DB-ANALYTICS-30000",
      ErrorCategory.RETRYABLE_SERVER_ERROR,
      new ErrorDescription(
          "Analytics database connection failed",
          "Analytics could not connect to its internal database (e.g. catalog metadata store).",
          "Retry the request. If the failure persists, check the Analytics database health and contact your administrator.")),

  /**
   * ScalarDB Cluster is unreachable.
   *
   * <p>Expected metadata keys: none (detail in message or cause).
   */
  SCALARDB_CLUSTER_UNAVAILABLE(
      "DB-ANALYTICS-30002",
      ErrorCategory.RETRYABLE_SERVER_ERROR,
      new ErrorDescription(
          "ScalarDB Cluster is unavailable",
          "Analytics could not reach the ScalarDB Cluster (network failure, deadline exceeded, or the cluster is down).",
          "Retry the request. If the failure persists, check the ScalarDB Cluster health and contact your administrator.")),

  // 301xx - Non-retryable Internal Errors

  /**
   * Persisted data does not match expected schema or format.
   *
   * <p>Expected metadata keys: context-dependent.
   */
  DATA_INCONSISTENCY(
      "DB-ANALYTICS-30100",
      ErrorCategory.NON_RETRYABLE_SERVER_ERROR,
      new ErrorDescription(
          "Persisted data is inconsistent with the expected schema",
          "Analytics encountered persisted data that does not match the expected schema or format.",
          "This indicates an internal data integrity issue. Contact your administrator with the error details.")),

  /**
   * ScalarDB Cluster backend token expired.
   *
   * <p>Expected metadata keys: none.
   */
  SCALARDB_BACKEND_TOKEN_EXPIRED(
      "DB-ANALYTICS-30101",
      ErrorCategory.NON_RETRYABLE_SERVER_ERROR,
      new ErrorDescription(
          "ScalarDB Cluster backend token has expired",
          "The token Analytics uses to authenticate to the ScalarDB Cluster has expired.",
          "Re-authenticate to refresh the backend token. The SDK normally handles this automatically.")),

  /**
   * ScalarDB privilege check failed.
   *
   * <p>Expected metadata keys: none (detail in message or cause).
   */
  SCALARDB_PRIVILEGE_CHECK_FAILURE(
      "DB-ANALYTICS-30102",
      ErrorCategory.NON_RETRYABLE_SERVER_ERROR,
      new ErrorDescription(
          "ScalarDB privilege check failed",
          "Analytics could not complete the privilege check against the ScalarDB Cluster.",
          "Verify that the ScalarDB Cluster is reachable and configured correctly. Contact your administrator if the failure persists.")),

  /**
   * Unexpected internal error.
   *
   * <p>Expected metadata keys: none.
   */
  INTERNAL_ERROR(
      "DB-ANALYTICS-30103",
      ErrorCategory.NON_RETRYABLE_SERVER_ERROR,
      new ErrorDescription(
          "Internal error",
          "Analytics encountered an unexpected internal error.",
          "Contact your administrator with the error details so the unexpected failure can be investigated.")),

  /**
   * Analytics database operation failed.
   *
   * <p>Expected metadata keys: {@code sql_state} (when available).
   */
  ANALYTICS_DB_OPERATION_FAILED(
      "DB-ANALYTICS-30104",
      ErrorCategory.NON_RETRYABLE_SERVER_ERROR,
      new ErrorDescription(
          "Analytics database operation failed",
          "An operation on the Analytics internal database failed (e.g. a constraint violation or an unexpected query/write error). The operation is not known to be safe to retry.",
          "Inspect the error details. Contact your administrator if the failure persists.")),

  // 400xx - Client-side Errors
  //
  // Detected on the client side before a structured server error (AnalyticsError) was received. The
  // axis is *where* the failure was terminated, not blame. These codes are produced only by the SDK
  // (e.g. when mapping a detail-less gRPC StatusRuntimeException); the server never emits them.

  /**
   * The server could not be reached.
   *
   * <p>Expected metadata keys: none (detail in message or cause).
   */
  SERVER_UNREACHABLE(
      "DB-ANALYTICS-40000",
      ErrorCategory.CLIENT_ERROR,
      new ErrorDescription(
          "The Analytics server could not be reached",
          "The request did not reach the server, or the server returned no structured error (e.g. network failure or the server is down).",
          "Verify the server endpoint and network connectivity, then retry. Contact your administrator if the failure persists.")),

  /**
   * The request timed out.
   *
   * <p>Expected metadata keys: none (detail in message or cause).
   */
  REQUEST_TIMEOUT(
      "DB-ANALYTICS-40001",
      ErrorCategory.CLIENT_ERROR,
      new ErrorDescription(
          "The request to the Analytics server timed out",
          "The request did not complete before its deadline expired.",
          "Retry the request. If timeouts persist, check server health and network latency, or increase the request deadline.")),

  /**
   * Unexpected client-side error.
   *
   * <p>Expected metadata keys: none (detail in message or cause).
   */
  CLIENT_INTERNAL_ERROR(
      "DB-ANALYTICS-40002",
      ErrorCategory.CLIENT_ERROR,
      new ErrorDescription(
          "Unexpected client-side error",
          "The SDK encountered an unexpected error while issuing the request or processing the response, with no structured server error available.",
          "Retry the request. Contact your administrator with the error details if the failure persists.")),

  /**
   * The client received a structured server error whose code it does not recognize.
   *
   * <p>This typically means the server is newer than this SDK and returned an error code that is
   * not present in this version's enum. The failure is surfaced conservatively (a non-retryable
   * {@code CLIENT_ERROR}, not a user error, no re-authentication) because the original category is
   * unknown; the original server code is preserved under the {@code server_error_code} metadata key
   * for diagnostics. This is distinct from {@link #CLIENT_INTERNAL_ERROR} (a genuine client-side
   * processing failure) so that "the client is behind the server" can be observed independently.
   *
   * <p>Expected metadata keys: {@code server_error_code} (the unrecognized code string), plus any
   * metadata carried by the original server error.
   */
  UNRECOGNIZED_SERVER_ERROR(
      "DB-ANALYTICS-40003",
      ErrorCategory.CLIENT_ERROR,
      new ErrorDescription(
          "The server returned an error code that this client does not recognize",
          "The server is likely newer than this client SDK and returned an error code not known to this version.",
          "Upgrade the client SDK to a version compatible with the server. See the server_error_code metadata for the original code, and contact your administrator if the problem persists."));

  private static final Map<String, AnalyticsErrorCode> CODE_MAP;

  static {
    Map<String, AnalyticsErrorCode> map = new HashMap<>();
    for (AnalyticsErrorCode value : values()) {
      map.put(value.code, value);
    }
    CODE_MAP = map;
  }

  private final String code;
  private final ErrorCategory errorCategory;
  private final ErrorDescription description;

  AnalyticsErrorCode(String code, ErrorCategory errorCategory, ErrorDescription description) {
    this.code = code;
    this.errorCategory = errorCategory;
    this.description = description;
  }

  /** Returns the error code string (e.g., {@code "DB-ANALYTICS-10100"}). */
  public String getCode() {
    return code;
  }

  /** Returns the error category for SDK control flow. */
  public ErrorCategory getErrorCategory() {
    return errorCategory;
  }

  /**
   * Returns the colocated user-facing description (message, cause, action).
   *
   * <p>The text content is not part of the API contract — see {@link ErrorDescription}.
   */
  public ErrorDescription getDescription() {
    return description;
  }

  /** Returns whether the client should trigger re-authentication. */
  public boolean requiresReauthentication() {
    return this == TOKEN_EXPIRED || this == SCALARDB_BACKEND_TOKEN_EXPIRED;
  }

  /** Returns {@code true} if this is a user error. */
  public boolean isUserError() {
    return errorCategory == ErrorCategory.USER_ERROR;
  }

  /**
   * Returns {@code true} if this error is safe to automatically retry.
   *
   * <p>"Retryable" means the operation is safe to re-execute without changing the outcome
   * (idempotent or otherwise side-effect-safe) <em>and</em> retrying may succeed. This is a
   * stronger guarantee than mere transience, because the property drives automatic retry: the
   * server-side gRPC mapping ({@code mapToGrpcStatus}) maps every retryable code to {@code
   * UNAVAILABLE} so the gRPC transport layer (and any status-aware proxy/mesh) retries it without
   * human judgement. A code that is transient but not safe to blindly re-execute must not be
   * categorized {@link ErrorCategory#RETRYABLE_SERVER_ERROR}.
   *
   * <p>This predicate drives retry on the <em>server</em> side only. By the time an {@link
   * AnalyticsException} carrying a retryable code surfaces to application code, the gRPC transport
   * has already exhausted its automatic retries; the code is reported for diagnostics, not as a
   * directive to retry again. Application code must not build a retry loop on {@code isRetryable()}
   * — there is no SDK-side retry loop (see ADR-0010), and re-executing here would double-retry an
   * operation the transport already gave up on.
   */
  public boolean isRetryable() {
    return errorCategory == ErrorCategory.RETRYABLE_SERVER_ERROR;
  }

  /**
   * Restores an {@link AnalyticsErrorCode} from a code string.
   *
   * <p>Returns an empty {@link Optional} when {@code code} matches no known constant. This is
   * expected at the gRPC/SDK boundary: a client may receive a code produced by a newer server that
   * is not present in this (older) version's enum. Callers should handle the empty case and degrade
   * gracefully (e.g. treat it as a generic, non-retryable failure) rather than assuming a constant
   * is always present.
   *
   * @param code the code string (e.g., {@code "DB-ANALYTICS-10100"})
   * @return the matching enum constant, or {@link Optional#empty()} if the code is not recognized
   */
  public static Optional<AnalyticsErrorCode> fromCode(String code) {
    return Optional.ofNullable(CODE_MAP.get(code));
  }

  /** Error categories for SDK control flow. */
  public enum ErrorCategory {
    /** User's input or request is the problem. User can fix it. */
    USER_ERROR,
    /** Server-side transient failure that is safe to automatically retry. */
    RETRYABLE_SERVER_ERROR,
    /** Server-side persistent failure. Retry will not help. */
    NON_RETRYABLE_SERVER_ERROR,
    /**
     * Failure detected on the client side before a structured server error was received (e.g. a
     * transport failure). Not retryable: transport-transient retries are handled at the gRPC layer.
     */
    CLIENT_ERROR
  }
}
