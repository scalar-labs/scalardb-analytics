# Error Mapping Documentation

This document describes how errors are mapped to gRPC status codes in the ScalarDB Analytics server.

## Overview

The server uses `ExceptionHandlingInterceptor` to automatically catch and map exceptions to appropriate gRPC status codes across all service methods. All errors carry a `DB-ANALYTICS-NNNNN` error code via the `AnalyticsErrorCode` enum.

### Error Code Format

Error codes follow the ScalarDB convention: `DB-ANALYTICS-NNNNN`

- `1xxxx` — User errors (invalid input, permission denied, authentication)
- `3xxxx` — Internal/operational errors (300xx retryable, 301xx non-retryable)
- `4xxxx` — Client-side errors (transport failures detected by the SDK before a structured server error was received; never emitted by the server)

### Error Categories

Each error code carries an `ErrorCategory` that drives SDK control flow:

| ErrorCategory | Meaning | SDK Behavior |
|---------------|---------|--------------|
| `USER_ERROR` | User's input or request is the problem | Propagate to caller immediately |
| `RETRYABLE_SERVER_ERROR` | Server-side transient failure that is **safe to automatically retry** (idempotent / side-effect-safe) | Retried by the gRPC transport: every retryable code maps to `UNAVAILABLE`, which the gRPC retry policy (and status-aware proxies/meshes) retries with backoff. There is no separate SDK-side retry loop. |
| `NON_RETRYABLE_SERVER_ERROR` | Server-side persistent failure | Propagate to caller immediately |
| `CLIENT_ERROR` | Failure detected client-side before a structured server error was received (transport failure). Produced only by the SDK; never emitted by the server | Propagate to caller immediately (transport-transient retries already happen at the gRPC layer) |

### Reauthentication

Error codes with `requiresReauthentication = true` indicate that the SDK should trigger a re-authentication flow before retrying.

### gRPC Response Format

- **Status code**: Derived from the error code via `ExceptionHandlingInterceptor.mapToGrpcStatus()`
- **Status description**: Prefixed with the error code (e.g., `"DB-ANALYTICS-10100: Catalog already exists [catalog_name=foo]"`)
- **Trailing metadata**:
  - `grpc-status-details-bin`: A `google.rpc.Status` proto containing an `AnalyticsError` detail with error code and structured metadata map (entity names, IDs, etc.)

### Logging Levels

- **User errors** (WARN): `ErrorCategory.USER_ERROR`
- **Server errors** (ERROR): `ErrorCategory.RETRYABLE_SERVER_ERROR`, `ErrorCategory.NON_RETRYABLE_SERVER_ERROR`

## Error Code Catalog

### 100xx — Authentication & Authorization

| Error Code | Name | gRPC Status | Category | Reauth | Description |
|------------|------|-------------|----------|--------|-------------|
| `DB-ANALYTICS-10000` | `AUTHENTICATION_FAILED` | `UNAUTHENTICATED` | USER_ERROR | No | Authentication failed (invalid credentials) |
| `DB-ANALYTICS-10001` | `TOKEN_EXPIRED` | `UNAUTHENTICATED` | USER_ERROR | Yes | Token has expired |
| `DB-ANALYTICS-10002` | `TOKEN_INVALID` | `UNAUTHENTICATED` | USER_ERROR | No | Token is invalid |
| `DB-ANALYTICS-10003` | `ACCESS_DENIED` | `PERMISSION_DENIED` | USER_ERROR | No | Insufficient permissions |

### 101xx — Entity Already Exists

