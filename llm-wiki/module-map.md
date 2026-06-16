# Module Map

## Monorepo Shape

```text
app/                 Android entry point
core/                Android core modules
feature/             Android feature api/impl modules
build-logic/         Android Gradle conventions

apps/
  api/               TypeScript API server
  web/               React/Next.js web app

packages/
  contracts/         canonical URLs, DTOs, fixtures
  api-client/        typed fetch wrapper
```

Android and TypeScript builds are intentionally separate. Share product contracts through URL/API schema and docs, not by making Android consume TypeScript source.

## Ownership

```text
:app
  Android entry point, MainActivity, app theme wiring
  AppRouter, AppDeepLinkResolver, NotmidRouteGraph
  ActivityRoute dispatch
  notmid runtime content-source selection and async app-shell loading state
  NotmidAppViewModel for top-level state, auth/write orchestration, and UI effects
  Android Credential Manager Google ID-token provider for Firebase REST exchange

:core:designsystem
  NotmidTheme, color/type/spacing/shape/elevation tokens
  Notmid* Material3 wrappers
  reusable Notmid UI primitives
  NotmidFeedbackEffectHandler for shared toast/alert rendering
  Liquid Glass primitives

:core:model
  pure Kotlin immutable models
  platform-independent UI feedback/effect contracts
  platform-independent action/effect delegate contracts for ViewModel injection
  no Android, Compose, Color, Dp, resource ids, or repositories

:core:domain
  suspend repository contracts, typed domain exceptions, and use cases
  pure Kotlin unless a real Android dependency is unavoidable

:core:data
  fake/static repository implementations
  mapping from product data to domain models
  API-backed notmid content repository behind :core:network:api
  thread detail/message hydration for inbox chat screens
  static/API protected-write repositories for capture, save, chat, and profile
  content repository selector for static vs API-backed runtime sources

:core:auth:api
  Firebase-free notmid auth gateway, sign-in request/result, and intent contracts

:core:auth:impl
  local release-safe auth gateway implementation
  debug fake sessions only when runtime auth mode allows fake
  API-verified Firebase auth gateway behind Firebase ID-token provider boundary
  Firebase Auth REST ID-token provider for anonymous sign-in and Google ID-token exchange

:core:network:api
  notmid API config, paths, HTTP method, request/response contracts
  typed NotmidNetworkException for transport, timeout, and invalid-request failures

:core:network:impl
  OkHttp-backed client implementation for the API network boundary

:core:router:api
  pure Kotlin route contracts
  Route, ComposeRoute, ActivityRoute, WebRoute
  DeepLinkSpec, RouteStack, RouteCommand

:core:router:impl
  DefaultRouteRegistry and matching helpers
  no Android Activity launching

:feature:notmid:api
  shared notmid route markers, destination ids, route events

:feature:notmid:common
  product-shaped UI adapters and shared screen sections

:feature:notmid:impl
  notmid app shell and feature orchestration

:feature:*:api
  route contracts, specs, deep-link specs, route events

:feature:*:impl
  Compose screens for that feature only
  feature:capture:impl owns Android CameraX preview and local still capture
  details; it exposes only screen state/callbacks to the notmid shell.

apps/api
  HTTP API, auth verification boundary, future persistence integrations

apps/web
  React app shell, web routes, shareable detail surfaces

packages/contracts
  shared TypeScript DTOs, canonical web route helpers, deterministic fixtures

packages/api-client
  typed fetch client for web/server-side tooling
```

## Dependency Direction

Allowed:

```text
feature:feed:impl -> feature:feed:api
feature:feed:impl -> feature:notmid:common
feature:notmid:impl -> feature:feed:impl
app -> feature:*:api and impl modules
core:designsystem -> core:model
```

Not allowed:

```text
feature:feed:impl -> feature:map:impl
feature impl -> AppRouter
core:model -> Compose/Android
core:designsystem -> product routes or repositories
core:router:impl -> Android Activity launch
```

## Android API To View Flow

```text
OkHttpNotmidNetworkClient
  -> suspend NotmidNetworkClient.execute(request): NotmidNetworkResponse
  -> throws NotmidNetworkException only for client/transport failures
  -> :core:data repository maps HTTP/body contracts into domain models
     (/v1/inbox/threads plus /detail hydrate chat messages)
     (thread.chatAccess carries friend/invite send permissions)
  -> NotmidAppViewModel catches typed repository/auth failures
  -> NotmidAppViewModel.state exposes StateFlow<NotmidAppUiState>
  -> successful chat-send receipts merge into the loaded inbox threadMessages
  -> successful chat invite accept/reject receipts merge the updated thread
  -> :core:model NotmidActionDelegate exposes a channel-backed Flow<NotmidAppAction>
  -> NotmidAppViewModel handles ordered actions at one reducer boundary
  -> :core:model NotmidUiEffectDelegate exposes SharedFlow<NotmidUiEffect>(replay = 0)
  -> :core:designsystem collects effects with LifecycleStartEffect
  -> NotmidFeedbackEffectHandler renders the effect while the UI is STARTED
  -> feature screens receive stable state/callback props, not network clients
```

Do not introduce a shared `Success`/`Failure` sealed response wrapper for
suspend network calls. Success returns the expected value; failure is raised as a
typed exception at the boundary that owns the failure meaning. Server-driven
presentation hints should map into `NotmidUiFeedback` (`Toast`, `Alert`,
`Inline`, `FullPage`) and optional deep-link actions before reaching feature UI.
Chat send permission is API-owned: friends can send immediately, non-friends
must be represented through `thread.chatAccess`, and feature UI only disables
composer or emits invite accept/reject callbacks from that state.
Collect persistent `StateFlow` UI state with `collectAsStateWithLifecycle`; keep
one-shot toast/alert/navigation effects as a separate `Flow` and collect them
through the lifecycle-aware design-system collector. UI effects should not be
replayed after the screen stops; the ViewModel may continue working, but stopped
UI must not process late toast or alert side effects on resume.
`MainActivity` is only the route holder: it creates app dependencies, collects
`NotmidAppViewModel.state` with `collectAsStateWithLifecycle`, passes
`effects` to `NotmidFeedbackEffectHandler`, and forwards user callbacks as
`NotmidAppAction`.
Keep ViewModel capabilities compositional: AndroidX still requires
`ViewModel` inheritance, but reusable side-effect emission should be injected as
a delegate instead of implemented directly by each ViewModel. Use the same
composition rule for action processing: UI-facing callers dispatch through
`onAction(action)`, while a `:core:model` `NotmidActionDelegate` owns the
channel-backed Flow stream. Flow-based action processing is useful when actions
need one ordered reducer boundary; use a channel-backed Flow when UI actions
must not be dropped before the collector is running. Keep the lifecycle-bound
toast/alert collector in `:core:designsystem`; it renders effects but does not
own ViewModel event stream contracts.

## Build Logic

Use project convention plugins:

```text
glassnavlab.android.application
glassnavlab.android.library
glassnavlab.android.library.compose
glassnavlab.kotlin.library
```

Keep conventions small. Do not add Hilt, KSP, Firebase, detekt, or navigation dependencies before code needs them.

## Verification

For module/build changes:

```bash
./gradlew help
./gradlew :app:compileDebugKotlin
```
