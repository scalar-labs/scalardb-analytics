# Error handling

Every failure that the catalog server reports, and every failure that the client SDK raises, is an
`AnalyticsException` carrying an `AnalyticsErrorCode`. The error code is the single taxonomy for
three purposes: telling users what went wrong and what to do, driving control flow such as retry
and re-authentication, and giving operators enough context to diagnose problems. Both types live in
`api` (`com.scalar.db.analytics.api.error`), so the server and the Java 8 clients share them.

## Error codes

Each `AnalyticsErrorCode` constant carries:

- a stable code in the ScalarDB format `DB-ANALYTICS-NNNNN`;
- an `ErrorCategory` that drives control flow; and
- an `ErrorDescription` holding the user-facing message, cause, and action.

The first digit of the number groups codes:

| Range | Meaning |
| --- | --- |
| `1xxxx` | User errors: authentication and authorization, duplicates, preconditions, data source connection, validation |
| `3xxxx` | Internal errors: `300xx` retryable, `301xx` not retryable |
| `4xxxx` | Client-side errors detected by the SDK before a structured server error arrived; never sent by the server |

The categories are `USER_ERROR`, `RETRYABLE_SERVER_ERROR`, `NON_RETRYABLE_SERVER_ERROR`, and
`CLIENT_ERROR`. `requiresReauthentication()` marks the codes that require signing in again.

The following rules keep the codes usable as an API:

- **Codes are stable.** Adding a code is compatible; changing the meaning of a code or removing it
  is a breaking change.
- **One code per user action.** Two failures get separate codes when they need different actions
  from the user or different handling in the SDK
  (see [design/define-error-codes-per-user-action.md](design/define-error-codes-per-user-action.md)).
- **User errors hide internals.** A `1xxxx` code describes the user's problem, such as "Catalog is
  not empty", never the implementation, such as a constraint violation. A `3xxxx` code is specific
  enough for triage while the user sees a generic presentation.
- **Wording belongs to the code, not to the throw site.** Throw sites pass only the code and
  metadata, and `AnalyticsException` has no constructor that takes a message. The text of
  `ErrorDescription` is for display only and may change at any time; code must never parse it.
- **Metadata is context, not control.** `AnalyticsException` carries a `Map<String, String>` of
  metadata, such as entity names and IDs, for messages and diagnostics. Control flow uses only the
  code, its category, and `requiresReauthentication()`
  (see [design/drive-control-flow-from-the-error-code.md](design/drive-control-flow-from-the-error-code.md)).

`AnalyticsException` is unchecked, so it propagates from where it is thrown to the gRPC boundary
without being caught and rewrapped on the way
(see [design/use-an-unchecked-analytics-exception.md](design/use-an-unchecked-analytics-exception.md)).
Its message has the form `DB-ANALYTICS-10100: Catalog already exists [catalog_name=foo]`.

The complete list of codes, with their gRPC status mapping, is
[`ERROR_MAPPING.md`](../scalardb-analytics-core/server/base/ERROR_MAPPING.md).

## Crossing the gRPC boundary

Java exceptions do not cross gRPC, so the error travels as data and is rebuilt on the other side.

On the server, `ExceptionHandlingInterceptor` catches every exception from interceptors and
services. It chooses a gRPC status from the error code and attaches an `AnalyticsError` message
(`error/v1/analytics_error.proto`: the code string and the metadata) to the status details using
gRPC's rich error model. Every retryable code maps to `UNAVAILABLE`.

In the SDK, `GrpcExceptionMapper` is the only place that turns a gRPC or runtime failure into an
`AnalyticsException`:

1. With an `AnalyticsError` detail, it rebuilds the exception from the code and metadata and
   ignores the gRPC status, because the code is the source of truth. A code this client does not
   know, for example from a newer server, becomes `UNRECOGNIZED_SERVER_ERROR` with the original code
   kept in the `server_error_code` metadata entry.
2. Without a detail, it maps the transport status to a client-side code: `UNAVAILABLE` to
   `SERVER_UNREACHABLE` and `DEADLINE_EXCEEDED` to `REQUEST_TIMEOUT`.
3. Anything else becomes `CLIENT_INTERNAL_ERROR`.

## Retry

Whether an error may be retried is decided once, by its category: `RETRYABLE_SERVER_ERROR` means
the operation is safe to execute again automatically and retrying may succeed. The server's status
mapping turns those codes into `UNAVAILABLE`, and the SDK's gRPC retry policy retries
`UNAVAILABLE` with exponential backoff (see [Architecture](architecture.md#clients)). There is no
retry loop in the SDK or in application code on top of that: by the time an `AnalyticsException`
reaches application code, the transport has already given up, and retrying again would multiply
attempts.
