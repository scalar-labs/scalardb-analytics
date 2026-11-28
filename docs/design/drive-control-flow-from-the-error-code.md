# Drive control flow from the error code

## Question

What mechanism decides how code reacts to an error, such as whether to retry it or to sign in
again?

## Answer

The error code decides. Each `AnalyticsErrorCode` constant carries the properties that control flow
needs: its `ErrorCategory`, which determines retryability, and `requiresReauthentication()`. The
same constant also carries the user-facing description and, through the exception, the metadata
used for diagnostics. There is no parallel hierarchy of exception types for control flow, and
metadata never drives control flow.

Retryability is decided in the domain and executed by the transport. A code in
`RETRYABLE_SERVER_ERROR` must be safe to retry automatically, not merely transient. The server maps
every such code to the gRPC status `UNAVAILABLE`, and the SDK's gRPC retry policy retries it. The SDK
has no application-level retry loop, and failures that the transport already retried without a
structured error become non-retryable client-side codes
(see [Error handling](../error-handling.md#retry)).

## Rationale

- **Errors must survive the gRPC boundary**: Java exception types are lost at the boundary, while a
  code and its metadata are serialized and rebuilt in the SDK. A mechanism based on exception types
  would need a second mechanism for clients.
- **One place decides each property**: when a code is defined as retryable, that definition alone
  drives both the server's status mapping and the resulting retries. No separate retryability
  table has to be kept in sync.
- **No class per distinction**: a new error with different control flow needs a new enum constant,
  not a new exception class alongside the code.
- **Retry once, in the layer that already can**: gRPC's built-in retry works on status codes, so
  mapping retryable codes to `UNAVAILABLE` reuses it, along with any status-aware proxies. Retrying
  again in the SDK or in applications would multiply attempts for operations the transport already
  gave up on.

## Alternatives

Separate mechanisms for each purpose: exception types such as a retryable exception for control
flow, code strings for users, and messages for logs. Exception types give compile-time `catch`
handling on the server, but they do not reach clients, and the two hierarchies can diverge.

## Consequences

- Control flow checks the code or category (`isRetryable()`, `requiresReauthentication()`) instead
  of catching exception types, so the compiler does not enforce them. The exhaustive switch in the
  server's status mapping still flags codes without a status.
- Everything control flow needs must be expressible as properties of the enum. Richer instructions,
  such as retrying after a given delay, would need an extension, for example `google.rpc.RetryInfo`.
