# repository-impl-spring-data-jdbc

Spring Data JDBC implementation of the ScalarDB Analytics repository layer.

## Overview

This module provides a Spring Data JDBC-based implementation of the repository interfaces defined in the `repository` module. It supports multiple RDBMSs (PostgreSQL, MySQL, SQL Server, Oracle) with a vendor-neutral schema design.

## Architecture

### Repository Pattern Implementation

The repository layer follows a clean architecture with two types of services:

1. **Repository Services**: Simple CRUD operations for single domain entities
   - Examples: `DataSourceRepository`, `NamespaceRepository`, `TableRepository`
   - Operations: create, findById, update, delete, list
   - Implementation: Leverage Spring Data JDBC's `ListCrudRepository`

2. **Query Services**: Complex queries involving joins across multiple tables
   - Examples: `DataSourceQueryService`, `DataSourceNamespaceQueryService`
   - Operations: Custom queries with joins, filtering, and aggregations
   - Implementation: Custom SQL using `JdbcTemplate` or `NamedParameterJdbcOperations`

### Package Structure

```
repository/impl/spring/
├── boot/                    # Spring Boot configuration
├── config/                  # Spring configuration classes
├── converter/               # Type converters for database mapping
│   ├── reading/            # Database to Java converters
│   └── writing/            # Java to database converters
├── exception/              # Repository-specific exceptions
├── query/                  # Query service implementations for complex queries
│   └── dialect/            # SQL dialect abstraction for Query services
├── repository/             # Repository service implementations
│   └── jdbc/              # Spring Data JDBC entities and repositories (colocated)
└── transaction/            # Transaction management
```

### Key Design Decisions

