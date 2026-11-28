# Authentication

Authentication establishes which Analytics user is calling the catalog server. Users sign in once
with a username and password, receive an access token, and send that token with every later
request. Authentication is enabled with `scalar.db.analytics.server.auth.enabled`; when it is
disabled, the server installs no authentication interceptor and no authorization checks. For
configuration, see the
[official guide](https://scalardb.scalar-labs.com/docs/latest/scalardb-analytics/authentication-and-authorization).

## Users, backend users, and links

Three concepts are kept separate, and the `user` and `internal-backend` CLI command groups follow
the same split:

- An **Analytics user** (`AuthUser`, in `api`) is the principal. It has a stable `userId` (UUID)
  and a username, and it is what roles, permissions, and access tokens attach to. It holds no
  credential.
- A **backend user** is a login identity in a **password backend**, the system that verifies
  passwords. `PasswordBackendType` names the backends: `INTERNAL`, which stores password hashes in
  the metadata store (`InternalCredential`), and `SCALARDB_CLUSTER`, which delegates verification to
  ScalarDB Cluster.
- A **link** (`PasswordIdentity`, in `domain`) maps a backend user, `(backend, backendUserId)`, to
  an Analytics user. A user can be linked to backend users in several backends.

Separating them lets one principal sign in through more than one backend and keeps permissions
attached to the principal, not to a credential. Backend users can be created and deleted only in
the internal backend; other backends own their users, and Analytics only links to them.

The server uses one password backend at a time, selected by
`scalar.db.analytics.server.auth.password.backend`.

## Signing in

`AuthenticationUseCaseImpl` handles a password sign-in:

1. The configured backend verifies the username and password and returns the backend user ID.
   The ScalarDB Cluster backend also returns a ScalarDB Cluster token, which the server keeps in
   memory for [authorization delegation](authorization.md#scalardb-cluster-delegation).
2. The link for that backend user resolves the Analytics user.
3. A new access token is issued, replacing the user's previous one, and returned together with
   its expiry and the `userId`.

Backends differ in who owns the user list:

- With the **internal** backend, the metadata store is the source of truth. A backend user without
  a link, or a link whose Analytics user has been deleted, fails with `AUTHENTICATION_FAILED`.
- The **ScalarDB Cluster** backend implements `JitProvisioningBackend`: ScalarDB Cluster is the
  source of truth, so the first successful sign-in of an unknown backend user creates the Analytics
  user and its link (just-in-time provisioning). Concurrent first sign-ins can conflict; the use case
  retries the transaction so the second one finds the identity the first created.

A new deployment needs someone who can manage it. At startup, `SuperAdminBootstrapRunner` ensures
the built-in SUPERADMIN role exists and, while no user holds it, assigns it to the configured
initial admin user. With the internal backend, it creates that user first when an initial admin
password is configured. With a just-in-time backend, the assignment happens at that user's first
sign-in instead.

## Access tokens and request validation

An access token is an opaque random string stored as an `IssuedToken` with its expiry. Each user has
at most one token, and signing in again replaces it. Concurrent sign-ins of the same user conflict
and are retried like concurrent first sign-ins, so they also leave one token. Why is recorded in
[design/keep-one-access-token-per-user.md](design/keep-one-access-token-per-user.md).

Clients send the token as `Authorization: Bearer <token>` and the `userId` as `x-user-id`. On the
server, `AuthenticationInterceptor` validates both through `TokenValidationUseCase`, which looks up
the token by user and token and checks its expiry. Validated tokens are cached in memory for a
short time on each server instance, so a replaced token can still be accepted on that instance
until its cache entry expires. An expired or unknown token fails with `TOKEN_EXPIRED` or
`TOKEN_INVALID` (see [Error handling](error-handling.md)).

On the client side, the SDK's `TokenManager` signs in, attaches the headers through
`ClientAuthInterceptor`, and signs in again in the background before the token expires. It stops
refreshing when the server rejects the credentials. The token can also be cached on disk, which the
CLI enables so that separate invocations reuse it.
The CLI reads the username and password only from its configuration file and environment
variables; why is recorded in
[design/read-cli-credentials-from-configuration.md](design/read-cli-credentials-from-configuration.md).
