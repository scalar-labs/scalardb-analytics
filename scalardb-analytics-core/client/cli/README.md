# CLI Client Module

The `client/cli` module implements the command-line interface (CLI) application for ScalarDB Analytics using Picocli framework and gRPC for server communication.

## Fat JAR

This module can be built as a fat JAR (uber JAR) that includes all dependencies:

```bash
./gradlew :client:cli:shadowJar
```

This creates `scalardb-analytics-cli-{version}-all.jar` in `client/cli/build/libs/`. You can run it directly with:

```bash
java -jar scalardb-analytics-cli-*-all.jar --help
```

## Module Architecture

### Command Hierarchy

The CLI follows a hierarchical command structure that mirrors the resource relationships:

```
scalardb-analytics
├── catalog
│   ├── create           # Create a new catalog
│   ├── delete           # Delete a catalog (supports --cascade)
│   ├── describe         # Show catalog details (supports --by-id)
│   └── list             # List all catalogs
├── data-source
│   ├── register         # Register a new data source in a catalog
│   ├── unregister       # Unregister a data source from its catalog (supports --cascade)
│   ├── describe         # Show data source details (supports --by-id)
│   └── list             # List data sources in a catalog
├── namespace
│   ├── describe         # Show namespace details (supports --by-id)
│   └── list             # List namespaces in a data source
├── table
│   ├── describe         # Show table details with columns (supports --by-id)
│   └── list             # List tables in a namespace
├── user                 # Analytics users (principals)
│   ├── create           # Create a user; with --backend-user, also create+link an internal-backend user atomically
│   ├── delete           # Delete a user (supports --cascade)
│   ├── describe         # Show user details (roles, linked backend user)
│   ├── list             # List all users
│   ├── link             # Link an existing user to an existing backend user
│   └── unlink           # Remove the link between a user and a backend user
├── internal-backend     # Users of the internal authentication backend (login credentials)
│   ├── create           # Create an internal-backend user
│   └── delete           # Delete an internal-backend user (supports --cascade)
├── role
│   ├── create           # Create a new role
│   ├── delete           # Delete a role
│   ├── list             # List all roles
│   ├── grant            # Assign a role to a user
│   └── revoke           # Remove a role assignment from a user
└── permission
    ├── grant            # Grant a permission on a resource to a user or role
    │   ├── catalog
    │   ├── data-source
    │   ├── namespace
    │   └── table
    ├── revoke           # Revoke a permission on a resource from a user or role
    │   ├── catalog
    │   ├── data-source
    │   ├── namespace
    │   └── table
    └── list             # List effective permissions for a user or a role
```

### Command Organization

Commands are organized by resource type, with each resource having its own package:

- **Read-only resources**: `namespace`, `table` - only support listing and describing
- **Manageable resources**: `catalog`, `data-source` - support full CRUD operations
- **Authentication & authorization**: `user`, `internal-backend`, `role`, `permission` - manage principals, internal-backend login credentials, roles, and effective permissions. A principal (`user`) is decoupled from its backend credential (`internal-backend`); `user link` / `user unlink` manage the association
- **Cascade-enabled**: `catalog`, `data-source`, `user`, `internal-backend` - support `--cascade` flag for recursive deletion / unlinking
- **ID-capable**: All `describe` commands support resource-specific ID options (e.g., `--catalog-id`, `--table-id`, `--user-id`) for UUID-based lookup

### Package Structure

```
com.scalar.db.analytics.client/
├── Main.java                      # Entry point, creates CommandLine instance
├── cli/
│   ├── CliRoot.java              # Root command, manages modules and config
│   ├── Formatter.java            # Output formatting interface
│   ├── Printer.java              # Console output abstraction
│   ├── CommandResult.java        # Result types (Success, Failure, Error)
│   └── {resource}/               # Command packages per resource type
│       ├── {Resource}Command.java # Parent command for resource
│       ├── Create.java           # Create/Register subcommand
│       ├── Delete.java           # Delete subcommand
│       ├── Describe.java         # Describe subcommand
│       └── List.java             # List subcommand
├── config/
│   ├── AppConfig.java            # Configuration interface using Owner library
│   ├── ConfigLoader.java         # Config file resolution logic
│   └── UnvalidatedConfig.java    # Raw config before validation
├── exception/
│   └── ClientException.java      # Client-specific exceptions
└── module/
    ├── Modules.java              # Module container and factory
    ├── GrpcModule.java           # gRPC channel and stub management
    └── UseCaseModule.java        # Use case interface bindings
```

### Key Design Patterns

#### 1. Command Pattern with Picocli

Each command implements `Callable<Integer>` and uses Picocli annotations:

```java
@Command(name = "delete", description = "Delete a catalog")
public class Delete implements Callable<Integer> {
    @ParentCommand
    private CatalogCommand parent;  // Access to parent command

    @ArgGroup(exclusive = true, multiplicity = "1")
    private DeleteParams params;    // Either by name or by ID

    @Option(names = {"--cascade"})
    private boolean cascade;

    @Override
    public Integer call() {
        // Command implementation
        return ExitCode.OK;
    }
}
```

#### 2. Module Pattern for Dependency Management

