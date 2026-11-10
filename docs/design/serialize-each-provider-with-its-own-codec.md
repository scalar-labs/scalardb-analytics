# Serialize each provider with its own codec

## Question

How should provider payloads be converted between JSON and provider objects so that adding a
provider type does not require changing a shared converter?

## Answer

Each provider type has its own codec implementing `ProviderCodec<T>`, which only converts between a
JSON payload and that provider's class. A separate `ProviderCodecRegistry` owns the mapping from
provider type names and classes to codecs, and `DataSourceProviderCodec` dispatches on the
payload's `type` field through the registry.

Codecs do not know their own type name, register themselves, or resolve `${file:...}` references.
The registry is populated with the built-in providers when it is created.

## Rationale

- A new provider adds a provider class and a codec and registers them in one place. Existing codecs
  and the dispatch logic do not change, and the payload-based gRPC contract and metadata schema
  (see [Data sources](../data-sources.md)) do not change either.
- Keeping codecs free of registration and resolution concerns makes each one a small, pure
  conversion that can be tested in isolation. Most codecs only configure a shared Jackson-based
  base class.
- Registering providers in one explicit place keeps the set of supported providers visible and
  avoids startup-time classpath scanning.

## Alternatives

- **One converter with a branch per provider**: fewer classes, but every new provider edits shared
  code and the branch grows with each provider.
- **Codecs that register themselves**: self-contained, but mixes conversion with registration and
  makes the codecs harder to test.
- **Discovering codecs by classpath scanning**: no registration code, but slower to start, harder
  to debug, and unreliable under some class loader setups.

## Consequences

There is one codec class per provider, and payloads pass through one extra layer between JSON and
the provider object. The registry is populated only with the built-in providers; registering a
provider from outside the `api` module would need an extension of the registry, while the codec
interface itself would stay unchanged.
