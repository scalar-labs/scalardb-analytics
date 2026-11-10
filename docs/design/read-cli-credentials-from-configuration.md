# Read CLI credentials from configuration

## Question

How does the CLI accept the username and password it signs in with?

## Answer

Only through the configuration file (`scalar.db.analytics.client.auth.username` and
`scalar.db.analytics.client.auth.password`) and the corresponding environment variables
(`SCALAR_DB_ANALYTICS_CLIENT_AUTH_USERNAME` and `SCALAR_DB_ANALYTICS_CLIENT_AUTH_PASSWORD`).
The CLI has no command-line options for its own sign-in credentials.

This concerns the credentials the CLI signs in with. Commands that set another user's password,
such as `user create` and `internal-backend create`, take it from an interactive prompt
(`--password`) or standard input (`--password-stdin`), never as an option value.

## Rationale

- **Consistency**: every other client setting, such as the server address and TLS, comes from the
  configuration file and environment variables. Options only for credentials would make the
  interface inconsistent.
- **Security**: a password given as an option value is visible in process listings and stays in
  shell history.
- **Sufficiency**: environment variables already serve one-off invocations, scripts, and CI
  pipelines without that exposure.

## Consequences

Signing in for a single ad-hoc command requires setting environment variables or editing the
configuration file. If credentials on the command line become necessary, they should be added as a
username option together with a standard-input password option, following the pattern of
`--password-stdin`.