| Error Code | Name | gRPC Status | Category | Reauth | Description |
|------------|------|-------------|----------|--------|-------------|
| `DB-ANALYTICS-10100` | `CATALOG_ALREADY_EXISTS` | `ALREADY_EXISTS` | USER_ERROR | No | Catalog already exists |
| `DB-ANALYTICS-10101` | `DATA_SOURCE_ALREADY_EXISTS` | `ALREADY_EXISTS` | USER_ERROR | No | Data source already exists |
| `DB-ANALYTICS-10102` | `NAMESPACE_ALREADY_EXISTS` | `ALREADY_EXISTS` | USER_ERROR | No | Namespace already exists |
| `DB-ANALYTICS-10103` | `TABLE_ALREADY_EXISTS` | `ALREADY_EXISTS` | USER_ERROR | No | Table already exists |
| `DB-ANALYTICS-10104` | `ROLE_ALREADY_EXISTS` | `ALREADY_EXISTS` | USER_ERROR | No | Role already exists |
| `DB-ANALYTICS-10105` | `USER_ALREADY_EXISTS` | `ALREADY_EXISTS` | USER_ERROR | No | User already exists |
| `DB-ANALYTICS-10106` | `ROLE_ALREADY_ASSIGNED` | `ALREADY_EXISTS` | USER_ERROR | No | Role already assigned |
| `DB-ANALYTICS-10107` | `PERMISSION_ALREADY_GRANTED` | `ALREADY_EXISTS` | USER_ERROR | No | Permission already granted |
| `DB-ANALYTICS-10108` | `BACKEND_USER_ALREADY_LINKED` | `ALREADY_EXISTS` | USER_ERROR | No | Backend user already linked (`reason=backend_user_linked`), or the user is already linked on the same backend (`reason=user_linked_on_backend`) |

### 102xx — Operation Precondition

| Error Code | Name | gRPC Status | Category | Reauth | Description |
|------------|------|-------------|----------|--------|-------------|
| `DB-ANALYTICS-10200` | `CATALOG_NOT_EMPTY` | `FAILED_PRECONDITION` | USER_ERROR | No | Catalog is not empty |
| `DB-ANALYTICS-10201` | `DATA_SOURCE_NOT_EMPTY` | `FAILED_PRECONDITION` | USER_ERROR | No | Data source is not empty |
| `DB-ANALYTICS-10202` | `BUILT_IN_ENTITY_NOT_MODIFIABLE` | `FAILED_PRECONDITION` | USER_ERROR | No | Cannot modify built-in entity |
| `DB-ANALYTICS-10203` | `USER_NOT_EMPTY` | `FAILED_PRECONDITION` | USER_ERROR | No | User is not empty |
| `DB-ANALYTICS-10204` | `RESOURCE_UNDER_ACL_DELEGATION` | `FAILED_PRECONDITION` | USER_ERROR | No | Analytics permission granted on a resource under ACL delegation |
| `DB-ANALYTICS-10205` | `INTERNAL_PASSWORD_BACKEND_REQUIRED` | `FAILED_PRECONDITION` | USER_ERROR | No | The operation manages internal-backend users, but the server uses another password backend |

### 103xx — Data Source Connection

| Error Code | Name | gRPC Status | Category | Reauth | Description |
|------------|------|-------------|----------|--------|-------------|
| `DB-ANALYTICS-10300` | `DATA_SOURCE_AUTHENTICATION_FAILED` | `INVALID_ARGUMENT` | USER_ERROR | No | Data source authentication failed |
| `DB-ANALYTICS-10301` | `DATA_SOURCE_UNREACHABLE` | `INVALID_ARGUMENT` | USER_ERROR | No | Data source is unreachable |

### 104xx — Input Validation

| Error Code | Name | gRPC Status | Category | Reauth | Description |
|------------|------|-------------|----------|--------|-------------|
| `DB-ANALYTICS-10400` | `INVALID_ARGUMENT` | `INVALID_ARGUMENT` | USER_ERROR | No | Invalid argument or validation failure |
| `DB-ANALYTICS-10401` | `INVALID_SCHEMA_METADATA` | `INVALID_ARGUMENT` | USER_ERROR | No | A namespace, table, column, or column type name in the data source schema contains a prohibited control or bidirectional formatting character |

