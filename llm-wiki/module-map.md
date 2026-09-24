# Module Map

This page is the Notmid repo inventory. It records what each local module or
workspace owns today.

Reusable Android architecture rules live outside this wiki:

- `docs/specs/android-commonization/README.md`
- Tao Agent OS Android cards for module structure, ViewModel state, and data flow.

## Repository Shape

```text
app/                 Android entry and DI assembly
core/                Android capabilities, domain ports and client adapters
feature/             Android feature api/ui modules and platform entries
build-logic/         Android Gradle conventions
docs/contracts/      Pinned HTTP contract used by Android tests
```

`Notmid-web` owns the web workspace and API client. `Notmid-server` owns the API,
SQL migrations and canonical TypeScript/OpenAPI contracts. Android has no
TypeScript source or Node/pnpm build dependency.

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
:feature:feed:ui
```

`owner`는 `auth`, `network`, `navigation`, `feed`처럼 함께 변경·리뷰되는
capability이고, `api`, `ui`, `impl`, `assertions`는 그 아래의 역할이다. 따라서
`:core:api:auth`처럼 역할을 owner보다 앞에 두지 않는다.

역할 이름은 [`docs/specs/notmid-target-boundary-ard.md`](../docs/specs/notmid-target-boundary-ard.md)의
Decision 2를 따른다. Compose feature는 `ui`가 소유하고, `impl`은 Activity/Intent/
manifest 같은 플랫폼 진입 전용이다. 현재 `impl` 자격을 가진 feature는
`:feature:webview:impl` 하나다.

일반 consumer는 owner의 `api`에만 의존하고, 같은 owner의 `impl`은 `api`를
구현한다. `assertions`는 `api`에만 의존하며 production `impl`을 기본 의존성으로
끌어오지 않는다. `:app`이 선택한 feature/core 구현을 runtime graph에 조립한다.
로그인 UI는 `:feature:auth:ui`, 셸과 제품 라우트 그래프는 `:app`이 소유한다.

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
  injected ActivityRouteLauncher and AppRouterRuntime from NotmidAppRouterFactory
  NotmidAppViewModel for app content loading and notice host effects
  NotmidShellViewModel for active route, auth gate, and shell actions
  Android Credential Manager Google ID-token provider for Firebase REST
  exchange, provided through app DI

:core:designsystem
  NotmidTheme, semantic color/type/spacing/shape/elevation tokens
  Notmid* Material3 wrappers
  reusable Notmid UI primitives and visual notice primitives
  Liquid Glass primitives

:core:model
  pure Kotlin immutable product models

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
  one shared notmid API JSON layer used by both API-backed repositories:
  field accessors, enum codec, model decoders, request encoders, and the
  id-derived palette/progress values the API does not send

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

:core:navigation:api
  pure Kotlin route contracts
  Route, ComposeRoute, ActivityRoute, TopLevelRoute
  DeepLinkSpec, DeepLinkRequest, DeepLinkResolver, RouteStack, RoutePlan
  RouteCommand, RouteEventHandler, RouteEventPlanner
  notmid/ NotmidRoute, NotmidTopLevelRoute, NotmidRouteEvent,
    NotmidDestinationIds, NotmidStaticDeepLinkSpec

:core:navigation:impl
  registry/ DefaultRouteRegistry
  event/ DefaultRouteEventPlanner
  deeplink/ DefaultDeepLinkResolver, DeepLinkUrlPolicy, UriDeepLinkRequestParser
  deeplink/ StaticRouteDeepLinkSpec, PrefixRouteDeepLinkSpec

:core:navigation:assertions
  RouteFixtures, RecordingRouter, RecordingRouteEventSink
  FakeRouteEventPlanner, RoutePlanSubject, RouteStackSubject
  reusable router test support that depends on :core:navigation:api, not impl

:feature:notmid:common
  product-shaped UI adapters and shared screen sections

:feature:webview:api
  route/ WebViewRoute and WebViewMode
  deeplink/ WebViewDeepLinkSpec
  activity/ WebViewActivityKeys

:feature:webview:impl
  WebView Activity wrapper and reusable Compose WebView content/controller
  Hilt @IntoSet ActivityRouteLaunchHandler contribution

:feature:auth:ui
  login screen state/action owner and auth gateway caller

:app
  notmid app shell and route dispatch
  router/ Notmid route registrations, deep-link registrations, event handlers
  rememberNotmidAppRouter and notmidRouteStack over the reusable runtime bundle

:feature:*:api
  route/ typed route data and top-level route metadata
  deeplink/ deep-link specs
  event/ public route events
  activity/ Activity lookup keys when the feature exposes ActivityRoute

:feature:*:ui
  Compose surface for that feature only
  feature:capture:ui owns Android CameraX preview and local still capture details
  screen state owners live here; :app NotmidAppViewModel retains app content loading
```

## External Service Repositories

The Web and API workspaces have separate repositories. Refer to their README
and verification commands. The integration boundary here is the pinned
`docs/contracts/notmid-openapi.json` plus canonical deep-link URL shapes.
See [extraction record](../docs/specs/notmid-service-extraction.md).

## Notmid Dependency Notes

Allowed examples:

```text
feature:feed:ui -> feature:feed:api
feature:feed:ui -> feature:notmid:common
app -> feature:feed:ui
app -> feature:auth:ui
app -> feature:*:api, feature:*:ui, and feature:webview:impl
app -> core:notice:api
```

Forbidden examples:

```text
feature:feed:ui -> feature:map:ui
feature ui -> app router implementation
core:model -> Compose/Android
core:designsystem -> product routes or repositories
core:navigation:impl -> Android Activity launch
```

## Build Logic Inventory

Project convention plugins:

```text
glassnavlab.android.application
glassnavlab.android.library
glassnavlab.android.library.compose
glassnavlab.kotlin.library
```
