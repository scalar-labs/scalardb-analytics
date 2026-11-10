# Keep one access token per user

## Question

How many access tokens can a user hold at once, and how are tokens keyed in the metadata store?

## Answer

Each user holds at most one access token. The `auth_access_tokens` table uses `user_id` as its
partition key and `token` as its clustering key. Signing in deletes the user's existing token and
creates a new one, so signing in again ends the previous session.

Validating a request is a lookup by both keys. Clients therefore send the `userId` they received
when signing in as the `x-user-id` header together with the token
(see [Authentication](../authentication.md)).

## Rationale

- Expired tokens never accumulate. The number of stored tokens is bounded by the number of users,
  and an expired token is replaced the next time its user signs in, so no purge job is needed.
- A purge would otherwise be hard to build: ScalarDB disables cross-partition scans by default, so
  finding expired tokens across all users would need an auxiliary, time-bucketed table or a
  single-partition design, both of which add schema and operational complexity.
- Looking tokens up by partition and clustering key avoids a secondary index on `token`, whose
  performance and consistency trade-offs in ScalarDB are unnecessary here.

## Consequences

- A user cannot keep concurrent sessions, for example from two machines: signing in on one ends
  the session on the other.
- Clients must keep the `userId` from the sign-in response and send it on every request.
- The key layout already allows several tokens per user. Supporting concurrent sessions would mean
  no longer deleting the previous token and adding a way to remove expired ones, for example by
  ordering a user's tokens by expiry.