`INVALID_ARGUMENT` raised by request validation carries `field` (the invalid request field) and `reason` (a snake_case token naming the violated rule, such as `blank` or `required_for_provider`) in its metadata, plus the names of the entities involved. `field` is the name of the request field in the proto definition:

| `field` | `reason` | Raised when |
|---------|----------|-------------|
| `role_name`, `username` | `blank` | A role or user is created with a blank name |
| `schema_json` | `not_allowed_for_provider` | A data source registration gives a manual schema for a provider that resolves schemas. The metadata also carries `catalog_name`, `data_source_name`, and `provider_type` |
| `schema_json` | `required_for_provider` | A data source registration gives no manual schema for a provider that does not resolve schemas. The metadata also carries `catalog_name`, `data_source_name`, and `provider_type` |
| `catalog_id`, `data_source_id`, `namespace_id`, `table_id`, `role_id`, `user_id`, `grantee_id`, `resource_id` | `malformed_uuid` | The ID of a `*ById` request is not a UUID |
| `grantee_type`, `permission`, `backend` | `unspecified` | The enum is left at its zero (`*_UNSPECIFIED`) value |
| `grantee_type`, `permission`, `backend` | `unrecognized` | The enum holds a value the server does not know, such as one sent by a newer client |
| `provider_payload_json` | `required` | A data source registration has no provider JSON |
| `provider_payload_json`, `schema_json` | `invalid` | The provider or schema JSON does not decode: malformed JSON, an unknown provider type or data type, or a value the provider rejects. The metadata also carries `catalog_name` and `data_source_name`; the decoder's message is logged on the server but not returned, since it can echo the payload |

### 105xx — Entity Not Found

Raised when an operation refers to an entity that must exist, such as the role in a role grant or the catalog a data source is registered in. Operations that report absence in their result (find, describe, delete) do not raise these.

| Error Code | Name | gRPC Status | Category | Reauth | Description |
|------------|------|-------------|----------|--------|-------------|
| `DB-ANALYTICS-10500` | `CATALOG_NOT_FOUND` | `NOT_FOUND` | USER_ERROR | No | Catalog not found |
| `DB-ANALYTICS-10501` | `DATA_SOURCE_NOT_FOUND` | `NOT_FOUND` | USER_ERROR | No | Data source not found |
| `DB-ANALYTICS-10502` | `NAMESPACE_NOT_FOUND` | `NOT_FOUND` | USER_ERROR | No | Namespace not found |
| `DB-ANALYTICS-10503` | `TABLE_NOT_FOUND` | `NOT_FOUND` | USER_ERROR | No | Table not found |
| `DB-ANALYTICS-10504` | `ROLE_NOT_FOUND` | `NOT_FOUND` | USER_ERROR | No | Role not found |
| `DB-ANALYTICS-10505` | `USER_NOT_FOUND` | `NOT_FOUND` | USER_ERROR | No | User not found |
| `DB-ANALYTICS-10506` | `BACKEND_USER_NOT_FOUND` | `NOT_FOUND` | USER_ERROR | No | Backend user not found in the password backend |
| `DB-ANALYTICS-10507` | `BACKEND_USER_LINK_NOT_FOUND` | `NOT_FOUND` | USER_ERROR | No | The user is not linked to the backend user |

### 300xx — Retryable Internal Errors

| Error Code | Name | gRPC Status | Category | Reauth | Description |
|------------|------|-------------|----------|--------|-------------|
| `DB-ANALYTICS-30000` | `ANALYTICS_DB_CONNECTION_FAILED` | `UNAVAILABLE` | RETRYABLE | No | Analytics database connection failed |
| `DB-ANALYTICS-30002` | `SCALARDB_CLUSTER_UNAVAILABLE` | `UNAVAILABLE` | RETRYABLE | No | ScalarDB Cluster is unreachable |

