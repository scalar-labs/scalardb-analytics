# Authorization

Authorization decides whether an authenticated user may perform an operation on a catalog
resource. It combines access control entries on individual resources, role-based grants, a
built-in superuser role, and, for ScalarDB data sources, delegation to ScalarDB Cluster's own
privileges. Authorization runs only when authentication is enabled
(see [Authentication](authentication.md)); otherwise a no-op `AuthorizationService` allows
everything.

## Permissions and the resource hierarchy

A **permission** (`Permission`, in `domain`) names both a resource type and an operation level, so
it is meaningful only on resources of that type:

| Resource type | Permissions |
| --- | --- |
| Catalog | `CATALOG_READ`, `CATALOG_WRITE`, `CATALOG_ADMIN` |
| Data source | `DATA_SOURCE_READ`, `DATA_SOURCE_ADMIN` |
| Namespace | `NAMESPACE_READ` |
| Table | `TABLE_READ` |

Permissions do not include one another: `CATALOG_ADMIN` does not imply `CATALOG_READ`. Instead,
each operation lists every permission that satisfies it. The Java enums are the source of truth;
each permission and resource type has a deterministic UUID derived from its name, which the stored
grants reference, so renaming an enum value is a breaking change.

Resources form the hierarchy catalog → data source → namespace → table, and a grant on an
ancestor can satisfy an operation on a descendant. An operation declares, for each level of the
target's ancestry, which permissions are enough (`PermissionRequirement`), and the requirements
are combined with OR. For example, describing a table is allowed by `TABLE_READ` on the table,
`NAMESPACE_READ` on its namespace, `DATA_SOURCE_READ` or `DATA_SOURCE_ADMIN` on its data source, or
`CATALOG_READ` or `CATALOG_ADMIN` on its catalog. The requirements are declared next to each
operation in `usecase-impl`, which is the canonical list.

## Grants and roles

An **access control entry** (`AccessControlEntry`) grants one permission on one resource to a
grantee, which is either a user or a **role**. A user's effective permissions are the union of
their direct entries and the entries of every role assigned to them (`RoleAssignment`). This
mirrors `GRANT ... TO user` and `GRANT ... TO role` in ScalarDB SQL, and the RPC names follow ScalarDB
SQL, but the model differs where Analytics has its own resource types: permissions are specific to
a resource type, and there is no grant option.

**SUPERADMIN** is a built-in role (see `BuiltInRole`) that cannot be deleted. Its holders bypass
every check. Operations that are not scoped to one catalog, such as creating a catalog and
managing users and roles, require SUPERADMIN.

Granting and revoking permissions on a resource requires `CATALOG_ADMIN` on the catalog that
contains it, or SUPERADMIN. Users may list their own effective permissions; listing another user's
or a role's permissions requires SUPERADMIN.

## Checking an operation

`AuthorizationServiceImpl` in `service` implements the check, and each use case calls it before
touching metadata:

1. If the user holds SUPERADMIN, allow.
2. For each declared requirement, if the resource is under
   [ScalarDB Cluster delegation](#scalardb-cluster-delegation), ask ScalarDB Cluster; otherwise
   match the user's direct and role entries against the requirement.
3. Allow if any requirement is satisfied; otherwise the use case fails with `ACCESS_DENIED`.

List operations apply the same check to each item and return only the items the user may see.
They load the user's roles and entries once and evaluate the items in memory.

## ScalarDB Cluster delegation

When ScalarDB Cluster is the password backend, its privilege system already governs the data in
ScalarDB. With `acl-delegation` enabled in the ScalarDB Cluster backend settings, Analytics uses
those privileges instead of its own entries for namespaces and tables of ScalarDB data sources:

| Resource | Delegation enabled | Delegation disabled |
| --- | --- | --- |
| Catalog, data source | Analytics entries | Analytics entries |
| Namespace or table of a ScalarDB data source | ScalarDB Cluster privileges | Analytics entries |
| Namespace or table of any other data source | Analytics entries | Analytics entries |

For a delegated resource, `ScalarDbAuthorizationDelegateImpl` maps the user to their ScalarDB
Cluster username through the user's link, then checks that the user holds `SELECT` on the ScalarDB
namespace or table, using the ScalarDB Cluster token kept from the user's sign-in. Results are cached
briefly per user and resource. A user without a ScalarDB Cluster identity is denied. If the server
no longer holds the user's ScalarDB Cluster token, for example after a restart, the request fails
with `SCALARDB_BACKEND_TOKEN_EXPIRED`, which requires the client to sign in again and so refreshes
both tokens. Granting an Analytics permission
on a delegated resource fails with `RESOURCE_UNDER_ACL_DELEGATION`, because the entry would never be
evaluated.

Why ScalarDB Cluster, rather than both systems, decides these resources is recorded in
[design/delegate-scalardb-resource-authorization.md](design/delegate-scalardb-resource-authorization.md).
