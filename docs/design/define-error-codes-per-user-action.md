# Define error codes per user action

## Question

How fine-grained should error codes be: what does one `AnalyticsErrorCode` represent?

## Answer

One code represents one problem that calls for a distinct reaction. If two failures require
different actions from the user or different handling in the SDK, they get separate codes, such as
`CATALOG_ALREADY_EXISTS` and `DATA_SOURCE_ALREADY_EXISTS` rather than one generic duplicate code.
Codes describe the user's situation, not the internal cause of the failure.

## Rationale

- **Precise handling without parsing messages**: clients can branch on the code alone. With broad
  codes, telling one duplicate from another would require parsing message text, which is not part
  of the API.
- **Actionable for users**: a code points to one concrete problem with one concrete fix, which
  users can look up in the documentation.
- **Stable**: user-facing situations change less often than implementation. A code named after an
  internal cause, such as a constraint violation, would change or become wrong when the
  implementation is refactored, breaking clients that depend on it.

## Alternatives

- **A few broad categories** (for example, "entity already exists"): fewer codes to maintain, but
  clients cannot distinguish cases without parsing messages, and users see one code for problems
  that need different actions.
- **Codes for internal failure points** (for example, unique index violations): exposes
  implementation details, and the codes change whenever the implementation does.

## Consequences

There are more codes to maintain, and a new resource type or operation may need new codes. Number
ranges have to be allocated with care so codes do not overlap. Each new code also needs a gRPC
status in `ExceptionHandlingInterceptor`, whose exhaustive switch over the enum makes a missing
case visible at compile time.
