---
title: Network And Error Contract
audience: Android engineers and AI agents
purpose: Notmid network boundary, typed errors, server presentation hints, retryability를 공통화한다.
status: draft
owner: notmid Android architecture
source_of_truth: docs/specs/android-commonization
last_verified: 2026-06-16
applies_to: core network, core data, domain failures, feedback mapping
related_pages:
  - 06-feedback-alert-toast-contract.md
  - 07-state-assertions-testing.md
  - llm-wiki/module-map.md
---

# Network And Error Contract

## Current State

Notmid currently has:

```text
:core:network:api
  NotmidNetworkClient
  NotmidNetworkRequest
  NotmidNetworkResponse
  NotmidNetworkError
  NotmidNetworkException

:core:network:impl
  OkHttpNotmidNetworkClient

:core:data
  ApiNotmidContentRepository
  ApiNotmidProtectedWriteRepository
  repository-specific JSON parsing and error mapping

:core:domain
  NotmidProtectedWriteFailure
  NotmidProtectedWriteException
```

This is a good start. The missing piece is a shared typed failure model that can represent server error envelopes, retryability, safe diagnostics, and optional UI presentation hints without making every suspend call return a generic `Success`/`Failure`.

## Decision

Do not wrap every suspend API call in a sealed result.

Use this rule:

```text
success -> return value or normalized response
failure -> throw typed boundary/domain exception
state owner -> catch typed failure and map to state/effect
```

The network layer should normalize transport/protocol details once. Repositories should map HTTP body contracts into domain failures. ViewModels should not parse raw response bodies.

## Error Layers

Use five layers.

```text
raw exception / raw HTTP response
  -> network boundary failure
  -> API contract failure
  -> domain failure
  -> user-visible feedback/state
```

### Raw Exception

Examples:

- `IOException`.
- `SocketTimeoutException`.
- malformed URL.
- JSON parse exception.
- HTTP non-2xx raw response.

Ownership:

- `:core:network:impl` or repository implementation.

### Network Boundary Failure

Examples:

```text
TransportUnavailable
Timeout
InvalidRequest
Cancelled
ProtocolStatus
MalformedBody
```

Ownership:

- `:core:network:api`.

### API Contract Failure

Examples:

```text
NotmidApiErrorEnvelope(
  code,
  messageKey,
  safeFallbackMessage,
  presentationHint,
  actionHint,
  retryable,
  retryAfterMillis,
  correlationId
)
```

Ownership:

- The shape belongs in `:core:network:api` if it is transport-wide.
- Product-specific mapping belongs in `:core:data` or `:core:domain`.

### Domain Failure

Examples:

```text
MissingAuth
InvalidRequest
Forbidden
Conflict
QuotaExceeded
NotFound
Unavailable
MalformedResponse
```

Ownership:

- `:core:domain`.

### User-Visible Feedback

Examples:

```text
Toast
Snackbar
Alert
Inline
FullPage
```

Ownership:

- Contract in the `:core-app` feedback API package.
- Rendering in the `:core-app` feedback implementation package.
- Feature-specific decision remains in feature/app state owner.

## Target Network API

Keep `NotmidNetworkClient` small, but expand failure metadata.

Target concepts:

```kotlin
sealed interface NotmidNetworkFailure {
    val code: NotmidNetworkFailureCode
    val retryable: Boolean
    val safeMessage: String?
    val correlationId: String?
    val causeName: String?
}

enum class NotmidNetworkFailureCode {
    InvalidRequest,
    Transport,
    Timeout,
    Cancelled,
    HttpStatus,
    MalformedBody,
}
```

If this is too broad for the first pass, start by extending `NotmidNetworkErrorCode` and `NotmidNetworkError`.

Do not expose:

- OkHttp `Response`.
- raw exception instance as public stable API.
- raw request/response body in public errors.
- secret headers or bearer tokens.

## Server Error Envelope

Target safe shape:

```kotlin
data class NotmidApiErrorEnvelope(
    val code: String,
    val messageKey: String? = null,
    val safeFallbackMessage: String? = null,
    val presentation: NotmidErrorPresentationHint? = null,
    val action: NotmidErrorActionHint? = null,
    val retryable: Boolean = false,
    val retryAfterMillis: Long? = null,
    val correlationId: String? = null,
)
```

Presentation hint is a hint, not a command. The client still decides whether toast, alert, inline, or full-page is appropriate.

Allowed action hint:

```text
retry
open_deep_link
sign_in
dismiss
```

Disallowed:

```text
raw component name
executable command
raw HTML
arbitrary JavaScript
feature implementation class name
```

## Repository Mapping

Repository implementation maps:

```text
NotmidNetworkResponse + body
  -> domain entity
  -> domain failure
```

Feature/ViewModel should see:

```text
domain entity
domain exception/failure
```

Feature/ViewModel should not see:

```text
HTTP status body parsing
OkHttp exception
JSON object
raw server envelope
```

## Retry Policy

Classify before retrying.

| Failure | Retry? | Owner |
| --- | --- | --- |
| timeout | yes with bounded retry/backoff | network or use case |
| temporary unavailable | yes when idempotent | repository/use case |
| rate limited | yes after retry-after when safe | repository/use case |
| invalid input | no | feature state |
| missing auth | no, route to auth | app state |
| conflict | maybe user decision | feature state |
| protected write mutation | only with idempotency key | domain/repository |

Never retry a protected mutation blindly if it can duplicate side effects.

## Network Assertions

`core:network:assertions` should provide:

```text
FakeNotmidNetworkClient
RecordingNotmidNetworkClient
QueuedNetworkResponse
QueuedNetworkFailure
NetworkRequestAssertions
ApiErrorEnvelopeFixtures
```

Required observations:

- request method/path/body.
- request headers with secret redaction helpers.
- response queue usage.
- thrown failure type/code.
- cancellation behavior where practical.

Do not store or assert real tokens.

## Relationship To Feedback

Network must not directly show UI.

Allowed flow:

```text
network throws typed failure
repository maps to domain failure
ViewModel/app reducer maps to FeedbackRequest or UiState
core-app feedback renderer shows Toast/Alert/Snackbar
```

Forbidden:

```text
network impl depends on Toast
repository opens AlertDialog
network exception handler depends on Router
server error envelope directly launches a route
```

The reference project's network exception handler coupled network handling to toast/dialog/router. Notmid should split this: network produces typed metadata, app/feature state owner decides UI and route effects.

## Migration Steps

1. Add `core:network:assertions` without changing production behavior.
2. Add tests around existing `NotmidNetworkException`.
3. Add safe server error envelope parser in `:core:data` or `:core:network:api` depending on final ownership.
4. Move duplicated protected write error parsing into a shared mapper.
5. Add typed retryability and correlation id fields.
6. Map common API failures to the `:core-app` feedback API package once it exists.

## Verification

Network API:

```bash
./gradlew :core:network:api:test
```

Network impl:

```bash
./gradlew :core:network:impl:test
```

Repository mapping:

```bash
./gradlew :core:data:test
```

Full Android compile:

```bash
./gradlew :app:compileDebugKotlin
```

Security check:

- tests must not print bearer tokens.
- network failures must not expose raw request/response bodies in public exception messages.
