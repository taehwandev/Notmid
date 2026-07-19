# Module Map

This page is the Notmid repo inventory. It records what each local module or
workspace owns today.

Reusable Android architecture rules live outside this wiki:

- `docs/specs/android-commonization/README.md`
- Tao Agent OS Android cards for module structure, ViewModel state, and data flow.

## Monorepo Shape

```text
app/                 Android entry point
core/                Android core modules
feature/             Android feature api/impl modules
build-logic/         Android Gradle convention plugins, including Hilt/KSP wiring

apps/
  api/               TypeScript API server
  web/               React/Next.js web app

packages/
  contracts/         canonical URLs, DTOs, fixtures
  api-client/        typed fetch wrapper
```

Android and TypeScript builds are intentionally separate. Share product
contracts through URL/API schema and docs, not by making Android consume
TypeScript source.

## Android Module Family Grammar

Android module family policy의 source of truth는
[`docs/specs/android-commonization/02-target-module-taxonomy.md`](../docs/specs/android-commonization/02-target-module-taxonomy.md)다.
현재 모듈도 다음 owner-first path를 사용한다.

```text
:<family>:<owner>:<role>

:core:auth:api
:core:auth:impl

:core:network:api
:core:network:impl
:core:network:assertions

:feature:feed:api
:feature:feed:impl
```

`owner`는 `auth`, `network`, `router`, `feed`처럼 함께 변경·리뷰되는 capability이고,
`api`, `impl`, `assertions`는 그 아래의 역할이다. 따라서 `:core:api:auth`처럼
역할을 owner보다 앞에 두지 않는다.

일반 consumer는 owner의 `api`에만 의존하고, 같은 owner의 `impl`은 `api`를
구현한다. `assertions`는 `api`에만 의존하며 production `impl`을 기본 의존성으로
끌어오지 않는다. `:app`과 product-shell composer인 `:feature:notmid:impl`만
선택한 feature/core 구현을 runtime graph에 조립한다.

모든 owner가 완성된 쌍이나 trio를 가져야 하는 것은 아니다. `:core:notice:api`,
`:core:data`, `:core:runtime`, `:core:base`, `:core:designsystem`,
`:feature:notmid:common`은 현재 소유권과 caller 압력에 맞춘 의도적인 단일 역할
또는 collapsed 경계다. 빈 `impl`이나 한 테스트만 쓰는 `assertions`를 추가해
모양만 맞추지 않는다.

## Android Modules