> Every `RETRYABLE_SERVER_ERROR` code maps to `UNAVAILABLE` so the gRPC transport retries it. `DB-ANALYTICS-30001` is intentionally unused: it previously held `ANALYTICS_DB_OPERATION_FAILED`, which was recategorized as non-retryable (see below) and is not reused.

### 301xx — Non-retryable Internal Errors

| Error Code | Name | gRPC Status | Category | Reauth | Description |
|------------|------|-------------|----------|--------|-------------|
| `DB-ANALYTICS-30100` | `DATA_INCONSISTENCY` | `DATA_LOSS` | NON_RETRYABLE | No | Persisted data does not match expected schema |
| `DB-ANALYTICS-30101` | `SCALARDB_BACKEND_TOKEN_EXPIRED` | `UNAUTHENTICATED` | NON_RETRYABLE | Yes | ScalarDB Cluster backend token expired |
| `DB-ANALYTICS-30102` | `SCALARDB_PRIVILEGE_CHECK_FAILURE` | `INTERNAL` | NON_RETRYABLE | No | ScalarDB privilege check failed |
| `DB-ANALYTICS-30103` | `INTERNAL_ERROR` | `INTERNAL` | NON_RETRYABLE | No | Unexpected internal error |
| `DB-ANALYTICS-30104` | `ANALYTICS_DB_OPERATION_FAILED` | `INTERNAL` | NON_RETRYABLE | No | Analytics database operation failed (generic catch-all; not safe to auto-retry) |

### 400xx — Client-side Errors

Produced only by the SDK (`GrpcExceptionMapper`) when a request terminates before a structured `AnalyticsError` is received. The server never emits these; `mapToGrpcStatus` rejects them.

| Error Code | Name | Mapped From (gRPC status, detail-less) | Category | Reauth | Description |
|------------|------|----------------------------------------|----------|--------|-------------|
| `DB-ANALYTICS-40000` | `SERVER_UNREACHABLE` | `UNAVAILABLE` | CLIENT_ERROR | No | The server could not be reached |
| `DB-ANALYTICS-40001` | `REQUEST_TIMEOUT` | `DEADLINE_EXCEEDED` | CLIENT_ERROR | No | The request timed out |
| `DB-ANALYTICS-40002` | `CLIENT_INTERNAL_ERROR` | any other status / non-gRPC exception | CLIENT_ERROR | No | Unexpected client-side error |

### Non-AnalyticsException Mappings

| Exception Type | Error Code Used | gRPC Status | Description |
|----------------|----------------|-------------|-------------|
| Any other exception | `DB-ANALYTICS-30103` | `INTERNAL` | Unexpected error |

A bare `IllegalArgumentException` is not treated as a caller error. Invalid input is reported as `AnalyticsException` before it reaches code with its own argument checks: the gRPC layer decodes request fields (`RequestFields`, `RequestMapper`), and the use cases validate the rest, mirroring the checks of the domain and API constructors (such as a blank role name). An `IllegalArgumentException` that reaches the interceptor therefore indicates a server-side defect and maps to `DB-ANALYTICS-30103`. New request-path validation must throw `AnalyticsException` with `field` and `reason` metadata.

## Implementation Details

### ExceptionHandlingInterceptor

- Located in `com.scalar.db.analytics.server.grpc.ExceptionHandlingInterceptor`
- Implemented as a gRPC `ServerInterceptor` that automatically handles all exceptions
- Uses `mapToGrpcStatus()` to derive the gRPC status code from the `AnalyticsErrorCode`
- Attaches `grpc-status-details-bin` with an `AnalyticsError` proto to all error responses via the standard `StatusProto` utility

### Adding New Error Codes

1. Add a new constant to `AnalyticsErrorCode` enum with code and error category. Document expected metadata keys in Javadoc.
2. Add the new code to the `mapToGrpcStatus()` switch in `ExceptionHandlingInterceptor`
3. Use the new code in `throw new AnalyticsException(AnalyticsErrorCode.NEW_CODE, "message", Map.of("key", value))`
4. Update this documentation
