# Server E2E Tests

End-to-end tests for the authentication and authorization feature with the Internal Backend. These tests exercise the full gRPC stack with auth enabled, TLS, and a real database (TestContainers + PostgreSQL).

The test base class (`AuthE2ETestBase`) starts a Spring Boot server with dynamic self-signed certificates and provides authenticated gRPC stubs for each service.

## Test Isolation

All test classes share a single server and database:

- The Spring `ApplicationContext` (and therefore the running server) is cached and reused across every class that extends `AuthE2ETestBase`.
- The PostgreSQL container is a `static` field started once for the whole suite.

Because the server and database are shared, restarting the server alone would not reset the data, and the initial admin is bootstrapped only at server startup. Each class therefore removes the resources it creates (users, roles, catalogs) in an `@AfterAll` cleanup hook so the suite is order-independent across classes.

Cleanup is performed through the gRPC API (e.g., `unregisterUser`, `deleteRole`, `deleteCatalog`) rather than at the database level. The E2E module is a black-box gRPC client, and the data is managed by ScalarDB (with transaction metadata and a coordinator table); manipulating the tables directly would couple the tests to the internal schema and risk leaving ScalarDB in an inconsistent state. Cleanup calls ignore `StatusRuntimeException` so that resources absent due to an earlier failure do not fail the teardown.

## Test Scenarios

Each test class is an ordered scenario that walks through one part of the auth flow.

### Authentication (`AuthenticationE2ETest`)

- Normal login returns a token and allows API access
- Wrong password returns `UNAUTHENTICATED`
- Non-existent user returns `UNAUTHENTICATED`
- Invalid token returns `UNAUTHENTICATED`
- Request without credentials returns `UNAUTHENTICATED`

### User Management (`UserManagementE2ETest`)

- Register a new user
- List users shows registered users
- Describe user shows details and backend type
- New user can log in
- Non-SUPERADMIN user registration is rejected
- Unregister a user
- Login after unregistration fails

### Role Management (`RoleManagementE2ETest`)

- List roles shows the built-in `SUPERADMIN` role
- Create a custom role
- Grant a role to a user
- List roles shows the custom role
- Non-SUPERADMIN role creation is rejected
- Deleting a built-in role fails
- Revoke a role from a user
- Delete a custom role

### Permissions and Resource Hierarchy (`PermissionE2ETest`)

- SUPERADMIN creates a catalog
- SUPERADMIN lists catalogs
- Non-SUPERADMIN catalog creation is rejected
- A user without permissions sees an empty catalog list
- Granting `CATALOG_READ` enables catalog listing
- List permissions shows the direct permission
- Role-based permission grants access (`VIA_ROLE` source)
- Revoking a permission removes access

## Related

UseCase-level integration tests (without gRPC) are in `server/base/src/integrationTest/`. They cover `AuthenticationUseCase`, `TokenValidationUseCase`, `UserUseCase`, `RoleUseCase`, `PermissionUseCase`, and `AuthorizationService` directly against a real database.
