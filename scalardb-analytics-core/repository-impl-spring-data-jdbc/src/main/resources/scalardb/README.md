# ScalarDB Schema Naming Conventions

## Namespace

All tables reside in a single ScalarDB namespace: **`scalardb_analytics`**.

## Table Naming

Most table names use a domain prefix to indicate which module they belong to.
Top-level entities that span multiple domains have no prefix.

| Prefix      | Domain   | Description                                      |
|-------------|----------|--------------------------------------------------|
| *(none)*    | Top-level | Cross-domain entities (e.g., catalogs)          |
| `registry_` | Registry | Data source, namespace, table, and column metadata |
| `auth_`     | Auth     | Users, credentials, and access tokens            |
| `authz_`    | Authz    | Roles, permissions, and access control entries   |

### Format

```
<table_name>                   -- top-level entity
<domain_prefix>_<table_name>   -- domain-scoped entity
```

### Examples

- `catalogs` - Catalog definitions (top-level)
- `registry_data_sources` - Data source configurations
- `registry_namespaces` - Namespace mappings
- `registry_tables` - Table metadata
- `registry_columns` - Column metadata
- `auth_users` - User accounts
- `auth_password_identities` - Password-based identity records
- `auth_access_tokens` - Access tokens
- `auth_internal_credentials` - Internal user credentials
- `authz_roles` - Role definitions
- `authz_role_assignments` - User-role assignments
- `authz_resource_types` - Resource type definitions
- `authz_permissions` - Permission definitions
- `authz_access_control_entries` - Access control entries

## Why a Single Namespace?

PostgreSQL has a 63-byte limit on identifiers. When ScalarDB maps
`namespace.table` to a PostgreSQL schema + table, long namespace names combined
with ScalarDB's internal metadata column prefixes can exceed this limit. Using a single namespace (`scalardb_analytics`) and encoding the domain in the
table name avoids the need for multiple namespaces and keeps all identifiers
well within the limit.
