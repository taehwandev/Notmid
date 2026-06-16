# Module Map

## Monorepo Shape

```text
app/                 Android entry point
core/                Android core modules, including core/base and core/runtime
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

## Planned Android Commonization

The current Android graph already uses feature `api` / `impl` modules. The next
architecture direction is documented in
`docs/specs/android-commonization/README.md` and should be applied in small,
testable slices instead of as a large structure copy.

Target families:

```text
:core:*                 pure Kotlin or implementation-neutral contracts
:core:base              reusable Compose Activity shell and app-root installation
:core:runtime           Android/Compose runtime commonization
:feature:*:api          feature route, event, and public contracts
:feature:*:impl         feature screens, state holders, and orchestration
:*:assertions           reusable test doubles, fixtures, recording helpers, assertion DSLs
```

Rules:

- Keep `:core:*` free of Android and Compose runtime unless a legacy boundary is
  being migrated.
- Use `:core:base` for role-split app-shell helpers: `activity` Compose-only
  BaseActivity, edge-to-edge defaults, final `onCreate`/`onNewIntent` template
  handling, `Content()` composition, root/deep-link convenience functions,
  `root` app-root assembly, and `deeplink` pending external deep-link effects.
- Use `:core:runtime` for runtime components: notice hosts, permission
  adapters, ActivityRoute launch adapters, router runtime, reusable WebView
  runtime, and resources.
- `assertions` modules should depend on stable API contracts and test
  libraries, not production impl modules by default.
- Do not recreate broad `BaseActivity` or `BaseViewModel` inheritance. A narrow
  Compose-only `BaseActivity` may own Activity-level platform defaults,
  final lifecycle template handling, `Content()` composition, app-root
  installation, pending deep-link handoff, and notice/router host installation;
  product state stays in ViewModels and app/feature owners.
- Keep `:core:designsystem` as the visual component/token owner. Notice
  runtime orchestration belongs in `:core:runtime`, while visual rendering
  primitives such as `NotmidSnackbarHost` remain in the design system.

## Ownership

```text
:app
  Android entry point, MainActivity, app theme selection
  runtime config injection and top-level composition
  Android entrypoint and concrete platform launch binding
  concrete ActivityRoute launch binding through the `:core:runtime` launcher registry
  notmid runtime content-source selection and async app-shell loading state
  NotmidAppViewModel for top-level state, auth/write orchestration, and UI effects
  Android Credential Manager Google ID-token provider for Firebase REST exchange

:core:designsystem
  NotmidTheme, color/type/spacing/shape/elevation tokens
  Notmid* Material3 wrappers
  reusable Notmid UI primitives
  NotmidSnackbarHost and other visual notice primitives only
  Liquid Glass primitives

:core:model
  pure Kotlin immutable models
  platform-independent action delegate contracts for ViewModel injection
  no Android, Compose, Color, Dp, resource ids, or repositories

:core:notice:api
  pure Kotlin notice request/effect contracts
  NoticeRequest, NoticePresentation, NoticeTone, NoticeAction
  NoticeEffect and NoticeEffectDelegate for one-shot UI events
  no Android, Compose, resources, feature policy, repositories, or rendering

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

:core:network:assertions
  FakeNotmidNetworkClient and RecordingNotmidNetworkClient for tests
  queued success/failure responses, request assertions, safe header redaction
  depends on :core:network:api, not production :core:network:impl

:core:network:impl
  OkHttp-backed client implementation for the API network boundary

:core:base
  activity BaseActivity and EdgeToEdgeConfig for Compose-only Activity content, edge-to-edge defaults, final lifecycle template handling, BaseAppRoot, and pending deep-link convenience
  root AppRoot for caller-provided theme slot, notice host, and ActivityRoute launch effect installation
  deeplink PendingDeepLink and PendingDeepLinkEffect for lifecycle-safe pending external deep-link handoff
  no Notmid feature policy, repositories, feature impl imports, WebView Intent construction, auth policy, runtime config, ViewModel creation, or data access

:core:runtime
  router/config AppRouterBundleConfig, AppDeepLinkUrlConfig, DefaultAppRouterBundle
  router/planner AppRoutePlanner and DefaultAppRoutePlanner
  router/deeplink AppDeepLinkResolver and DefaultAppDeepLinkResolver
  router/runtime AppRouterRuntime, DefaultAppRouterRuntime, PendingActivityRouteRequest
  router/activity ActivityRouteLauncher, ActivityRouteLaunchHandler, DefaultActivityRouteLauncher, ActivityRouteLauncherEffect
  notice/host NoticeHost, NoticeEffectLifecycleCollector, NoticeAlertDialog
  Android Toast/Snackbar/Alert dispatch using :core:notice:api and design-system visuals
  no Notmid feature policy, repositories, feature impl imports, WebView Intent construction, auth policy, or data access
  test source owns module-local app-router test doubles such as fake deep-link
  resolver and recording ActivityRoute launcher