1. **Vendor-Neutral Schema Design**
   - Single ScalarDB SQL migration executed via `ScalarDbSqlMigrator`
   - Type mappings support all target databases
   - Unique constraint handling varies by database (MySQL/Oracle don't support constraints on JSON/CLOB)

2. **Type Conversion Strategy**
   - Custom converters registered based on database type
   - `StringList` wrapper class for uniform array handling
   - Database-specific type support (e.g., PostgreSQL native UUID and JSONB)

3. **Repository vs Query Service Separation**
   - Separation of concerns between simple CRUD and complex queries
   - Repository implementations use Spring Data JDBC repositories
   - Query services handle multi-table joins and complex operations

4. **Transaction Management**
   - `withTransaction()`: Execute multiple operations in a single transaction
   - `single()`: Each operation in its own transaction (leverages @Transactional)
   - Context objects from `single()` must not be reused
   - Repository / query implementations annotate the class with `@Transactional(readOnly = true)` so that `single()` calls still execute inside a Spring-managed read-only transaction.
   - Methods that mutate state override this default with `@Transactional(readOnly = false)` to obtain a write-capable boundary.

## Database Type Compatibility

### Type Mapping Matrix

| Java Type | PostgreSQL | MySQL | SQL Server | Oracle | H2 |
|-----------|------------|--------|------------|--------|-----|
| `UUID` | `UUID` | `CHAR(36)` | `CHAR(36)` | `CHAR(36)` | `UUID` |
| `List<String>` (via `StringList`) | `TEXT[]` | `JSON` | `NVARCHAR(400)` | `VARCHAR2(4000)` | `VARCHAR ARRAY` |
| `Boolean` | `BOOLEAN` | `TINYINT(1)` | `BIT` | `NUMBER(1)` | `BOOLEAN` |
| `boolean` | `BOOLEAN` | `TINYINT(1)` | `BIT` | `NUMBER(1)` | `BOOLEAN` |
| `String` (JSON) | `JSONB` | `JSON` | `NVARCHAR(MAX)` | `VARCHAR2(4000)` | `VARCHAR(4000)` |
| `Instant` | `TIMESTAMPTZ` | `TIMESTAMP` | `DATETIME2` | `TIMESTAMP` | `TIMESTAMP` |

### Type Converters

Converters are automatically selected based on database type:

- **PostgreSQL**: `StringListToPostgreSqlArrayConverter` / `PostgreSqlArrayToStringListConverter` (native array support)
- **MySQL/SQL Server**: `StringListToJsonConverter` / `JsonToStringListConverter` (JSON storage)
- **Oracle**: `StringListToJsonConverter` / `JsonToStringListConverter` (JSON storage)
- **H2**: `StringListToH2ArrayConverter` / `H2ArrayToStringListConverter` (native array support)
- **UUID handling**: Native for PostgreSQL/H2, String conversion for MySQL/SQL Server/Oracle
- **Boolean handling**: Native for PostgreSQL/H2/SQL Server, Integer conversion for MySQL/Oracle

## Implementation Patterns

### Repository Implementation
```java
@Component
@Transactional(readOnly = true)
public class CatalogRepositoryImpl implements CatalogRepository<SpringDataJdbcTransactionContext> {
    private final CatalogJdbcRepository jdbcRepository;

    @Override
    public Optional<Catalog> findByName(SpringDataJdbcTransactionContext ctx, String name) {
        return jdbcRepository.findByName(name).map(this::toModel);
    }
}
```

### Query Service Implementation
```java
@Component
public class DataSourceNamespaceQueryServiceImpl
    implements DataSourceNamespaceQueryService<SpringDataJdbcTransactionContext> {

    @Override
    public List<DataSourceNamespace> listByCatalogName(
            SpringDataJdbcTransactionContext ctx, String catalogName) throws RepositoryException {
        String sql = """
            SELECT n.namespace_id, n.data_source_id, n.names,
                   d.catalog_id, d.name as data_source_name, d.provider_type
            FROM namespaces n
            JOIN data_sources d ON n.data_source_id = d.data_source_id
            JOIN catalogs c ON d.catalog_id = c.catalog_id
            WHERE c.name = :catalogName
            """;

        MapSqlParameterSource params = new MapSqlParameterSource("catalogName", catalogName);
        // rowMapper builds DataSourceNamespace aggregates from the result set.
        return jdbcOperations.query(sql, params, rowMapper);
    }
}
```

### Entity Mapping
```java
@Table("catalogs")
public class CatalogEntity implements Persistable<UUID> {
    @Id
    @Column("catalog_id")
    private UUID catalogId;

    @Column("name")
    private String name;

    @Column("created_at")
    private Instant createdAt;

    @Override
    public boolean isNew() {
        return true; // Always treat as new for insert
    }
}
```

### Transaction Usage
```java
// DON'T: Reuse context
SpringDataJdbcTransactionContext ctx = transactionManager.single();
repository.create(ctx, entity1);
repository.create(ctx, entity2); // Wrong!

// DO: Inline single() calls
repository.create(transactionManager.single(), entity1);
repository.create(transactionManager.single(), entity2);

// DO: Use withTransaction for multiple operations
transactionManager.withTransaction(ctx -> {
    repository.create(ctx, entity1);
    repository.create(ctx, entity2);
    return repository.findAll(ctx);
});
```

## ScalarDB SQL Migration

Schema changes are applied by `ScalarDbSqlMigrator`, which delegates to ScalarDB's Schema Loader.
The canonical schema definition lives in `src/main/resources/scalardb/schema.json`; the migrator reads this file and provisions tables/indexes on startup using the ScalarDB SQL API.
To evolve the schema, update the JSON definition and rerun the migrator—there are no SQL migration scripts (`db/migration`) anymore, and versioning is handled by Schema Loader itself.

## Testing Strategy

### Unit Tests
- H2 in-memory database
- `SpringDataJdbcTransactionManagerH2Test`: Transaction behavior tests
- Tests use actual database operations

### Integration Tests
- Testcontainers for real database instances
- Separate test classes per database type
- Three test categories:
  - **Table CRUD tests**: Verify all 16 tables' CRUD operations
  - **Query service tests**: Verify complex queries and joins
  - **Repository service tests**: Verify all 7 repository service implementations

### Test Organization
```
test/
├── SpringDataJdbcTransactionManagerH2Test  # Transaction behavior tests
└── integrationTest/
    ├── tables/
    │   ├── TablesCrudTestBase              # Base class with all CRUD tests
    │   ├── PostgreSqlTablesCrudTest        # PostgreSQL-specific setup
    │   ├── MySqlTablesCrudTest             # MySQL-specific setup
    │   ├── SqlServerTablesCrudTest         # SQL Server-specific setup
    │   └── OracleTablesCrudTest            # Oracle-specific setup
    ├── queries/
    │   ├── QueriesTestBase                 # Base class with all query tests
    │   ├── PostgreSqlQueriesTest           # PostgreSQL-specific setup
    │   ├── MySqlQueriesTest                # MySQL-specific setup
    │   ├── SqlServerQueriesTest            # SQL Server-specific setup
    │   └── OracleQueriesTest               # Oracle-specific setup
    └── repositories/
        ├── RepositoriesTestBase            # Base class with all repository tests
        ├── PostgreSqlRepositoriesTest      # PostgreSQL-specific setup
        ├── MySqlRepositoriesTest           # MySQL-specific setup
        ├── SqlServerRepositoriesTest       # SQL Server-specific setup
        ├── OracleRepositoriesTest          # Oracle-specific setup
        └── H2RepositoriesTest              # H2-specific setup
```

## Configuration

### Application Properties
```properties
# Database connection (JDBC URL format)
scalar.db.analytics.server.db.contact-points=jdbc:postgresql://localhost:5432/scalardb_analytics
scalar.db.analytics.server.db.username=postgres
scalar.db.analytics.server.db.password=password

# Optional: Connection pool settings
scalar.db.analytics.server.db.pool.size=10
scalar.db.analytics.server.db.pool.max-lifetime=1800000
scalar.db.analytics.server.db.pool.connection-timeout=30000
scalar.db.analytics.server.db.pool.minimum-idle=5
scalar.db.analytics.server.db.pool.idle-timeout=600000
```

### Supported Databases

The database type is automatically detected from the JDBC URL:

- **PostgreSQL**: `jdbc:postgresql://host:5432/database` (tested with PostgreSQL 16.4)
- **MySQL**: `jdbc:mysql://host:3306/database?permitMysqlScheme=true&sslMode=trust` (tested with MySQL 8.0.36). The bundled driver is MariaDB Connector/J, which rejects the `jdbc:mysql` scheme unless `permitMysqlScheme=true` is set, and defaults to `sslMode=disable`, under which MySQL 8.x accounts using the default `caching_sha2_password` plugin cannot complete authentication. Use `sslMode=disable` only for servers with TLS turned off.
- **SQL Server**: `jdbc:sqlserver://host:1433;databaseName=database` (tested with SQL Server 2019)
- **Oracle**: `jdbc:oracle:thin:@host:1521:database` (tested with Oracle 23)
- **H2**: `jdbc:h2:mem:database` (tested with H2 2.2.224, for development/testing only)

## Known Limitations

1. **MySQL/Oracle**: Cannot create unique constraints on JSON/CLOB columns
2. **SQL Server**: Array storage limited to 400 characters due to 900-byte index key constraint
