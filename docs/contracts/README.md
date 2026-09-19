# Android HTTP contract snapshot

`notmid-openapi.json` is the pinned service contract consumed by Android tests.
It is a data artifact, not server implementation. Android compilation and tests
must not import TypeScript, start the API, or require Node/pnpm.

## Provenance

- Service contract version: `0.1.0`, OpenAPI `3.1.0`.
- Source repository revision: `ed319152d460c27077a39b9cfaefc1a8602910c6`.
- Source export: `notmidOpenApiDocument` from the service contracts package.
- Snapshot SHA-256: `aeda3e1b207f04ac92f5d3110b647f63c6dbcd5b4e565c3ddc4dce09c0e92967`.
- Serialization: `JSON.stringify(document, null, 2)` plus one trailing newline.

This snapshot was exported directly from the service source before repository
extraction. It is not evidence of a deployed API version. The source owner must
publish the same artifact with future service releases; Android adopts an
explicit reviewed version rather than fetching the current service during builds.

## Boundary and verification

The service owns endpoints and wire schemas. Android owns HTTP adapters, JSON
mapping to domain values, and presentation. App DI selects adapters; it does not
implement server authorization, persistence, or endpoint handlers.

The data module loads this file only as a test resource. Its contract tests use
schema-derived response bodies through the real repository decoders and compare
outgoing request methods, paths and bodies with the snapshot. Run:

```sh
./gradlew :core:data:test :core:network:api:test --offline
```

The current service schema does not declare object `required` lists. Therefore
the snapshot cannot prove which fields the server guarantees to return. Tests
exercise the documented properties and Android's required-field handling, but
are not a complete OpenAPI validator or a live provider conformance test. Fixing
server schema requiredness is a separate compatibility change, not something
the Android consumer should invent in its pinned copy.

The tests cover endpoint paths/methods (including auth), content repository
decoding, protected writes, documented enums/attachment variants, and selected
malformed responses. Auth response decoding is covered by the existing auth
tests, not by these schema-derived fixtures. JSON coercion and fields that the
Android mappers ignore are not evidence of strict wire-schema enforcement.

Android currently resolves deep links locally. The snapshot retains the service
deep-link endpoint for completeness; the unused Android endpoint constant has
been removed. This does not change navigation behavior.

## Updating the snapshot

1. Obtain the versioned OpenAPI artifact from the service owner and record its
   source revision and SHA-256 here.
2. Replace the snapshot without manually rewriting server schemas.
3. Review route, method, request, response and error compatibility with the
   Android adapters; update adapters only for an explicitly accepted change.
4. Run the contract tests and the full Android tests/build before committing.

Never put tokens, runtime environment files, database URLs, or provider
credentials in this artifact.