:core:router:api
  pure Kotlin route contracts
  Route, ComposeRoute, ActivityRoute, TopLevelRoute
  DeepLinkSpec, DeepLinkRequest, DeepLinkResolver, RouteStack, RoutePlan
  RouteCommand, RouteEventHandler, RouteEventPlanner

:core:router:impl
  registry/ DefaultRouteRegistry
  event/ DefaultRouteEventPlanner
  deeplink/ DefaultDeepLinkResolver, DeepLinkUrlPolicy, UriDeepLinkRequestParser
  deeplink/ StaticRouteDeepLinkSpec, PrefixRouteDeepLinkSpec
  no Android Activity launching

:feature:notmid:api
  route/ shared notmid route markers
  deeplink/ notmid static deep-link helper
  destination/ shared destination ids
  event/ route events

:feature:notmid:common
  product-shaped UI adapters and shared screen sections

:feature:notmid:impl
  notmid app shell and feature orchestration
  router/ Notmid route registrations, deep-link registrations, event handlers,
  rememberNotmidAppRouter, and notmidRouteStack over the reusable `:core:runtime` bundle

:feature:*:api
  route/ typed route data and top-level route metadata
  deeplink/ deep-link specs
  event/ public route events
  activity/ Activity lookup keys only when the feature exposes ActivityRoute

:feature:*:impl
  Compose screens for that feature only
  feature:capture:impl owns Android CameraX preview and local still capture
  details; it exposes only screen state/callbacks to the notmid shell.

apps/api
  HTTP API, auth verification boundary, future persistence integrations

apps/web
  React app shell, web routes, shareable detail surfaces

packages/contracts
  routes/ canonical web route helpers and URL shapes
  dto/ or schema/ shared TypeScript DTO and validation shapes
  fixtures/ deterministic fixture data
  parity/ route/API parity resolvers only when needed

packages/api-client
  typed fetch client for web/server-side tooling
```

## Cross-Language Contract Boundaries

The split rule is not Android-only. Kotlin modules, TypeScript packages, API
server folders, web feature folders, and reusable test-support modules should
all separate caller-facing contract families once their callers differ.

Use separate files/folders or exports for:

- route/path helpers and navigation events
- DTOs, schemas, and generated contract shapes
- typed API clients and transport errors
- server handlers, validation, product policy, and repository adapters
- fixtures, recording fakes, and assertion helpers

Avoid catch-all `contracts`, `api`, `common`, `utils`, or `index` exports that
force a caller to import routes, schemas, fixtures, server code, and runtime
adapters together.

## Dependency Direction

Allowed:

```text
feature:feed:impl -> feature:feed:api
feature:feed:impl -> feature:notmid:common
feature:notmid:impl -> feature:feed:impl
app -> feature:*:api and impl modules
app -> core:notice:api
```

Not allowed:

```text
feature:feed:impl -> feature:map:impl
feature impl -> app router implementation
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
  -> :core:notice:api NoticeEffectDelegate exposes SharedFlow<NoticeEffect>(replay = 0)
  -> :core:runtime NoticeHost collects effects with LifecycleStartEffect
  -> NoticeHost dispatches Toast/Snackbar/Alert using design-system visuals
  -> feature screens receive stable state/callback props, not network clients
```

Do not introduce a shared `Success`/`Failure` sealed response wrapper for
suspend network calls. Success returns the expected value; failure is raised as a
typed exception at the boundary that owns the failure meaning. Server-driven
presentation hints should map into `NoticeRequest` (`Toast`, `Snackbar`, `Alert`,
`Inline`, `FullPage`) and optional deep-link actions before reaching feature UI.
Chat send permission is API-owned: friends can send immediately, non-friends
must be represented through `thread.chatAccess`, and feature UI only disables
composer or emits invite accept/reject callbacks from that state.
Collect persistent `StateFlow` UI state with `collectAsStateWithLifecycle`; keep
one-shot toast/alert/navigation effects as a separate `Flow` and collect them
through the lifecycle-aware `:core:runtime` notice host. UI effects should not be
replayed after the screen stops; the ViewModel may continue working, but stopped
UI must not process late toast or alert side effects on resume.
`MainActivity` is only the route holder: it creates app dependencies, collects
`NotmidAppViewModel.state` with `collectAsStateWithLifecycle`, passes
`effects` to `NoticeHost`, and forwards user callbacks as
`NotmidAppAction`.
Keep ViewModel capabilities compositional: AndroidX still requires
`ViewModel` inheritance, but reusable side-effect emission should be injected as
a delegate instead of implemented directly by each ViewModel. Use the same
composition rule for action processing: UI-facing callers dispatch through
`onAction(action)`, while a `:core:model` `NotmidActionDelegate` owns the
channel-backed Flow stream. Flow-based action processing is useful when actions
need one ordered reducer boundary; use a channel-backed Flow when UI actions
must not be dropped before the collector is running. Keep the lifecycle-bound
toast/snackbar/alert collector in `:core:runtime`; it renders effects but does not
own feature state, repositories, or product route policy.

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
