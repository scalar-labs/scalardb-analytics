---
created: 2025-10-10
updated: 2025-10-10
---

# gRPC Module

This module provides gRPC protocol definitions and type-safe mappers for converting between Protocol Buffer messages and domain models.

## Overview

The gRPC module serves as the communication contract between:
- **Server**: `scalardb-analytics-server` (gRPC service implementations)
- **Client**: `scalardb-analytics` CLI and programmatic clients

## Components

### Protocol Buffer Definitions

Located in `src/main/proto/`, organized by domain:

- **catalog**: Catalog management (create, find, list, initialize)
- **datasource**: Data source registration and schema introspection
- **namespace**: Namespace listing within data sources
- **table**: Table discovery and metadata
- **view**: View registration and management
- **view_namespace**: View namespace operations
- **datatype**: Common data type definitions

All proto files follow the versioning pattern `v1/` for future compatibility.

### MapStruct Mappers

Type-safe bidirectional mappers between protobuf messages and domain models, located in `src/main/java/.../grpc/mapper/`.

#### Architecture

```
Domain Model ←→ MapStruct Mapper ←→ Protobuf Message
     (core)         (grpc)           (grpc/generated)
```

#### Mapper Organization

Each domain entity has a dedicated mapper interface:

- `CatalogMapper`: Catalog ↔ catalog.v1.Catalog
- `DataSourceMapper`: DataSource ↔ datasource.v1.DataSource (provider JSON travels inline as `provider_payload_json`)
- `NamespaceMapper`: Namespace/DataSourceNamespace ↔ namespace.v1.Namespace/DataSourceNamespace
- `TableMapper`: Table/TableDetail/DataSourceNamespaceTableDetail ↔ table.v1.*
- `ViewMapper`: View/ViewDetail/ViewNamespaceView ↔ view.v1.*
- `ViewNamespaceMapper`: ViewNamespace ↔ view_namespace.v1.ViewNamespace

**Shared Utilities:**
- `UuidMapper`: UUID ↔ String conversion
- `DataTypeMapper`: Enum mapping with protobuf UNSPECIFIED/UNRECOGNIZED handling
- `MapStructConfig`: Shared configuration (component model, null handling)

#### Mapping Strategies

##### 1. Basic Types

```java
@Mapper(config = MapStructConfig.class, uses = {UuidMapper.class})
public interface ExampleMapper {
    @Mapping(target = "id", source = "id", qualifiedByName = "uuidToString")
    ProtoMessage toProto(DomainModel model);

    @Mapping(target = "id", source = "id", qualifiedByName = "stringToUuid")
    DomainModel toDomain(ProtoMessage proto);
}
```

- **UUID**: Converted via `UuidMapper` with `@Named` methods
- **Enums**: Explicit `@ValueMapping` for protobuf special values (UNSPECIFIED, UNRECOGNIZED)

##### 2. Polymorphic Types

- **DataSourceProvider**: gRPC transports provider configuration as JSON (`provider_payload_json`) that already embeds the provider `type` alongside provider-specific fields. `DataSourceProviderJsonMapper` wraps the shared `DataSourceProviderCodec` (bootstrapped from `CodecObjectMapperFactory` / `ProviderCodecRegistry`) so MapStruct simply forwards the JSON string.
- **Other polymorphic types (e.g., `ViewProvider`)**: continue to rely on a oneof + visitor/switch strategy, setting the correct variant during MapStruct after-mapping hooks.

##### 3. Collections and Nested Objects

- **Collections**: Automatically mapped by MapStruct
- **Null elements**: Filtered using `@AfterMapping` when needed
- **Nested objects**: Composed via `uses = {NestedMapper.class}`

Example:
```java
@Mapper(uses = {NamespaceSchemaMapper.class, TableSchemaMapper.class})
public interface DataSourceSchemaMapper {
    // MapStruct automatically uses NamespaceSchemaMapper for List<NamespaceSchema>
    DataSourceSchemaProto toProto(DataSourceSchema schema);
}
```

##### 4. Protobuf-Specific Fields

Ignored fields that are protobuf implementation details:

```java
@IgnoreProtobufBuilderDefaults
@Mapping(target = "idBytes", ignore = true)
@Mapping(target = "nameBytes", ignore = true)
@Mapping(target = "mergeFrom", ignore = true)
@Mapping(target = "clearXxx", ignore = true)
ProtoMessage toProto(DomainModel model);
```

- **ByteString fields**: Ignored (e.g., `idBytes`, `nameBytes`)
- **Builder methods**: Ignored (e.g., `mergeFrom`, `clearXxx`)
- **Repeated field helpers**: Ignored (e.g., `xxxList`, `xxxBuilderList`)

The `@IgnoreProtobufBuilderDefaults` meta-annotation groups common ignores for protobuf builders.

#### Singleton Pattern

All mappers use the singleton pattern via MapStruct's `Mappers.getMapper()`:

```java
@Mapper(config = MapStructConfig.class)
public interface ExampleMapper {
    ExampleMapper INSTANCE = Mappers.getMapper(ExampleMapper.class);

    // mapping methods...
}
```

**Usage:**
```java
// In service implementations or use cases
ExampleMapper mapper = ExampleMapper.INSTANCE;
ProtoMessage proto = mapper.toProto(domainModel);
```

## Integration Points

### Server-Side (service implementations)

Located in `server/base/src/main/java/.../server/grpc/`:

```java
@Override
public void findCatalog(FindCatalogRequest request, StreamObserver<FindCatalogResponse> observer) {
    Catalog catalog = catalogUseCase.findCatalog(request.getCatalogName());

    FindCatalogResponse response = FindCatalogResponse.newBuilder()
        .setCatalog(CatalogMapper.INSTANCE.toProto(catalog))
        .build();

    observer.onNext(response);
    observer.onCompleted();
}
```

### Client-Side (use case implementations)

Located in `usecase-grpc-client/src/main/java/.../usecase/grpc/`:

```java
@Override
public Catalog createCatalog(String catalogName) {
    CreateCatalogRequest request = CreateCatalogRequest.newBuilder()
        .setCatalogName(catalogName)
        .build();

    CreateCatalogResponse response = stub.createCatalog(request);

    return CatalogMapper.INSTANCE.toDomain(response.getCatalog());
}
```

## Design Principles

### RPC Idempotency

All RPCs **must** be designed to be idempotent. The client SDK retries transient gRPC errors (UNAVAILABLE, DEADLINE_EXCEEDED, RESOURCE_EXHAUSTED) by default with exponential backoff. This means any RPC may be called more than once for a single client request, so server-side implementations must produce the same result regardless of how many times a request is received.

## Building This Module

The gRPC module uses MapStruct annotation processors during compilation to generate mapper implementations:

```bash
./gradlew :grpc:build
```

Generated mapper implementations are in `build/generated/sources/annotationProcessor/`.