Instead of a DI framework, uses a simple module pattern:

```java
public record Modules(GrpcModule grpc, UseCaseModule useCase) {
    public static Modules initialize(AppConfig config) {
        GrpcModule grpcModule = GrpcModule.initialize(config.catalogServer());
        UseCaseModule useCaseModule = UseCaseModule.initialize(grpcModule);
        return new Modules(grpcModule, useCaseModule);
    }
}
```

Modules are lazily loaded in `CliRoot`:

```java
public Modules loadModules() {
    if (modules == null) {
        AppConfig config = loadConfig();
        modules = Modules.initialize(config);
    }
    return modules;
}
```

#### 3. Result Pattern for Command Output

Commands return structured results for consistent output handling:

```java
public interface CommandResult {
    // `result` is an arbitrary payload serialized as JSON (e.g. an entity or list),
    // so read and create commands can return structured output, not just a message.
    class Success implements CommandResult {
        Success(String message, Object result) { ... }
        Success(Object result) { ... } // message defaults to "Success"
    }
    class Failure implements CommandResult { Failure(String message) { ... } }
    class CommandError implements CommandResult { CommandError(String message, Exception exception) { ... } }
}
```

### Configuration

The client expects a properties file with server connection details. Default location: `${XDG_CONFIG_HOME}/scalardb-analytics/client.properties`

Example configuration file:

```properties
# Server connection
scalar.db.analytics.client.server.host=localhost
scalar.db.analytics.client.server.catalog.port=11051

# TLS/SSL configuration
scalar.db.analytics.client.server.tls.enabled=true
scalar.db.analytics.client.server.tls.ca_root_cert_path=/path/to/ca.crt
scalar.db.analytics.client.server.tls.override_authority=analytics.example.com
```

Configuration can be overridden via:

- Environment variable: `SCALAR_DB_ANALYTICS_CONFIG_PATH=/path/to/config.properties`
- Command line: `scalardb-analytics -c /path/to/config.properties catalog list`

### Command Implementation Patterns

#### ArgGroup for Mutually Exclusive Options

```java
@ArgGroup(exclusive = true, multiplicity = "1")
static class DeleteParams {
    @Option(names = {"-c", "--catalog"})
    String catalogName;

    @Option(names = {"-i", "--catalog-id"})
    UUID catalogId;
}
```

#### Nested ArgGroups for Complex Parameters

```java
static class DescribeParams {
    @ArgGroup(exclusive = false)
    ByName byName;

    @Option(names = {"-i", "--table-id"})
    UUID tableId;
}

static class ByName {
    @Option(names = {"-c", "--catalog"}, required = true)
    String catalogName;

    @Option(names = {"-d", "--data-source"}, required = true)
    String dataSourceName;
}
```

### Error Handling

Consistent error handling across commands:

```java
try {
    // Command logic
    return ExitCode.OK;
} catch (DeletionBlockedException e) {
    printer().printFailure(new Failure(e.getMessage()));
    return ExitCode.USAGE;
} catch (Exception e) {
    printer().printError(new CommandError("Operation failed", e));
    return ExitCode.SOFTWARE;
}
```

Exit codes follow Unix conventions:

- `0` (OK): Success
- `64` (USAGE): Command line usage error
- `70` (SOFTWARE): Internal software error

### Cascade Deletion Implementation

The CLI provides cascade deletion support with safe defaults:

```java
// In Delete command
@Option(names = {"--cascade"}, defaultValue = "false")
private boolean cascade;
```

**Default behavior (cascade=false)**:

- Deletion fails if the resource has any dependencies
- Throws `DeletionBlockedException` with details about what's blocking deletion
- CLI catches this and suggests using `--cascade` flag

**With cascade=true**:

- Recursively deletes all dependent resources
- Deletion is atomic (all-or-nothing)

Example flow:

```java
try {
    boolean deleted = modules.useCase().catalog().deleteCatalog(catalogName, cascade);
} catch (DeletionBlockedException e) {
    // e.getMessage() contains: "Cannot delete catalog 'X' because it contains Y data source(s)..."
    printer().printFailure(new Failure(e.getMessage() + ". Use --cascade to delete all children."));
    return ExitCode.USAGE;
}
```

Commands that support cascade:

- `catalog delete --cascade`: Deletes catalog and all its data sources (including namespaces, tables, and columns)
- `data-source unregister --cascade`: Unregisters data source and all its namespaces, tables and columns
- `user delete --cascade`: Deletes a user along with its linked backend users, identities, and role assignments
- `internal-backend delete --cascade`: Unlinks the internal-backend user from any principal before deleting it

### Testing

The client module uses unit tests to verify command behavior. Tests use mocked dependencies to isolate command logic and verify:

- Command parameter parsing
- Error handling
- Success scenarios
- Exit codes

Test files are organized by command in `src/test/java` following the same package structure as the main code.

### Adding New Commands

1. Create command class in appropriate package
2. Implement `Callable<Integer>`
3. Add `@Command` annotation with metadata
4. Define parameter classes with `@Option`/`@Parameters`
5. Add to parent command's subcommands list
6. Implement command logic following existing patterns