```text
:app
  Android entry point, NotmidApplication, MainActivity, manifest/theme selection
  Hilt root with @HiltAndroidApp and @AndroidEntryPoint activity injection
  Hilt runtime modules for BuildConfig-backed config, network clients,
  static/API repository selection, auth gateway selection, and dispatcher
  bindings
  top-level injected ActivityRouteLauncher and NotmidAppRouterFactory
  @HiltViewModel NotmidAppViewModel for top-level state, auth/write
  orchestration, and effects
  Android Credential Manager Google ID-token provider for Firebase REST
  exchange, provided through app DI

:core:designsystem
  NotmidTheme, semantic color/type/spacing/shape/elevation tokens
  Notmid* Material3 wrappers
  reusable Notmid UI primitives and visual notice primitives
  Liquid Glass primitives

:core:model
  pure Kotlin immutable product models
  platform-independent action delegate contracts

:core:notice:api
  pure Kotlin notice request/effect contracts
  NoticeRequest, NoticePresentation, NoticeTone, NoticeAction
  NoticeEffect and NoticeEffectDelegate

:core:domain
  suspend repository contracts, typed domain exceptions, and use cases

:core:data
  fake/static repository implementations
  API-backed notmid content repository behind :core:network:api
  thread detail/message hydration for inbox chat screens
  static/API protected-write repositories for capture, save, chat, and profile
  content repository selector for static vs API-backed runtime sources

:core:auth:api
  Firebase-free notmid auth gateway, sign-in request/result, and intent contracts

:core:auth:impl
  local release-safe auth gateway implementation
  debug fake sessions when runtime auth mode allows fake
  API-verified Firebase auth gateway behind Firebase ID-token provider boundary
  Firebase Auth REST ID-token provider for anonymous sign-in and Google ID-token exchange

:core:network:api
  notmid API config, paths, HTTP method, request/response contracts
  typed NotmidNetworkException for transport, timeout, and invalid-request failures

:core:network:impl
  OkHttp-backed client implementation for the API network boundary

:core:network:assertions
  FakeNotmidNetworkClient and RecordingNotmidNetworkClient for tests
  queued success/failure responses, request assertions, safe header redaction

:core:base
  Compose-only BaseActivity and EdgeToEdgeConfig
  BaseAppRoot and root AppRoot installation
  pending external deep-link convenience types/effects

:core:runtime
  router/config AppRouterBundleConfig, AppDeepLinkUrlConfig, DefaultAppRouterBundle
  router/planner AppRoutePlanner and DefaultAppRoutePlanner
  router/deeplink AppDeepLinkResolver and DefaultAppDeepLinkResolver
  router/runtime AppRouterRuntime, DefaultAppRouterRuntime, PendingActivityRouteRequest
  router/activity ActivityRouteLauncher, ActivityRouteLaunchHandler, DefaultActivityRouteLauncher, ActivityRouteLauncherEffect
  Hilt ActivityComponent binding for the default ActivityRouteLauncher
  notice/host NoticeHost, NoticeEffectLifecycleCollector, NoticeAlertDialog
  Android Toast/Snackbar/Alert dispatch using :core:notice:api and design-system visuals

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

:core:router:assertions
  RouteFixtures, RecordingRouter, RecordingRouteEventSink
  FakeRouteEventPlanner, RoutePlanSubject, RouteStackSubject
  reusable router test support that depends on :core:router:api, not impl

:feature:notmid:api
  route/ shared notmid route markers
  deeplink/ notmid static deep-link helper
  destination/ shared destination ids
  event/ route events

:feature:notmid:common
  product-shaped UI adapters and shared screen sections

:feature:webview:api
  route/ WebViewRoute and WebViewMode
  deeplink/ WebViewDeepLinkSpec
  activity/ WebViewActivityKeys

:feature:webview:impl
  WebView Activity wrapper and reusable Compose WebView content/controller
  Hilt @IntoSet ActivityRouteLaunchHandler contribution

:feature:notmid:impl
  notmid app shell and feature orchestration
  router/ Notmid route registrations, deep-link registrations, event handlers
  rememberNotmidAppRouter and notmidRouteStack over the reusable runtime bundle

:feature:*:api
  route/ typed route data and top-level route metadata
  deeplink/ deep-link specs
  event/ public route events
  activity/ Activity lookup keys when the feature exposes ActivityRoute

:feature:*:impl
  Compose screens for that feature only
  feature:capture:impl owns Android CameraX preview and local still capture details
```

## Web And API Workspaces

```text
apps/api
  Hono HTTP API
  token verification boundary
  fixture and Postgres repository adapters
  protected write policy
  privacy-safe audit logging

apps/web
  React/Next.js product shell
  /notmid canonical web surface
  shareable detail routes
  Firebase Auth anonymous and Google session bridge
  protected write server actions backed by apps/api

packages/contracts
  routes/ canonical web route helpers and URL shapes
  dto/ or schema/ shared TypeScript DTO and validation shapes
  fixtures/ deterministic fixture data
  parity/ route/API parity resolvers when needed

packages/api-client
  typed fetch client for web/server-side tooling
```

## Notmid Dependency Notes

Allowed examples:

```text
feature:feed:impl -> feature:feed:api
feature:feed:impl -> feature:notmid:common
feature:notmid:impl -> feature:feed:impl
app -> feature:*:api and impl modules
app -> core:notice:api
```

Forbidden examples:

```text
feature:feed:impl -> feature:map:impl
feature impl -> app router implementation
core:model -> Compose/Android
core:designsystem -> product routes or repositories
core:router:impl -> Android Activity launch
```

## Build Logic Inventory

Project convention plugins:

```text
glassnavlab.android.application
glassnavlab.android.library
glassnavlab.android.library.compose
glassnavlab.kotlin.library
```
