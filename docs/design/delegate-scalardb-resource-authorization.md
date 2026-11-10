# Delegate ScalarDB resource authorization to ScalarDB Cluster

## Question

When users sign in through ScalarDB Cluster, which system decides access to the namespaces and
tables of ScalarDB data sources?

## Answer

When `acl-delegation` is enabled in the ScalarDB Cluster backend settings, ScalarDB Cluster alone
decides. For a namespace or table of a ScalarDB data source, Analytics checks that the user holds
`SELECT` on the corresponding ScalarDB namespace or table and does not evaluate its own entries.
Granting Analytics permissions on such resources is rejected. Catalogs, data sources, and resources
of other data sources stay under Analytics authorization. Delegation is off unless enabled.

The behavior of the check is described in
[Authorization](../authorization.md#scalardb-cluster-delegation).

## Rationale

- **One source of truth**: ScalarDB Cluster's privileges already govern the ScalarDB data. Users
  expect them to be authoritative, and maintaining matching grants in two systems is confusing.
- **Scope**: ScalarDB privileges exist at the namespace and table level, which map directly to
  Analytics namespaces and tables. Catalogs and data sources have no ScalarDB equivalent, and other
  data source types offer no comparable privilege system.
- **Permission mapping**: Analytics operations on these resources only read metadata and data, so
  ScalarDB `SELECT` is the privilege that corresponds to `NAMESPACE_READ` and `TABLE_READ`.
- **Identity**: ScalarDB Cluster privileges belong to ScalarDB users, so only users linked to a
  ScalarDB Cluster identity can hold them. A user without one cannot have the required privileges,
  so the check denies access.
- **Opt-in**: delegation changes who controls access to existing resources, so operators must
  enable it knowingly.

## Consequences

- Access to delegated resources depends on ScalarDB Cluster being reachable; when it is not, the
  check fails with a retryable error rather than falling back.
- The server keeps each user's ScalarDB Cluster token in memory to query privileges on their
  behalf. After a server restart, users must sign in again before they can reach delegated
  resources.
- Privilege results are cached briefly, so a privilege change in ScalarDB Cluster takes effect
  after the cache entry expires.
